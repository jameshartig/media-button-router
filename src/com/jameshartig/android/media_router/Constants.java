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
package com.jameshartig.android.media_router;

/**
 * Constants.
 * 
 * @author Harleen Sahni
 * @author James Hartig
 */
public class Constants {

    private Constants() {
        // Intentionally blank
    }

    public static final String TAG = "MediaButtonRouter";
    public static final String INTENT_ACTION_VIEW_MEDIA_BUTTON_LIST = "com.jameshartig.android.VIEW_MEDIA_LIST";
    public static final String INTENT_ACTION_VIEW_MEDIA_LIST_KEYPRESS = "com.jameshartig.android.VIEW_MEDIA_LIST_KEYPRESS";
    public static final String ENABLED_PREF_KEY = "enable_receiver";
    public static final String TIMEOUT_KEY = "timeout";
    public static final String HIDDEN_APPS_KEY = "hidden_apps";
    public static final String LAST_MEDIA_BUTTON_RECEIVER = "last_media_button_receiver";
    public static final String IGNORE_NEW_RECEIVER = "ignoreNewReceiver";
}
