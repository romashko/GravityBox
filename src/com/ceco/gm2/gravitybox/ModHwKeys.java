/*
 * Copyright (C) 2013 Peter Gregus for GravityBox Project (C3C076@xda)
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ceco.gm2.gravitybox;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.hardware.input.InputManager;
import android.os.Build;
import android.os.Handler;
import android.os.PowerManager;
import android.os.Process;
import android.os.SystemClock;
import android.provider.Settings;
import android.view.HapticFeedbackConstants;
import android.view.KeyEvent;
import android.view.ViewConfiguration;
import android.widget.Toast;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XCallback;

public class ModHwKeys {
    private static final String TAG = "GB:ModHwKeys";
    private static final String CLASS_PHONE_WINDOW_MANAGER = "com.android.internal.policy.impl.PhoneWindowManager";
    private static final String CLASS_ACTIVITY_MANAGER_NATIVE = "android.app.ActivityManagerNative";
    private static final String CLASS_WINDOW_STATE = "android.view.WindowManagerPolicy$WindowState";
    private static final String CLASS_WINDOW_MANAGER_FUNCS = "android.view.WindowManagerPolicy.WindowManagerFuncs";
    private static final String CLASS_IWINDOW_MANAGER = "android.view.IWindowManager";
    private static final String CLASS_LOCAL_POWER_MANAGER = "android.os.LocalPowerManager";
    private static final boolean DEBUG = false;

    private static final int FLAG_WAKE = 0x00000001;
    private static final int FLAG_WAKE_DROPPED = 0x00000002;
    public static final String ACTION_SCREENSHOT = "gravitybox.intent.action.SCREENSHOT";
    public static final String ACTION_SHOW_POWER_MENU = "gravitybox.intent.action.SHOW_POWER_MENU";

    private static final String SEPARATOR = "#C3C0#";

    private static Class<?> classActivityManagerNative;
    private static Object mPhoneWindowManager;
    private static Context mContext;
    private static Context mGbContext;
    private static String mStrAppKilled;
    private static String mStrNothingToKill;
    private static String mStrNoPrevApp;
    private static String mStrCustomAppNone;
    private static String mStrCustomAppMissing;
    private static String mStrExpandedDesktopDisabled;
    private static boolean mIsMenuLongPressed = false;
    private static boolean mIsMenuDoubleTap = false;
    private static boolean mIsBackLongPressed = false;
    private static boolean mIsRecentsLongPressed = false;
    private static boolean mIsHomeLongPressed = false;
    private static int mMenuLongpressAction = 0;
    private static int mMenuDoubletapAction = 0;
    private static int mHomeLongpressAction = 0;
    private static int mHomeLongpressActionKeyguard = 0;
    private static int mBackLongpressAction = 0;
    private static int mRecentsSingletapAction = 0;
    private static int mRecentsLongpressAction = 0;
    private static int mDoubletapSpeed = GravityBoxSettings.HWKEY_DOUBLETAP_SPEED_DEFAULT;
    private static int mKillDelay = GravityBoxSettings.HWKEY_KILL_DELAY_DEFAULT;
    private static boolean mVolumeRockerWakeDisabled = false;
    private static boolean mHwKeysEnabled = true;
    private static XSharedPreferences mPrefs;

    private static List<String> mKillIgnoreList = new ArrayList<String>(Arrays.asList(
            "com.android.systemui",
            "com.mediatek.bluetooth",
            "android.process.acore",
            "com.google.process.gapps"
    ));

    private static void log(String message) {
        XposedBridge.log(TAG + ": " + message);
    }

    private static enum HwKey {
        MENU,
        HOME,
        BACK,
        RECENTS
    }

    private static enum HwKeyTrigger {
        MENU_LONGPRESS,
        MENU_DOUBLETAP,
        HOME_LONGPRESS,
        HOME_LONGPRESS_KEYGUARD,
        BACK_LONGPRESS,
        RECENTS_SINGLETAP,
        RECENTS_LONGPRESS
    }

    private static BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (DEBUG) log("Broadcast received: " + intent.toString());

            String action = intent.getAction();
            int value = GravityBoxSettings.HWKEY_ACTION_DEFAULT;
            if (intent.hasExtra(GravityBoxSettings.EXTRA_HWKEY_VALUE)) {
                value = intent.getIntExtra(GravityBoxSettings.EXTRA_HWKEY_VALUE, value);
            }

            if (action.equals(GravityBoxSettings.ACTION_PREF_HWKEY_MENU_LONGPRESS_CHANGED)) {
                mMenuLongpressAction = value;
                if (DEBUG) log("Menu long-press action set to: " + value);
            } else if (action.equals(GravityBoxSettings.ACTION_PREF_HWKEY_MENU_DOUBLETAP_CHANGED)) {
                mMenuDoubletapAction = value;
                if (DEBUG) log("Menu double-tap action set to: " + value);
            } else if (action.equals(GravityBoxSettings.ACTION_PREF_HWKEY_HOME_LONGPRESS_CHANGED)) {
                if (intent.hasExtra(GravityBoxSettings.EXTRA_HWKEY_VALUE)) {
                    mHomeLongpressAction = value;
                    if (DEBUG) log("Home long-press action set to: " + value);
                }
                if (intent.hasExtra(GravityBoxSettings.EXTRA_HWKEY_HOME_LONGPRESS_KG)) {
                    mHomeLongpressActionKeyguard = intent.getBooleanExtra(
                            GravityBoxSettings.EXTRA_HWKEY_HOME_LONGPRESS_KG, false) ?
                                    GravityBoxSettings.HWKEY_ACTION_TORCH : 
                                        GravityBoxSettings.HWKEY_ACTION_DEFAULT;
                    if (DEBUG) log("Home long-press action while keyguard on set to: " + 
                                        mHomeLongpressActionKeyguard);
                }
            } else if (action.equals(GravityBoxSettings.ACTION_PREF_HWKEY_BACK_LONGPRESS_CHANGED)) {
                mBackLongpressAction = value;
                if (DEBUG) log("Back long-press action set to: " + value);
            } else if (action.equals(GravityBoxSettings.ACTION_PREF_HWKEY_RECENTS_SINGLETAP_CHANGED)) {
                mRecentsSingletapAction = value;
                if (DEBUG) log("Recents single-tap action set to: " + value);
            } else if (action.equals(GravityBoxSettings.ACTION_PREF_HWKEY_RECENTS_LONGPRESS_CHANGED)) {
                mRecentsLongpressAction = value;
                if (DEBUG) log("Recents long-press action set to: " + value);
            } else if (action.equals(GravityBoxSettings.ACTION_PREF_HWKEY_DOUBLETAP_SPEED_CHANGED)) {
                mDoubletapSpeed = value;
                if (DEBUG) log("Doubletap speed set to: " + value);
            } else if (action.equals(GravityBoxSettings.ACTION_PREF_HWKEY_KILL_DELAY_CHANGED)) {
                mKillDelay = value;
                if (DEBUG) log("Kill delay set to: " + value);
            } else if (action.equals(GravityBoxSettings.ACTION_PREF_VOLUME_ROCKER_WAKE_CHANGED)) {
                mVolumeRockerWakeDisabled = intent.getBooleanExtra(
                        GravityBoxSettings.EXTRA_VOLUME_ROCKER_WAKE_DISABLE, false);
                if (DEBUG) log("mVolumeRockerWakeDisabled set to: " + mVolumeRockerWakeDisabled);
            } else if (action.equals(GravityBoxSettings.ACTION_PREF_PIE_CHANGED) && 
                    intent.hasExtra(GravityBoxSettings.EXTRA_PIE_HWKEYS_DISABLE)) {
                mHwKeysEnabled = !intent.getBooleanExtra(GravityBoxSettings.EXTRA_PIE_HWKEYS_DISABLE, false);
            } else if (action.equals(ACTION_SCREENSHOT) && mPhoneWindowManager != null) {
                XposedHelpers.callMethod(mPhoneWindowManager, "takeScreenshot");
            } else if (action.equals(GravityBoxSettings.ACTION_PREF_DISPLAY_ALLOW_ALL_ROTATIONS_CHANGED)) {
                final boolean allowAllRotations = intent.getBooleanExtra(
                        GravityBoxSettings.EXTRA_ALLOW_ALL_ROTATIONS, false);
                if (mPhoneWindowManager != null) {
                    XposedHelpers.setIntField(mPhoneWindowManager, "mAllowAllRotations",
                            allowAllRotations ? 1 : 0);
                }
            } else if (action.equals(ACTION_SHOW_POWER_MENU) && mPhoneWindowManager != null) {
                XposedHelpers.callMethod(mPhoneWindowManager, "showGlobalActionsDialog");
            }
        }
    };

    public static void initZygote(final XSharedPreferences prefs) {
        try {
            mPrefs = prefs;
            try {
                mMenuLongpressAction = Integer.valueOf(
                        prefs.getString(GravityBoxSettings.PREF_KEY_HWKEY_MENU_LONGPRESS, "0"));
                mMenuDoubletapAction = Integer.valueOf(
                        prefs.getString(GravityBoxSettings.PREF_KEY_HWKEY_MENU_DOUBLETAP, "0"));
                mHomeLongpressAction = Integer.valueOf(
                        prefs.getString(GravityBoxSettings.PREF_KEY_HWKEY_HOME_LONGPRESS, "0"));
                mBackLongpressAction = Integer.valueOf(
                        prefs.getString(GravityBoxSettings.PREF_KEY_HWKEY_BACK_LONGPRESS, "0"));
                mRecentsSingletapAction = Integer.valueOf(
                        prefs.getString(GravityBoxSettings.PREF_KEY_HWKEY_RECENTS_SINGLETAP, "0"));
                mRecentsLongpressAction = Integer.valueOf(
                        prefs.getString(GravityBoxSettings.PREF_KEY_HWKEY_RECENTS_LONGPRESS, "0"));
                mDoubletapSpeed = Integer.valueOf(
                        prefs.getString(GravityBoxSettings.PREF_KEY_HWKEY_DOUBLETAP_SPEED, "400"));
                mKillDelay = Integer.valueOf(
                        prefs.getString(GravityBoxSettings.PREF_KEY_HWKEY_KILL_DELAY, "1000"));
            } catch (NumberFormatException e) {
                XposedBridge.log(e);
            }

            mHomeLongpressActionKeyguard = prefs.getBoolean(
                    GravityBoxSettings.PREF_KEY_HWKEY_HOME_LONGPRESS_KEYGUARD, false) ?
                            GravityBoxSettings.HWKEY_ACTION_TORCH : GravityBoxSettings.HWKEY_ACTION_DEFAULT;
            mVolumeRockerWakeDisabled = prefs.getBoolean(
                    GravityBoxSettings.PREF_KEY_VOLUME_ROCKER_WAKE_DISABLE, false);
            mHwKeysEnabled = !prefs.getBoolean(GravityBoxSettings.PREF_KEY_HWKEYS_DISABLE, false);

            final Class<?> classPhoneWindowManager = XposedHelpers.findClass(CLASS_PHONE_WINDOW_MANAGER, null);
            classActivityManagerNative = XposedHelpers.findClass(CLASS_ACTIVITY_MANAGER_NATIVE, null);

            if (Build.VERSION.SDK_INT > 16) {
                XposedHelpers.findAndHookMethod(classPhoneWindowManager, "init",
                    Context.class, CLASS_IWINDOW_MANAGER, CLASS_WINDOW_MANAGER_FUNCS, phoneWindowManagerInitHook);
            } else {
                XposedHelpers.findAndHookMethod(classPhoneWindowManager, "init",
                        Context.class, CLASS_IWINDOW_MANAGER, CLASS_WINDOW_MANAGER_FUNCS, 
                        CLASS_LOCAL_POWER_MANAGER, phoneWindowManagerInitHook);
            }

            XposedHelpers.findAndHookMethod(classPhoneWindowManager, "interceptKeyBeforeQueueing", 
                    KeyEvent.class, int.class, boolean.class, new XC_MethodHook(XCallback.PRIORITY_HIGHEST) {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    KeyEvent event = (KeyEvent) param.args[0];
                    int keyCode = event.getKeyCode();
                    boolean down = event.getAction() == KeyEvent.ACTION_DOWN;
                    boolean keyguardOn = (Boolean) XposedHelpers.callMethod(mPhoneWindowManager, "keyguardOn");
                    Handler handler = (Handler) XposedHelpers.getObjectField(param.thisObject, "mHandler");

                    if (mVolumeRockerWakeDisabled && 
                            (keyCode == KeyEvent.KEYCODE_VOLUME_UP ||
                                    keyCode == KeyEvent.KEYCODE_VOLUME_DOWN)) {
                        int policyFlags = (Integer) param.args[1];
                        policyFlags &= ~FLAG_WAKE;
                        policyFlags &= ~FLAG_WAKE_DROPPED;
                        param.args[1] = policyFlags;
                        return;
                    }

                    if (keyCode == KeyEvent.KEYCODE_HOME) {
                        if (!down) {
                            handler.removeCallbacks(mHomeLongPressKeyguard);
                            if (mIsHomeLongPressed) {
                                mIsHomeLongPressed = false;
                                param.setResult(0);
                                return;
                            }
                            if (!mHwKeysEnabled && 
                                    event.getRepeatCount() == 0 &&
                                    (event.getFlags() & KeyEvent.FLAG_FROM_SYSTEM) != 0) {
                               if (DEBUG) log("HOME KeyEvent coming from HW key and keys disabled. Ignoring.");
                               param.setResult(0);
                               return;
                           }
                        } else if (keyguardOn) {
                            if (event.getRepeatCount() == 0) {
                                mIsHomeLongPressed = false;
                                if (mHomeLongpressActionKeyguard != GravityBoxSettings.HWKEY_ACTION_DEFAULT) {
                                    handler.postDelayed(mHomeLongPressKeyguard, 
                                            getLongpressTimeoutForAction(mHomeLongpressActionKeyguard));
                                }
                            } else {
                                if (mHomeLongpressActionKeyguard != GravityBoxSettings.HWKEY_ACTION_DEFAULT) {
                                    param.setResult(0);
                                }
                                return;
                            }
                        }
                    }
                }
            });

            XposedHelpers.findAndHookMethod(classPhoneWindowManager, "interceptKeyBeforeDispatching", 
                    CLASS_WINDOW_STATE, KeyEvent.class, int.class, new XC_MethodHook() {

                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    if ((Boolean) XposedHelpers.callMethod(mPhoneWindowManager, "keyguardOn")) return;

                    KeyEvent event = (KeyEvent) param.args[1];
                    int keyCode = event.getKeyCode();
                    boolean down = event.getAction() == KeyEvent.ACTION_DOWN;
                    Handler mHandler = (Handler) XposedHelpers.getObjectField(param.thisObject, "mHandler");
                    if (DEBUG) log("keyCode=" + keyCode);

                    if (keyCode == KeyEvent.KEYCODE_MENU) {
                        if (!hasAction(HwKey.MENU) && mHwKeysEnabled) return;

                        if (!down) {
                            mHandler.removeCallbacks(mMenuLongPress);
                            if (mIsMenuLongPressed) {
                                param.setResult(-1);
                                return;
                            }
                            if (!mHwKeysEnabled &&
                                    event.getRepeatCount() == 0 && 
                                    ((event.getFlags() & KeyEvent.FLAG_FROM_SYSTEM) != 0)) {
                                if (DEBUG) log("MENU KeyEvent coming from HW key and keys disabled. Ignoring.");
                                param.setResult(-1);
                                return;
                            }
                        } else {
                            if (event.getRepeatCount() == 0) {
                                if (mIsMenuDoubleTap) {
                                    performAction(HwKeyTrigger.MENU_DOUBLETAP);
                                    mHandler.removeCallbacks(mMenuDoubleTapReset);
                                    mIsMenuDoubleTap = false;
                                    param.setResult(-1);
                                    return;
                                } else {
                                    mIsMenuLongPressed = false;
                                    mIsMenuDoubleTap = false;
                                    if (mMenuDoubletapAction != GravityBoxSettings.HWKEY_ACTION_DEFAULT) {
                                        mIsMenuDoubleTap = true;
                                        mHandler.postDelayed(mMenuDoubleTapReset, mDoubletapSpeed);
                                    }
                                    if (mMenuLongpressAction != GravityBoxSettings.HWKEY_ACTION_DEFAULT) {
                                        mHandler.postDelayed(mMenuLongPress, 
                                                getLongpressTimeoutForAction(mMenuLongpressAction));
                                    }
                                }
                            } else {
                                if (mMenuLongpressAction != GravityBoxSettings.HWKEY_ACTION_DEFAULT) {
                                    param.setResult(-1);
                                }
                                return;
                            }
                        }
                    }

                    if (keyCode == KeyEvent.KEYCODE_BACK) {
                        if (!hasAction(HwKey.BACK) && mHwKeysEnabled) return;

                        if (!down) {
                            mHandler.removeCallbacks(mBackLongPress);
                            if (mIsBackLongPressed) {
                                param.setResult(-1);
                                return;
                            }
                            if (!mHwKeysEnabled &&
                                    event.getRepeatCount() == 0 && 
                                    ((event.getFlags() & KeyEvent.FLAG_FROM_SYSTEM) != 0)) {
                                if (DEBUG) log("BACK KeyEvent coming from HW key and keys disabled. Ignoring.");
                                param.setResult(-1);
                                return;
                            }
                        } else {
                            if (event.getRepeatCount() == 0) {
                                mIsBackLongPressed = false;
                                if (mBackLongpressAction != GravityBoxSettings.HWKEY_ACTION_DEFAULT) {
                                    mHandler.postDelayed(mBackLongPress, 
                                            getLongpressTimeoutForAction(mBackLongpressAction));
                                }
                            } else {
                                param.setResult(-1);
                                return;
                            }
                        }
                    }

                    if (keyCode == KeyEvent.KEYCODE_APP_SWITCH) {
                        if (!hasAction(HwKey.RECENTS)) return;

                        if (!down) {
                            mHandler.removeCallbacks(mRecentsLongPress);
                            if (!mIsRecentsLongPressed) {
                                if (mRecentsSingletapAction != GravityBoxSettings.HWKEY_ACTION_DEFAULT) {
                                    performAction(HwKeyTrigger.RECENTS_SINGLETAP);
                                } else {
                                    toggleRecentApps();
                                }
                            }
                        } else {
                            if (event.getRepeatCount() == 0) {
                                mIsRecentsLongPressed = false;
                                if (mRecentsLongpressAction != GravityBoxSettings.HWKEY_ACTION_DEFAULT) {
                                    mHandler.postDelayed(mRecentsLongPress, 
                                            getLongpressTimeoutForAction(mRecentsLongpressAction));
                                }
                            }
                        }
                        param.setResult(-1);
                        return;
                    }
                }
            });

            XposedHelpers.findAndHookMethod(classPhoneWindowManager, "handleLongPressOnHome", new XC_MethodReplacement() {

                @Override
                protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                    if (!hasAction(HwKey.HOME)) {
                        XposedBridge.invokeOriginalMethod(param.method, param.thisObject, param.args);
                        return null;
                    }

                    if (Build.VERSION.SDK_INT > 17) {
                        XposedHelpers.setBooleanField(param.thisObject, "mHomeConsumed", true);
                    } else {
                        XposedHelpers.setBooleanField(param.thisObject, "mHomeLongPressed", true);
                    }
                    performAction(HwKeyTrigger.HOME_LONGPRESS);

                    return null;
                }
            });

            if (Build.VERSION.SDK_INT > 16) {
                XposedHelpers.findAndHookMethod(classPhoneWindowManager, 
                        "isWakeKeyWhenScreenOff", int.class, new XC_MethodHook() {
    
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        int keyCode = (Integer) param.args[0];
                        if (mVolumeRockerWakeDisabled && 
                                (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN ||
                                 keyCode == KeyEvent.KEYCODE_VOLUME_UP)) {
                            param.setResult(false);
                        }
                    }
                });
            }
        } catch (Throwable t) {
            XposedBridge.log(t);
        }
    }

    private static XC_MethodHook phoneWindowManagerInitHook = new XC_MethodHook() {
        @Override
        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
            mPhoneWindowManager = param.thisObject;
            mContext = (Context) XposedHelpers.getObjectField(mPhoneWindowManager, "mContext");
            mGbContext = mContext.createPackageContext(GravityBox.PACKAGE_NAME, Context.CONTEXT_IGNORE_SECURITY);
            XposedHelpers.setIntField(mPhoneWindowManager, "mAllowAllRotations", 
                    mPrefs.getBoolean(GravityBoxSettings.PREF_KEY_DISPLAY_ALLOW_ALL_ROTATIONS, false) ? 1 : 0);

            Resources res = mGbContext.getResources();
            mStrAppKilled = res.getString(R.string.app_killed);
            mStrNothingToKill = res.getString(R.string.nothing_to_kill);
            mStrNoPrevApp = res.getString(R.string.no_previous_app_found);
            mStrCustomAppNone = res.getString(R.string.hwkey_action_custom_app_none);
            mStrCustomAppMissing = res.getString(R.string.hwkey_action_custom_app_missing);
            mStrExpandedDesktopDisabled = res.getString(R.string.hwkey_action_expanded_desktop_disabled);
    
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(GravityBoxSettings.ACTION_PREF_HWKEY_MENU_LONGPRESS_CHANGED);
            intentFilter.addAction(GravityBoxSettings.ACTION_PREF_HWKEY_MENU_DOUBLETAP_CHANGED);
            intentFilter.addAction(GravityBoxSettings.ACTION_PREF_HWKEY_HOME_LONGPRESS_CHANGED);
            intentFilter.addAction(GravityBoxSettings.ACTION_PREF_HWKEY_BACK_LONGPRESS_CHANGED);
            intentFilter.addAction(GravityBoxSettings.ACTION_PREF_HWKEY_RECENTS_SINGLETAP_CHANGED);
            intentFilter.addAction(GravityBoxSettings.ACTION_PREF_HWKEY_RECENTS_LONGPRESS_CHANGED);
            intentFilter.addAction(GravityBoxSettings.ACTION_PREF_HWKEY_DOUBLETAP_SPEED_CHANGED);
            intentFilter.addAction(GravityBoxSettings.ACTION_PREF_HWKEY_KILL_DELAY_CHANGED);
            intentFilter.addAction(GravityBoxSettings.ACTION_PREF_VOLUME_ROCKER_WAKE_CHANGED);
            intentFilter.addAction(GravityBoxSettings.ACTION_PREF_PIE_CHANGED);
            intentFilter.addAction(ACTION_SCREENSHOT);
            intentFilter.addAction(ACTION_SHOW_POWER_MENU);
            intentFilter.addAction(GravityBoxSettings.ACTION_PREF_DISPLAY_ALLOW_ALL_ROTATIONS_CHANGED);
            mContext.registerReceiver(mBroadcastReceiver, intentFilter);

            if (DEBUG) log("Phone window manager initialized");
        }
    };

    private static Runnable mMenuLongPress = new Runnable() {

        @Override
        public void run() {
            if (DEBUG) log("mMenuLongPress runnable launched");
            mIsMenuLongPressed = true;
            performAction(HwKeyTrigger.MENU_LONGPRESS);
        }
    };

    private static Runnable mMenuDoubleTapReset = new Runnable() {

        @Override
        public void run() {
            if (DEBUG) log("menu key double tap timed out");
            mIsMenuDoubleTap = false;
        }
    };

    private static Runnable mBackLongPress = new Runnable() {

        @Override
        public void run() {
            if (DEBUG) log("mBackLongPress runnable launched");
            mIsBackLongPressed = true;
            performAction(HwKeyTrigger.BACK_LONGPRESS);
        }
    };

    private static Runnable mRecentsLongPress = new Runnable() {

        @Override
        public void run() {
            if (DEBUG) log("mRecentsLongPress runnable launched");
            mIsRecentsLongPressed = true;
            performAction(HwKeyTrigger.RECENTS_LONGPRESS);
        }
    };

    private static Runnable mHomeLongPressKeyguard = new Runnable() {

        @Override
        public void run() {
            if (DEBUG) log("mHomeLongPressKeyguard runnable launched");
            mIsHomeLongPressed = true;
            performAction(HwKeyTrigger.HOME_LONGPRESS_KEYGUARD);
        }
    };

    private static int getActionForHwKeyTrigger(HwKeyTrigger keyTrigger) {
        int action = GravityBoxSettings.HWKEY_ACTION_DEFAULT;

        if (keyTrigger == HwKeyTrigger.MENU_LONGPRESS) {
            action = mMenuLongpressAction;
        } else if (keyTrigger == HwKeyTrigger.MENU_DOUBLETAP) {
            action = mMenuDoubletapAction;
        } else if (keyTrigger == HwKeyTrigger.HOME_LONGPRESS) {
            action = mHomeLongpressAction;
        } else if (keyTrigger == HwKeyTrigger.HOME_LONGPRESS_KEYGUARD) {
            action = mHomeLongpressActionKeyguard;
        } else if (keyTrigger == HwKeyTrigger.BACK_LONGPRESS) {
            action = mBackLongpressAction;
        } else if (keyTrigger == HwKeyTrigger.RECENTS_SINGLETAP) {
            action = mRecentsSingletapAction;
        } else if (keyTrigger == HwKeyTrigger.RECENTS_LONGPRESS) {
            action = mRecentsLongpressAction;
        }

        if (DEBUG) log("Action for HWKEY trigger " + keyTrigger + " = " + action);
        return action;
    }

    private static boolean hasAction(HwKey key) {
        boolean retVal = false;
        if (key == HwKey.MENU) {
            retVal |= getActionForHwKeyTrigger(HwKeyTrigger.MENU_LONGPRESS) != GravityBoxSettings.HWKEY_ACTION_DEFAULT;
            retVal |= getActionForHwKeyTrigger(HwKeyTrigger.MENU_DOUBLETAP) != GravityBoxSettings.HWKEY_ACTION_DEFAULT;
        } else if (key == HwKey.HOME) {
            retVal |= getActionForHwKeyTrigger(HwKeyTrigger.HOME_LONGPRESS) != GravityBoxSettings.HWKEY_ACTION_DEFAULT;
        } else if (key == HwKey.BACK) {
            retVal |= getActionForHwKeyTrigger(HwKeyTrigger.BACK_LONGPRESS) != GravityBoxSettings.HWKEY_ACTION_DEFAULT;
        } else if (key == HwKey.RECENTS) {
            retVal |= getActionForHwKeyTrigger(HwKeyTrigger.RECENTS_SINGLETAP) != GravityBoxSettings.HWKEY_ACTION_DEFAULT;
            retVal |= getActionForHwKeyTrigger(HwKeyTrigger.RECENTS_LONGPRESS) != GravityBoxSettings.HWKEY_ACTION_DEFAULT;
        }

        if (DEBUG) log("HWKEY " + key + " has action = " + retVal);
        return retVal;
    }

    private static int getLongpressTimeoutForAction(int action) {
        return (action == GravityBoxSettings.HWKEY_ACTION_KILL) ?
                mKillDelay : ViewConfiguration.getLongPressTimeout();
    }

    private static void performAction(HwKeyTrigger keyTrigger) {
        int action = getActionForHwKeyTrigger(keyTrigger);
        if (DEBUG) log("Performing action " + action + " for HWKEY trigger " + keyTrigger.toString());

        if (action == GravityBoxSettings.HWKEY_ACTION_DEFAULT) return;

        if (action == GravityBoxSettings.HWKEY_ACTION_SEARCH) {
            launchSearchActivity();
        } else if (action == GravityBoxSettings.HWKEY_ACTION_VOICE_SEARCH) {
            launchVoiceSearchActivity();
        } else if (action == GravityBoxSettings.HWKEY_ACTION_PREV_APP) {
            switchToLastApp();
        } else if (action == GravityBoxSettings.HWKEY_ACTION_KILL) {
            killForegroundApp();
        } else if (action == GravityBoxSettings.HWKEY_ACTION_SLEEP) {
            goToSleep();
        } else if (action == GravityBoxSettings.HWKEY_ACTION_RECENT_APPS) {
            toggleRecentApps();
        } else if (action == GravityBoxSettings.HWKEY_ACTION_CUSTOM_APP
                    || action == GravityBoxSettings.HWKEY_ACTION_CUSTOM_APP2) {
            launchCustomApp(action);
        } else if (action == GravityBoxSettings.HWKEY_ACTION_MENU) {
            injectMenuKey();
        } else if (action == GravityBoxSettings.HWKEY_ACTION_EXPANDED_DESKTOP) {
            toggleExpandedDesktop();
        } else if (action == GravityBoxSettings.HWKEY_ACTION_TORCH) {
            toggleTorch();
        }
    }

    private static void launchSearchActivity() {
        try {
            XposedHelpers.callMethod(mPhoneWindowManager, "launchAssistAction");
        } catch (Exception e) {
            XposedBridge.log(e);
        }
    }

    private static void launchVoiceSearchActivity() {
        try {
            XposedHelpers.callMethod(mPhoneWindowManager, "launchAssistLongPressAction");
        } catch (Exception e) {
            XposedBridge.log(e);
        }
    }

    private static void killForegroundApp() {
        Handler handler = (Handler) XposedHelpers.getObjectField(mPhoneWindowManager, "mHandler");
        if (handler == null) return;

        handler.post(
            new Runnable() {
                @Override
                public void run() {
                    try {
                        final Intent intent = new Intent(Intent.ACTION_MAIN);
                        final PackageManager pm = mContext.getPackageManager();
                        String defaultHomePackage = "com.android.launcher";
                        intent.addCategory(Intent.CATEGORY_HOME);
                        
                        final ResolveInfo res = pm.resolveActivity(intent, 0);
                        if (res.activityInfo != null && !res.activityInfo.packageName.equals("android")) {
                            defaultHomePackage = res.activityInfo.packageName;
                        }
        
                        Object mgr = XposedHelpers.callStaticMethod(classActivityManagerNative, "getDefault");
        
                        @SuppressWarnings("unchecked")
                        List<RunningAppProcessInfo> apps = (List<RunningAppProcessInfo>) 
                                XposedHelpers.callMethod(mgr, "getRunningAppProcesses");
        
                        String targetKilled = null;
                        for (RunningAppProcessInfo appInfo : apps) {  
                            int uid = appInfo.uid;  
                            // Make sure it's a foreground user application (not system,  
                            // root, phone, etc.)  
                            if (uid >= Process.FIRST_APPLICATION_UID && uid <= Process.LAST_APPLICATION_UID  
                                    && appInfo.importance == RunningAppProcessInfo.IMPORTANCE_FOREGROUND &&
                                    !mKillIgnoreList.contains(appInfo.processName) &&
                                    !appInfo.processName.equals(defaultHomePackage)) {  
                                if (DEBUG) log("Killing process ID " + appInfo.pid + ": " + appInfo.processName);
                                Process.killProcess(appInfo.pid);
                                targetKilled = appInfo.processName;
                                try {
                                    targetKilled = (String) pm.getApplicationLabel(
                                            pm.getApplicationInfo(targetKilled, 0));
                                } catch (PackageManager.NameNotFoundException nfe) {
                                    //
                                }
                                break;
                            }  
                        }
        
                        if (targetKilled != null) {
                            Class<?>[] paramArgs = new Class<?>[3];
                            paramArgs[0] = XposedHelpers.findClass(CLASS_WINDOW_STATE, null);
                            paramArgs[1] = int.class;
                            paramArgs[2] = boolean.class;
                            XposedHelpers.callMethod(mPhoneWindowManager, "performHapticFeedbackLw",
                                    paramArgs, null, HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING, true);
                            Toast.makeText(mContext, 
                                    String.format(mStrAppKilled, targetKilled), Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(mContext, mStrNothingToKill, Toast.LENGTH_SHORT).show();
                        }
                    } catch (Exception e) {  
                        XposedBridge.log(e);  
                    }
                }
            }
         );
    }

    private static void switchToLastApp() {
        Handler handler = (Handler) XposedHelpers.getObjectField(mPhoneWindowManager, "mHandler");
        if (handler == null) return;

        handler.post(
            new Runnable() {
                @Override
                public void run() {
                    int lastAppId = 0;
                    int looper = 1;
                    String packageName;
                    final Intent intent = new Intent(Intent.ACTION_MAIN);
                    final ActivityManager am = (ActivityManager) mContext
                            .getSystemService(Context.ACTIVITY_SERVICE);
                    String defaultHomePackage = "com.android.launcher";
                    intent.addCategory(Intent.CATEGORY_HOME);
                    final ResolveInfo res = mContext.getPackageManager().resolveActivity(intent, 0);
                    if (res.activityInfo != null && !res.activityInfo.packageName.equals("android")) {
                        defaultHomePackage = res.activityInfo.packageName;
                    }
                    List <ActivityManager.RunningTaskInfo> tasks = am.getRunningTasks(5);
                    // lets get enough tasks to find something to switch to
                    // Note, we'll only get as many as the system currently has - up to 5
                    while ((lastAppId == 0) && (looper < tasks.size())) {
                        packageName = tasks.get(looper).topActivity.getPackageName();
                        if (!packageName.equals(defaultHomePackage) && !packageName.equals("com.android.systemui")) {
                            lastAppId = tasks.get(looper).id;
                        }
                        looper++;
                    }
                    if (lastAppId != 0) {
                        am.moveTaskToFront(lastAppId, ActivityManager.MOVE_TASK_NO_USER_ACTION);
                    } else {
                        Toast.makeText(mContext, mStrNoPrevApp, Toast.LENGTH_SHORT).show();
                    }
                }
            }
        );
    }

    private static void goToSleep() {
        try {
            PowerManager pm = (PowerManager) mContext.getSystemService(Context.POWER_SERVICE);
            pm.goToSleep(SystemClock.uptimeMillis());
        } catch (Exception e) {
            XposedBridge.log(e);
        }
    }

    private static void toggleRecentApps() {
        try {
            if (Build.VERSION.SDK_INT > 17) {
                XposedHelpers.callMethod(mPhoneWindowManager, "toggleRecentApps");
            } else {
                Object statusbar = XposedHelpers.callMethod(mPhoneWindowManager, "getStatusBarService");
                if (statusbar != null) {
                    XposedHelpers.callMethod(statusbar, "toggleRecentApps");
                }
            }
        } catch (Throwable t) {
            XposedBridge.log(t);
        }
    }

    private static void launchCustomApp(final int action) {
        Handler handler = (Handler) XposedHelpers.getObjectField(mPhoneWindowManager, "mHandler");
        if (handler == null) return;
        mPrefs.reload();

        handler.post(
            new Runnable() {
                @Override
                public void run() {
                    try {
                        String appInfo = (action == GravityBoxSettings.HWKEY_ACTION_CUSTOM_APP) ?
                                mPrefs.getString(GravityBoxSettings.PREF_KEY_HWKEY_CUSTOM_APP, null) :
                                    mPrefs.getString(GravityBoxSettings.PREF_KEY_HWKEY_CUSTOM_APP2, null);
                        if (appInfo == null) {
                            Toast.makeText(mContext, mStrCustomAppNone, Toast.LENGTH_SHORT).show();
                            return;
                        }

                        String[] splitValue = appInfo.split(SEPARATOR);
                        ComponentName cn = new ComponentName(splitValue[0], splitValue[1]);
                        Intent i = new Intent();
                        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        i.setComponent(cn);
                        mContext.startActivity(i);
                    } catch (ActivityNotFoundException e) {
                        Toast.makeText(mContext, mStrCustomAppMissing, Toast.LENGTH_SHORT).show();
                    } catch (Throwable t) {
                        XposedBridge.log(t);
                    }
                }
            }
        );
    }

    private static void injectMenuKey() {
        Handler handler = (Handler) XposedHelpers.getObjectField(mPhoneWindowManager, "mHandler");
        if (handler == null) return;

        handler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    final long eventTime = SystemClock.uptimeMillis();
                    final InputManager inputManager = (InputManager)
                            mContext.getSystemService(Context.INPUT_SERVICE);
                    XposedHelpers.callMethod(inputManager, "injectInputEvent",
                            new KeyEvent(eventTime - 50, eventTime - 50, KeyEvent.ACTION_DOWN, 
                                    KeyEvent.KEYCODE_MENU, 0), 0);
                    XposedHelpers.callMethod(inputManager, "injectInputEvent",
                            new KeyEvent(eventTime - 50, eventTime - 25, KeyEvent.ACTION_UP, 
                                    KeyEvent.KEYCODE_MENU, 0), 0);
                } catch (Throwable t) {
                        XposedBridge.log(t);
                }
            }
        });
    }

    private static void toggleExpandedDesktop() {
        Handler handler = (Handler) XposedHelpers.getObjectField(mPhoneWindowManager, "mHandler");
        if (handler == null) return;

        handler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    final ContentResolver resolver = mContext.getContentResolver();
                    final int edMode = Settings.System.getInt(resolver,
                            ModExpandedDesktop.SETTING_EXPANDED_DESKTOP_MODE, 0);
                    if (edMode == GravityBoxSettings.ED_DISABLED) {
                        Toast.makeText(mContext, mStrExpandedDesktopDisabled, Toast.LENGTH_SHORT).show();
                    } else {
                        final int edState = Settings.System.getInt(resolver,
                                ModExpandedDesktop.SETTING_EXPANDED_DESKTOP_STATE, 0);
                        Settings.System.putInt(resolver, 
                                ModExpandedDesktop.SETTING_EXPANDED_DESKTOP_STATE,
                                (edState == 1) ? 0 : 1);
                    }
                } catch (Throwable t) {
                        XposedBridge.log(t);
                }
            }
        });
    }

    private static void toggleTorch() {
        try {
            Intent intent = new Intent(mGbContext, TorchService.class);
            intent.setAction(TorchService.ACTION_TOGGLE_TORCH);
            mGbContext.startService(intent);
        } catch (Throwable t) {
            log("Error toggling Torch: " + t.getMessage());
        }
    }
}