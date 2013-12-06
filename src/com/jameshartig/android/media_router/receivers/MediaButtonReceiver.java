/*
 * Copyright 2011 Harleen Sahni
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jameshartig.android.media_router.receivers;

import static com.jameshartig.android.media_router.Constants.TAG;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.app.ActivityManager.RunningTaskInfo;
import android.app.KeyguardManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.ResolveInfo;
import android.media.AudioManager;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.KeyEvent;

import com.jameshartig.android.media_router.Constants;
import com.jameshartig.android.media_router.ReceiverSelector;
import com.jameshartig.android.media_router.ReceiverSelectorLocked;
import com.jameshartig.android.media_router.Utils;

/**
 * Handles routing media button intents to application that is playing music
 * 
 * @author Harleen Sahni
 * @author James Hartig
 */
public class MediaButtonReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        if (!preferences.getBoolean(Constants.ENABLED_PREF_KEY, true)) {
            return;
        }

        ActivityManager activityManager = ((ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE));

        if (Utils.isHandlingThroughSoleReceiver()) {
            // Try to figure out if our selector is currently open
            List<RunningTaskInfo> runningTasks = activityManager.getRunningTasks(1);
            if (runningTasks.size() > 0) {
                String className = runningTasks.get(0).topActivity.getClassName();
                if (className.equals(ReceiverSelector.class.getName())
                        || className.equals(ReceiverSelectorLocked.class.getName())) {
                    Log.d(TAG, "Selector is already open, rebroadcasting for selector only.");
                    Intent receiver_selector_intent = new Intent(Constants.INTENT_ACTION_VIEW_MEDIA_LIST_KEYPRESS);
                    receiver_selector_intent.putExtras(intent);
                    context.sendBroadcast(receiver_selector_intent);
                    if (isOrderedBroadcast()) {
                        abortBroadcast();
                    }
                    return;
                }
            }
        }

        // Sometimes we take too long finish and Android kills
        // us and forwards the intent to another broadcast receiver. If this
        // keeps being a problem, than we should always return immediately and
        // handle forwarding the intent in another thread
        if (Intent.ACTION_MEDIA_BUTTON.equals(intent.getAction())) {
            //Log.d(TAG, "Media Button Receiver: received media button intent: " + intent);

            KeyEvent keyEvent = (KeyEvent) intent.getExtras().get(Intent.EXTRA_KEY_EVENT);
            int keyCode = Utils.getAdjustedKeyCode(keyEvent);
            //Log.d(TAG, "Media Button Receiver: handling media key event: " + keyEvent);

            // Don't want to capture volume buttons
            if (Utils.isMediaButton(keyCode)) {
                AudioManager audioManager = ((AudioManager) context.getSystemService(Context.AUDIO_SERVICE));
                boolean musicActive = audioManager.isMusicActive();
                List<ResolveInfo> receivers = Utils.getMediaReceivers(context.getPackageManager(), true, context.getApplicationContext());
                String last_media_button_receiver = preferences.getString(Constants.LAST_MEDIA_BUTTON_RECEIVER, null);
                ComponentName lastReceiverComponentName = null;
                boolean ignorePrompt = false;
                boolean ignoreServices;

                if (last_media_button_receiver != null) {
                    if (last_media_button_receiver.equals(Constants.IGNORE_NEW_RECEIVER)) {
                        ignorePrompt = true;
                    } else {
                        lastReceiverComponentName = ComponentName.unflattenFromString(last_media_button_receiver);
                        if (musicActive) {
                            if (keyEvent.getAction() == KeyEvent.ACTION_UP) {
                                Utils.forwardKeyCodeToComponent(context, lastReceiverComponentName, false, keyCode, null);
                            }
                            if (isOrderedBroadcast()) {
                                abortBroadcast();
                            }
                            return;
                        }
                    }
                }

                List<RunningTaskInfo> runningTasks = activityManager.getRunningTasks(1);
                List<RunningServiceInfo> runningServices = activityManager.getRunningServices(Integer.MAX_VALUE);
                String lastReceiverPackageName = lastReceiverComponentName != null ? lastReceiverComponentName.getPackageName() : null;
                ActivityInfo activityInfo;
                for (ResolveInfo resolveInfo : receivers) {
                    activityInfo = resolveInfo.activityInfo;
                    if (activityInfo == null) {
                        continue;
                    }
                    if (MediaButtonReceiver.class.getName().equals(activityInfo.name)) {
                        continue;
                    }
                    ignoreServices = true;
                    //if music is active we should look at services or
                    //if we are looking at the app that was last open, we should look for services
                    if (musicActive
                        || (lastReceiverPackageName != null && activityInfo.packageName.equals(lastReceiverPackageName))) {
                        ignoreServices = false;
                    }
                    for (RunningTaskInfo runningTask : runningTasks) {
                        String packageName = runningTask.topActivity.getPackageName();
                        if (packageName.equals(activityInfo.packageName)) {
                            if (keyEvent.getAction() == KeyEvent.ACTION_UP) {
                                Log.d(TAG, "Found task for " + packageName + "! sending key code");
                                ComponentName componentName = new ComponentName(activityInfo.packageName, activityInfo.name);
                                Utils.forwardKeyCodeToComponent(context, componentName, false, keyCode, null);
                            }
                            if (isOrderedBroadcast()) {
                                abortBroadcast();
                            }
                            return;
                        }
                    }
                    if (ignoreServices) {
                        continue;
                    }
                    for (RunningServiceInfo runningService : runningServices) {
                        if (!runningService.started || !runningService.foreground) {
                            continue;
                        }
                        String packageName = runningService.service.getPackageName();
                        if (packageName.equals(activityInfo.packageName)) {
                            if (keyEvent.getAction() == KeyEvent.ACTION_UP) {
                                Log.d(TAG, "Found service for " + packageName + "! sending key code");
                                ComponentName componentName = new ComponentName(activityInfo.packageName, activityInfo.name);
                                Utils.forwardKeyCodeToComponent(context, componentName, false, keyCode, null);
                            }
                            if (isOrderedBroadcast()) {
                                abortBroadcast();
                            }
                            return;
                        }
                    }
                }
                if (musicActive) {
                    //music was playing but we can't handle it
                    return;
                }

                // No music playing
                if (isOrderedBroadcast()) {
                    abortBroadcast();
                }

                if (keyEvent.getAction() == KeyEvent.ACTION_UP) {

                    if (receivers == null || ignorePrompt) {
                        return;
                    }

                    if (receivers.size() > 0) {
                        //our app counts as 1 so if there's 2 then that means that we should skip our own app and do the default
                        if (receivers.size() <= 2) {
                            for (ResolveInfo resolveInfo : receivers) {
                                if (MediaButtonReceiver.class.getName().equals(resolveInfo.activityInfo.name)) {
                                    continue;
                                }
                                Utils.forwardKeyCodeToComponent(context, new ComponentName(
                                        resolveInfo.activityInfo.packageName, resolveInfo.activityInfo.name), false,
                                        keyCode, null);
                                break;
                            }
                            return;
                        }
                        showSelector(context, intent, keyEvent);
                    }
                }
            }

        }
    }

    /**
     * Shows the selector dialog that allows the user to decide which music
     * player should receiver the media button press intent.
     * 
     * @param context
     *            The context.
     * @param intent
     *            The intent to forward.
     * @param keyEvent
     *            The key event
     */
    private void showSelector(Context context, Intent intent, KeyEvent keyEvent) {
        KeyguardManager manager = (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);
        boolean locked = manager.inKeyguardRestrictedInputMode();

        Intent showForwardView = new Intent(Constants.INTENT_ACTION_VIEW_MEDIA_BUTTON_LIST);
        showForwardView.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        showForwardView.putExtras(intent);
        showForwardView.setClassName(context,
                locked ? ReceiverSelectorLocked.class.getName() : ReceiverSelector.class.getName());
        context.startActivity(showForwardView);
    }
}
