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
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

import com.ceco.gm2.gravitybox.preference.AppPickerPreference;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LayoutInflated;
import de.robv.android.xposed.callbacks.XC_InitPackageResources.InitPackageResourcesParam;
import android.app.Notification;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.provider.Settings;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.format.DateFormat;
import android.text.style.RelativeSizeSpan;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.animation.Animation;
import android.widget.LinearLayout;
import android.widget.TextView;

public class ModStatusBar {
    public static final String PACKAGE_NAME = "com.android.systemui";
    private static final String TAG = "GB:ModStatusBar";
    private static final String CLASS_PHONE_STATUSBAR = "com.android.systemui.statusbar.phone.PhoneStatusBar";
    private static final String CLASS_TICKER = "com.android.systemui.statusbar.phone.PhoneStatusBar$MyTicker";
    private static final String CLASS_PHONE_STATUSBAR_POLICY = "com.android.systemui.statusbar.phone.PhoneStatusBarPolicy";
    private static final String CLASS_POWER_MANAGER = "android.os.PowerManager";
    private static final String CLASS_LOCATION_CONTROLLER = "com.android.systemui.statusbar.policy.LocationController";
    private static final String CLASS_STATUSBAR_NOTIF = "com.android.internal.statusbar.StatusBarNotification";
    private static final boolean DEBUG = false;

    private static final float BRIGHTNESS_CONTROL_PADDING = 0.15f;
    private static final int BRIGHTNESS_CONTROL_LONG_PRESS_TIMEOUT = 750; // ms
    private static final int BRIGHTNESS_CONTROL_LINGER_THRESHOLD = 20;
    private static final int STATUS_BAR_DISABLE_EXPAND = 0x00010000;
    public static final String SETTING_ONGOING_NOTIFICATIONS = "gb_ongoing_notifications";

    private static ViewGroup mIconArea;
    private static ViewGroup mRootView;
    private static LinearLayout mLayoutClock;
    private static TextView mClock;
    private static TextView mClockExpanded;
    private static Object mPhoneStatusBar;
    private static Object mStatusBarView;
    private static Context mContext;
    private static int mAnimPushUpOut;
    private static int mAnimPushDownIn;
    private static int mAnimFadeIn;
    private static boolean mClockCentered = false;
    private static int mClockOriginalPaddingLeft;
    private static int mClockShowDow = GravityBoxSettings.DOW_DISABLED;
    private static boolean mAmPmHide = false;
    private static boolean mClockHide = false;
    private static String mClockLink;
    private static boolean mAlarmHide = false;
    private static Object mPhoneStatusBarPolicy;
    private static SettingsObserver mSettingsObserver;
    private static String mOngoingNotif;
    private static TrafficMeter mTrafficMeter;
    private static ViewGroup mSbContents;

    // Brightness control
    private static boolean mBrightnessControlEnabled;
    private static boolean mBrightnessControl;
    private static float mScreenWidth;
    private static int mMinBrightness;
    private static int mLinger;
    private static int mInitialTouchX;
    private static int mInitialTouchY;
    private static int BRIGHTNESS_ON = 255;
    private static VelocityTracker mVelocityTracker;

    private static void log(String message) {
        XposedBridge.log(TAG + ": " + message);
    }

    private static BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (DEBUG) log("Broadcast received: " + intent.toString());
            if (intent.getAction().equals(GravityBoxSettings.ACTION_PREF_CLOCK_CHANGED)) {
                if (intent.hasExtra(GravityBoxSettings.EXTRA_CENTER_CLOCK)) {
                    setClockPosition(intent.getBooleanExtra(GravityBoxSettings.EXTRA_CENTER_CLOCK, false));
                    updateTrafficMeterPosition();
                }
                if (intent.hasExtra(GravityBoxSettings.EXTRA_CLOCK_DOW)) {
                    mClockShowDow = intent.getIntExtra(GravityBoxSettings.EXTRA_CLOCK_DOW,
                            GravityBoxSettings.DOW_DISABLED);
                    if (mClock != null) {
                        XposedHelpers.callMethod(mClock, "updateClock");
                    }
                }
                if (intent.hasExtra(GravityBoxSettings.EXTRA_AMPM_HIDE)) {
                    mAmPmHide = intent.getBooleanExtra(GravityBoxSettings.EXTRA_AMPM_HIDE, false);
                    if (mClock != null) {
                        XposedHelpers.callMethod(mClock, "updateClock");
                    }
                    if (mClockExpanded != null) {
                        XposedHelpers.callMethod(mClockExpanded, "updateClock");
                    }
                }
                if (intent.hasExtra(GravityBoxSettings.EXTRA_CLOCK_HIDE)) {
                    mClockHide = intent.getBooleanExtra(GravityBoxSettings.EXTRA_CLOCK_HIDE, false);
                    if (mClock != null) {
                        mClock.setVisibility(mClockHide ? View.GONE : View.VISIBLE);
                    }
                }
                if (intent.hasExtra(GravityBoxSettings.EXTRA_CLOCK_LINK)) {
                    mClockLink = intent.getStringExtra(GravityBoxSettings.EXTRA_CLOCK_LINK);
                }
                if (intent.hasExtra(GravityBoxSettings.EXTRA_ALARM_HIDE)) {
                    mAlarmHide = intent.getBooleanExtra(GravityBoxSettings.EXTRA_ALARM_HIDE, false);
                    if (mPhoneStatusBarPolicy != null) {
                        String alarmFormatted = Settings.System.getString(context.getContentResolver(),
                                Settings.System.NEXT_ALARM_FORMATTED);
                        Intent i = new Intent();
                        i.putExtra("alarmSet", (alarmFormatted != null && !alarmFormatted.isEmpty()));
                        XposedHelpers.callMethod(mPhoneStatusBarPolicy, "updateAlarm", i);
                    }
                }
            } else if (intent.getAction().equals(GravityBoxSettings.ACTION_PREF_STATUSBAR_BRIGHTNESS_CHANGED)
                    && intent.hasExtra(GravityBoxSettings.EXTRA_SB_BRIGHTNESS)) {
                mBrightnessControlEnabled = intent.getBooleanExtra(
                        GravityBoxSettings.EXTRA_SB_BRIGHTNESS, false);
                if (mSettingsObserver != null) {
                    mSettingsObserver.update();
                }
            } else if (intent.getAction().equals(
                    GravityBoxSettings.ACTION_PREF_ONGOING_NOTIFICATIONS_CHANGED)) {
                if (intent.hasExtra(GravityBoxSettings.EXTRA_ONGOING_NOTIF)) {
                    mOngoingNotif = intent.getStringExtra(GravityBoxSettings.EXTRA_ONGOING_NOTIF);
                    if (DEBUG) log("mOngoingNotif = " + mOngoingNotif);
                } else if (intent.hasExtra(GravityBoxSettings.EXTRA_ONGOING_NOTIF_RESET)) {
                    mOngoingNotif = "";
                    Settings.Secure.putString(mContext.getContentResolver(),
                            SETTING_ONGOING_NOTIFICATIONS, "");
                    if (DEBUG) log("Ongoing notifications list reset");
                }
            } else if (intent.getAction().equals(GravityBoxSettings.ACTION_PREF_DATA_TRAFFIC_CHANGED)
                    && mTrafficMeter != null) {
                if (intent.hasExtra(GravityBoxSettings.EXTRA_DT_ENABLE)) {
                    mTrafficMeter.setTrafficMeterEnabled(intent.getBooleanExtra(
                            GravityBoxSettings.EXTRA_DT_ENABLE, false));
                }
                if (intent.hasExtra(GravityBoxSettings.EXTRA_DT_POSITION)) {
                    mTrafficMeter.setTrafficMeterPosition(intent.getIntExtra(
                            GravityBoxSettings.EXTRA_DT_POSITION,
                            GravityBoxSettings.DT_POSITION_AUTO));
                    updateTrafficMeterPosition();
                }
                if (intent.hasExtra(GravityBoxSettings.EXTRA_DT_SIZE)) {
                    mTrafficMeter.setTextSize(1, intent.getIntExtra(GravityBoxSettings.EXTRA_DT_SIZE, 14));
                }
            }
        }
    };

    static class SettingsObserver extends ContentObserver {
        SettingsObserver(Handler handler) {
            super(handler);
        }

        void observe() {
            ContentResolver resolver = mContext.getContentResolver();
            resolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.SCREEN_BRIGHTNESS_MODE), false, this);
            update();
        }

        @Override
        public void onChange(boolean selfChange) {
            update();
        }

        public void update() {
            ContentResolver resolver = mContext.getContentResolver();
            int brightnessValue = Settings.System.getInt(resolver,
                    Settings.System.SCREEN_BRIGHTNESS_MODE, 0);
            mBrightnessControl = brightnessValue != Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC
                    && mBrightnessControlEnabled;
        }
    }

    public static void initResources(final XSharedPreferences prefs, final InitPackageResourcesParam resparam) {
        try {
            String layout = Utils.hasGeminiSupport() ? "gemini_super_status_bar" : "super_status_bar";
            resparam.res.hookLayout(PACKAGE_NAME, "layout", layout, new XC_LayoutInflated() {

                @Override
                public void handleLayoutInflated(LayoutInflatedParam liparam) throws Throwable {
                    prefs.reload();
                    mClockShowDow = Integer.valueOf(
                            prefs.getString(GravityBoxSettings.PREF_KEY_STATUSBAR_CLOCK_DOW, "0"));
                    mAmPmHide = prefs.getBoolean(GravityBoxSettings.PREF_KEY_STATUSBAR_CLOCK_AMPM_HIDE, false);
                    mClockHide = prefs.getBoolean(GravityBoxSettings.PREF_KEY_STATUSBAR_CLOCK_HIDE, false);
                    mClockLink = prefs.getString(GravityBoxSettings.PREF_KEY_STATUSBAR_CLOCK_LINK, null);
                    mAlarmHide = prefs.getBoolean(GravityBoxSettings.PREF_KEY_ALARM_ICON_HIDE, false);

                    String iconAreaId = Build.VERSION.SDK_INT > 16 ?
                            "system_icon_area" : "icons";
                    mIconArea = (ViewGroup) liparam.view.findViewById(
                            liparam.res.getIdentifier(iconAreaId, "id", PACKAGE_NAME));
                    if (mIconArea == null) return;

                    mRootView = Build.VERSION.SDK_INT > 16 ?
                            (ViewGroup) mIconArea.getParent().getParent() :
                            (ViewGroup) mIconArea.getParent();
                    if (mRootView == null) return;

                    mSbContents = Build.VERSION.SDK_INT > 16 ?
                            (ViewGroup) mIconArea.getParent() : mIconArea;

                    // find statusbar clock
                    mClock = (TextView) mIconArea.findViewById(
                            liparam.res.getIdentifier("clock", "id", PACKAGE_NAME));
                    if (mClock != null) {
                        ModStatusbarColor.setClock(mClock);
                        // use this additional field to identify the instance of Clock that resides in status bar
                        XposedHelpers.setAdditionalInstanceField(mClock, "sbClock", true);
                        mClockOriginalPaddingLeft = mClock.getPaddingLeft();
                        if (mClockHide) {
                            mClock.setVisibility(View.GONE);
                        }
                    }

                    // find notification panel clock
                    String panelHolderId = Build.VERSION.SDK_INT > 16 ?
                            "panel_holder" : "notification_panel";
                    final ViewGroup panelHolder = (ViewGroup) liparam.view.findViewById(
                            liparam.res.getIdentifier(panelHolderId, "id", PACKAGE_NAME));
                    if (panelHolder != null) {
                        mClockExpanded = (TextView) panelHolder.findViewById(
                                liparam.res.getIdentifier("clock", "id", PACKAGE_NAME));
                        if (mClockExpanded != null && Build.VERSION.SDK_INT < 17) {
                            mClockExpanded.setClickable(true);
                            mClockExpanded.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    launchClockApp();
                                }
                            });
                        }
                    }
                    
                    // inject new clock layout
                    mLayoutClock = new LinearLayout(liparam.view.getContext());
                    mLayoutClock.setLayoutParams(new LinearLayout.LayoutParams(
                                    LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
                    mLayoutClock.setGravity(Gravity.CENTER);
                    mRootView.addView(mLayoutClock);
                    if (DEBUG) log("mLayoutClock injected");

                    XposedHelpers.findAndHookMethod(mClock.getClass(), "getSmallTime", new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            // is this a status bar Clock instance?
                            // yes, if it contains our additional sbClock field
                            if (DEBUG) log("getSmallTime() called. mAmPmHide=" + mAmPmHide);
                            Object sbClock = XposedHelpers.getAdditionalInstanceField(param.thisObject, "sbClock");
                            if (DEBUG) log("Is statusbar clock: " + (sbClock == null ? "false" : "true"));
                            Calendar calendar = Calendar.getInstance(TimeZone.getDefault());
                            String clockText = param.getResult().toString();
                            if (DEBUG) log("Original clockText: '" + clockText + "'");
                            String amPm = calendar.getDisplayName(
                                    Calendar.AM_PM, Calendar.SHORT, Locale.getDefault());
                            if (DEBUG) log("Locale specific AM/PM string: '" + amPm + "'");
                            int amPmIndex = clockText.indexOf(amPm);
                            if (DEBUG) log("Original AM/PM index: " + amPmIndex);
                            if (mAmPmHide && amPmIndex != -1) {
                                clockText = clockText.replace(amPm, "").trim();
                                if (DEBUG) log("AM/PM removed. New clockText: '" + clockText + "'");
                                amPmIndex = -1;
                            } else if (!mAmPmHide 
                                        && !DateFormat.is24HourFormat(mClock.getContext()) 
                                        && amPmIndex == -1) {
                                // insert AM/PM if missing
                                clockText += " " + amPm;
                                amPmIndex = clockText.indexOf(amPm);
                                if (DEBUG) log("AM/PM added. New clockText: '" + clockText + "'; New AM/PM index: " + amPmIndex);
                            }
                            CharSequence dow = "";
                            // apply day of week only to statusbar clock, not the notification panel clock
                            if (mClockShowDow != GravityBoxSettings.DOW_DISABLED && sbClock != null) {
                                dow = getFormattedDow(calendar.getDisplayName(
                                        Calendar.DAY_OF_WEEK, Calendar.SHORT, Locale.getDefault())) + " ";
                            }
                            clockText = dow + clockText;
                            SpannableStringBuilder sb = new SpannableStringBuilder(clockText);
                            sb.setSpan(new RelativeSizeSpan(0.7f), 0, dow.length(), 
                                    Spannable.SPAN_EXCLUSIVE_INCLUSIVE);
                            if (amPmIndex > -1) {
                                int offset = Character.isWhitespace(clockText.charAt(dow.length() + amPmIndex - 1)) ?
                                        1 : 0;
                                sb.setSpan(new RelativeSizeSpan(0.7f), dow.length() + amPmIndex - offset, 
                                        dow.length() + amPmIndex + amPm.length(), 
                                        Spannable.SPAN_EXCLUSIVE_INCLUSIVE);
                            }
                            if (DEBUG) log("Final clockText: '" + sb + "'");
                            param.setResult(sb);
                        }
                    });

                    setClockPosition(prefs.getBoolean(
                            GravityBoxSettings.PREF_KEY_STATUSBAR_CENTER_CLOCK, false));

                    // insert Traffic meter
                    mTrafficMeter = new TrafficMeter(liparam.view.getContext());
                    LinearLayout.LayoutParams lParams = new LinearLayout.LayoutParams(
                            LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT);
                    mTrafficMeter.setLayoutParams(lParams);
                    mTrafficMeter.setGravity(Gravity.CENTER_VERTICAL);
                    mTrafficMeter.setTextAppearance(liparam.view.getContext(), 
                            liparam.view.getContext().getResources().getIdentifier(
                            "TextAppearance.StatusBar.Clock", "style", PACKAGE_NAME));
                    int position = GravityBoxSettings.DT_POSITION_AUTO;
                    try {
                        position = Integer.valueOf(prefs.getString(
                                GravityBoxSettings.PREF_KEY_DATA_TRAFFIC_POSITION, "0"));
                    } catch (NumberFormatException nfe) {
                        log("Invalid preference value for PREF_KEY_DATA_TRAFFIC_POSITION");
                    }
                    mTrafficMeter.setTrafficMeterPosition(position);
                    int size = 14;
                    try {
                        size = Integer.valueOf(prefs.getString(
                                GravityBoxSettings.PREF_KEY_DATA_TRAFFIC_SIZE, "14"));
                    } catch (NumberFormatException nfe) {
                        log("Invalid preference value for PREF_KEY_DATA_TRAFFIC_SIZE");
                    }
                    mTrafficMeter.setTextSize(1, size);
                    ModStatusbarColor.setTrafficMeter(mTrafficMeter);
                    updateTrafficMeterPosition();
                    mTrafficMeter.setTrafficMeterEnabled(prefs.getBoolean(
                            GravityBoxSettings.PREF_KEY_DATA_TRAFFIC_ENABLE, false));
                }
            });
        } catch (Throwable t) {
            XposedBridge.log(t);
        }
    }

    private static String getFormattedDow(String inDow) {
        switch (mClockShowDow) {
            case GravityBoxSettings.DOW_LOWERCASE: 
                return inDow.toLowerCase(Locale.getDefault());
            case GravityBoxSettings.DOW_UPPERCASE:
                return inDow.toUpperCase(Locale.getDefault());
            case GravityBoxSettings.DOW_STANDARD:
            default: return inDow;
        }
    }

    public static void init(final XSharedPreferences prefs, final ClassLoader classLoader) {
        try {
            final Class<?> phoneStatusBarClass =
                    XposedHelpers.findClass(CLASS_PHONE_STATUSBAR, classLoader);
            final Class<?> tickerClass =
                    XposedHelpers.findClass(CLASS_TICKER, classLoader);
            final Class<?> phoneStatusBarPolicyClass = 
                    XposedHelpers.findClass(CLASS_PHONE_STATUSBAR_POLICY, classLoader);
            final Class<?> powerManagerClass = XposedHelpers.findClass(CLASS_POWER_MANAGER, classLoader);
            final Class<?> locationControllerClass = XposedHelpers.findClass(CLASS_LOCATION_CONTROLLER, classLoader);

            final Class<?>[] loadAnimParamArgs = new Class<?>[2];
            loadAnimParamArgs[0] = int.class;
            loadAnimParamArgs[1] = Animation.AnimationListener.class;

            mBrightnessControlEnabled = prefs.getBoolean(
                    GravityBoxSettings.PREF_KEY_STATUSBAR_BRIGHTNESS, false);
            mOngoingNotif = prefs.getString(GravityBoxSettings.PREF_KEY_ONGOING_NOTIFICATIONS, "");

            XposedBridge.hookAllConstructors(phoneStatusBarPolicyClass, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    mPhoneStatusBarPolicy = param.thisObject;
                }
            });

            XposedHelpers.findAndHookMethod(phoneStatusBarPolicyClass, 
                    "updateAlarm", Intent.class, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    Object sbService = XposedHelpers.getObjectField(param.thisObject, "mService");
                    if (sbService != null) {
                        boolean alarmSet = ((Intent)param.args[0]).getBooleanExtra("alarmSet", false);
                        XposedHelpers.callMethod(sbService, "setIconVisibility", "alarm_clock",
                                (alarmSet && !mAlarmHide));
                    }
                }
            });

            XposedHelpers.findAndHookMethod(phoneStatusBarClass, "makeStatusBarView", new XC_MethodHook() {

                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    mPhoneStatusBar = param.thisObject;
                    mStatusBarView = XposedHelpers.getObjectField(mPhoneStatusBar, "mStatusBarView");
                    mContext = (Context) XposedHelpers.getObjectField(mPhoneStatusBar, "mContext");
                    Resources res = mContext.getResources();
                    mAnimPushUpOut = res.getIdentifier("push_up_out", "anim", "android");
                    mAnimPushDownIn = res.getIdentifier("push_down_in", "anim", "android");
                    mAnimFadeIn = res.getIdentifier("fade_in", "anim", "android");

                    mScreenWidth = (float) res.getDisplayMetrics().widthPixels;
                    mMinBrightness = res.getInteger(res.getIdentifier(
                            "config_screenBrightnessDim", "integer", "android"));
                    BRIGHTNESS_ON = XposedHelpers.getStaticIntField(powerManagerClass, "BRIGHTNESS_ON");

                    IntentFilter intentFilter = new IntentFilter();
                    intentFilter.addAction(GravityBoxSettings.ACTION_PREF_CLOCK_CHANGED);
                    intentFilter.addAction(GravityBoxSettings.ACTION_PREF_STATUSBAR_BRIGHTNESS_CHANGED);
                    intentFilter.addAction(GravityBoxSettings.ACTION_PREF_ONGOING_NOTIFICATIONS_CHANGED);
                    intentFilter.addAction(GravityBoxSettings.ACTION_PREF_DATA_TRAFFIC_CHANGED);
                    mContext.registerReceiver(mBroadcastReceiver, intentFilter);

                    mSettingsObserver = new SettingsObserver(
                            (Handler) XposedHelpers.getObjectField(mPhoneStatusBar, "mHandler"));
                    mSettingsObserver.observe();
                }
            });

            XposedHelpers.findAndHookMethod(phoneStatusBarClass, "showClock", boolean.class, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    if (mClock == null) return;

                    boolean visible = (Boolean) param.args[0] && !mClockHide;
                    mClock.setVisibility(visible ? View.VISIBLE : View.GONE);
                }
            });

            if (Build.VERSION.SDK_INT > 16) {
                XposedHelpers.findAndHookMethod(phoneStatusBarClass, "startActivityDismissingKeyguard", 
                        Intent.class, boolean.class, new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        if (mClockLink == null) return;

                        Intent i = (Intent) param.args[0];
                        if (i != null && Intent.ACTION_QUICK_CLOCK.equals(i.getAction())) {
                            final ComponentName cn = getComponentNameFromClockLink();
                            if (cn != null) {
                                i = new Intent();
                                i.setComponent(cn);
                                param.args[0] = i;
                            }
                        }
                    }
                });
            }

            XposedHelpers.findAndHookMethod(tickerClass, "tickerStarting", new XC_MethodHook() {

                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    if (mLayoutClock == null) return;

                    mLayoutClock.setVisibility(View.GONE);
                    Animation anim = (Animation) XposedHelpers.callMethod(
                            mPhoneStatusBar, "loadAnim", loadAnimParamArgs, mAnimPushUpOut, null);
                    mLayoutClock.startAnimation(anim);
                }
            });

            XposedHelpers.findAndHookMethod(tickerClass, "tickerDone", new XC_MethodHook() {

                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    if (mLayoutClock == null) return;

                    mLayoutClock.setVisibility(View.VISIBLE);
                    Animation anim = (Animation) XposedHelpers.callMethod(
                            mPhoneStatusBar, "loadAnim", loadAnimParamArgs, mAnimPushDownIn, null);
                    mLayoutClock.startAnimation(anim);
                }
            });

            XposedHelpers.findAndHookMethod(tickerClass, "tickerHalting", new XC_MethodHook() {

                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    if (mLayoutClock == null) return;

                    mLayoutClock.setVisibility(View.VISIBLE);
                    Animation anim = (Animation) XposedHelpers.callMethod(
                            mPhoneStatusBar, "loadAnim", loadAnimParamArgs, mAnimFadeIn, null);
                    mLayoutClock.startAnimation(anim);
                }
            });

            XposedHelpers.findAndHookMethod(phoneStatusBarClass, 
                    "interceptTouchEvent", MotionEvent.class, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    if (!mBrightnessControl) return;

                    brightnessControl((MotionEvent) param.args[0]);
                    if ((XposedHelpers.getIntField(param.thisObject, "mDisabled")
                            & STATUS_BAR_DISABLE_EXPAND) != 0) {
                        param.setResult(true);
                    }
                }
            });

            XposedHelpers.findAndHookMethod(locationControllerClass, 
                    "onReceive", Context.class, Intent.class, new XC_MethodReplacement() {
                @SuppressWarnings("unchecked")
                @Override
                protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                    prefs.reload();
                    if (!prefs.getBoolean(GravityBoxSettings.PREF_KEY_GPS_NOTIF_DISABLE, false)) {
                        XposedBridge.invokeOriginalMethod(param.method, param.thisObject, param.args);
                        return null;
                    }

                    if (DEBUG) log("LocationController: running onReceive replacement");

                    ArrayList<Object> changeCallbacks = null;
                    try {
                        changeCallbacks = (ArrayList<Object>) XposedHelpers.getObjectField(
                                param.thisObject, "mChangeCallbacks");
                    } catch(NoSuchFieldError nfe) {
                        if (DEBUG) log("LocationController: mChangeCallbacks field does not exist");
                    }
                    if (changeCallbacks == null) {
                        // there are no callback objects attached to be notified of GPS status so we simply exit
                        return null;
                    }

                    try {
                        final Intent intent = (Intent) param.args[1];
                        final String action = intent.getAction();
                        final boolean enabled = intent.getBooleanExtra("enabled", false);
                        final Resources res = mContext.getResources();
                        boolean visible;
                        int textResId;

                        if (action.equals("android.location.GPS_FIX_CHANGE") && enabled) {
                            textResId = res.getIdentifier("gps_notification_found_text", "string", PACKAGE_NAME);
                            visible = true;
                        } else if (action.equals("android.location.GPS_ENABLED_CHANGE") && !enabled) {
                            textResId = 0;
                            visible = false;
                        } else {
                            textResId = res.getIdentifier("gps_notification_searching_text", "string", PACKAGE_NAME);
                            visible = true;
                        }

                        final Class<?>[] paramArgs = new Class[] { boolean.class, String.class };
                        for (Object cb : changeCallbacks) {
                            XposedHelpers.callMethod(cb, "onLocationGpsStateChanged", paramArgs,
                                    visible, textResId == 0 ? null : res.getString(textResId));
                        }

                        return null;
                    } catch(Throwable t) {
                        // fall back to original method
                        XposedBridge.log(t);
                        XposedBridge.invokeOriginalMethod(param.method, param.thisObject, param.args);
                        return null;
                    }
                }
            });

            XposedHelpers.findAndHookMethod(phoneStatusBarClass, "addNotification", 
                    IBinder.class, CLASS_STATUSBAR_NOTIF, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    final Object notif = param.args[1];
                    final String pkg = (String) XposedHelpers.getObjectField(notif, "pkg");
                    final boolean ongoing = (Boolean) XposedHelpers.callMethod(notif, "isOngoing");
                    final int id = (Integer) XposedHelpers.getIntField(notif, "id");
                    final Notification n = (Notification) XposedHelpers.getObjectField(notif, "notification");
                    if (DEBUG) log ("addNotificationViews: pkg=" + pkg + "; id=" + id + 
                                    "; iconId=" + n.icon + "; ongoing=" + ongoing);

                    if (!ongoing) return;

                    // store if new
                    final String notifData = pkg + "," + n.icon;
                    final ContentResolver cr = mContext.getContentResolver();
                    String storedNotifs = Settings.Secure.getString(cr,
                            SETTING_ONGOING_NOTIFICATIONS);
                    if (storedNotifs == null || !storedNotifs.contains(notifData)) {
                        if (storedNotifs == null || storedNotifs.isEmpty()) {
                            storedNotifs = notifData;
                        } else {
                            storedNotifs += "#C3C0#" + notifData;
                        }
                        if (DEBUG) log("New storedNotifs = " + storedNotifs);
                        Settings.Secure.putString(cr, SETTING_ONGOING_NOTIFICATIONS, storedNotifs);
                    }

                    // block if requested
                    if (mOngoingNotif.contains(notifData)) {
                        param.setResult(null);
                        if (DEBUG) log("Ongoing notification " + notifData + " blocked.");
                    }
                }
            });
        }
        catch (Throwable t) {
            XposedBridge.log(t);
        }
    }

    private static void setClockPosition(boolean center) {
        if (mClockCentered == center || mClock == null || 
                mIconArea == null || mLayoutClock == null) {
            return;
        }

        if (center) {
            mClock.setGravity(Gravity.CENTER);
            mClock.setLayoutParams(new LinearLayout.LayoutParams(
                    LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
            mClock.setPadding(0, 0, 0, 0);
            mIconArea.removeView(mClock);
            mLayoutClock.addView(mClock);
            if (DEBUG) log("Clock set to center position");
        } else {
            mClock.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
            mClock.setLayoutParams(new LinearLayout.LayoutParams(
                    LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT));
            mClock.setPadding(mClockOriginalPaddingLeft, 0, 0, 0);
            mLayoutClock.removeView(mClock);
            mIconArea.addView(mClock);
            if (DEBUG) log("Clock set to normal position");
        }

        mClockCentered = center;
    }

    private static void updateTrafficMeterPosition() {
        if (mTrafficMeter == null || mSbContents == null ||
            mLayoutClock == null || mIconArea == null) return;

        mSbContents.removeView(mTrafficMeter);
        mLayoutClock.removeView(mTrafficMeter);
        mIconArea.removeView(mTrafficMeter);

        switch(mTrafficMeter.getTrafficMeterPosition()) {
            case GravityBoxSettings.DT_POSITION_AUTO:
                if (mClockCentered) {
                    mIconArea.addView(mTrafficMeter, 
                            Build.VERSION.SDK_INT > 16 ? 0 : 1);
                } else {
                    mLayoutClock.addView(mTrafficMeter);
                }
                break;
            case GravityBoxSettings.DT_POSITION_LEFT:
                mSbContents.addView(mTrafficMeter, 0);
                break;
        }
    }

    private static ComponentName getComponentNameFromClockLink() {
        if (mClockLink == null) return null;

        try {
            String[] splitValue = mClockLink.split(AppPickerPreference.SEPARATOR);
            ComponentName cn = new ComponentName(splitValue[0], splitValue[1]);
            return cn;
        } catch (Exception e) {
            log("Error getting ComponentName from clock link: " + e.getMessage());
            return null;
        }
    }

    private static void launchClockApp() {
        if (mContext == null || mClockLink == null) return;

        try {
            Intent i = new Intent();
            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            i.setComponent(getComponentNameFromClockLink());
            mContext.startActivity(i);
            if (mPhoneStatusBar != null) {
                XposedHelpers.callMethod(mPhoneStatusBar, Build.VERSION.SDK_INT > 16 ?
                        "animateCollapsePanels" : "animateCollapse");
            }
        } catch (ActivityNotFoundException e) {
            log("Error launching assigned app for clock: " + e.getMessage());
        } catch (Throwable t) {
            XposedBridge.log(t);
        }
    }

    private static Runnable mLongPressBrightnessChange = new Runnable() {
        @Override
        public void run() {
            try {
                XposedHelpers.callMethod(mStatusBarView, "performHapticFeedback", 
                        HapticFeedbackConstants.LONG_PRESS);
                adjustBrightness(mInitialTouchX);
                mLinger = BRIGHTNESS_CONTROL_LINGER_THRESHOLD + 1;
            } catch (Throwable t) {
                XposedBridge.log(t);
            }
        }
    };

    private static void adjustBrightness(int x) {
        try {
            float raw = ((float) x) / mScreenWidth;
    
            // Add a padding to the brightness control on both sides to
            // make it easier to reach min/max brightness
            float padded = Math.min(1.0f - BRIGHTNESS_CONTROL_PADDING,
                    Math.max(BRIGHTNESS_CONTROL_PADDING, raw));
            float value = (padded - BRIGHTNESS_CONTROL_PADDING) /
                    (1 - (2.0f * BRIGHTNESS_CONTROL_PADDING));
    
            int newBrightness = mMinBrightness + (int) Math.round(value *
                    (BRIGHTNESS_ON - mMinBrightness));
            newBrightness = Math.min(newBrightness, BRIGHTNESS_ON);
            newBrightness = Math.max(newBrightness, mMinBrightness);

            Class<?> classSm = XposedHelpers.findClass("android.os.ServiceManager", null);
            Class<?> classIpm = XposedHelpers.findClass("android.os.IPowerManager.Stub", null);
            IBinder b = (IBinder) XposedHelpers.callStaticMethod(
                    classSm, "getService", Context.POWER_SERVICE);
            Object power = XposedHelpers.callStaticMethod(classIpm, "asInterface", b);
            if (power != null) {
                final String bcMethod = Build.VERSION.SDK_INT > 16 ?
                        "setTemporaryScreenBrightnessSettingOverride" : "setBacklightBrightness";
                XposedHelpers.callMethod(power, bcMethod, newBrightness);
                Settings.System.putInt(mContext.getContentResolver(),
                        Settings.System.SCREEN_BRIGHTNESS, newBrightness);
            }
        } catch (Throwable t) {
            XposedBridge.log(t);
        }
    }

    private static void brightnessControl(MotionEvent event) {
        try {
            final int action = event.getAction();
            final int x = (int) event.getRawX();
            final int y = (int) event.getRawY();
            Handler handler = (Handler) XposedHelpers.getObjectField(mPhoneStatusBar, "mHandler");
            int notificationHeaderHeight = Build.VERSION.SDK_INT > 16 ?
                    XposedHelpers.getIntField(mPhoneStatusBar, "mNotificationHeaderHeight") :
                        XposedHelpers.getIntField(mPhoneStatusBar, "mNotificationPanelMinHeight");
    
            if (action == MotionEvent.ACTION_DOWN) {
                mLinger = 0;
                mInitialTouchX = x;
                mInitialTouchY = y;
                mVelocityTracker = VelocityTracker.obtain();
                handler.removeCallbacks(mLongPressBrightnessChange);
                if ((y) < notificationHeaderHeight) {
                    handler.postDelayed(mLongPressBrightnessChange,
                            BRIGHTNESS_CONTROL_LONG_PRESS_TIMEOUT);
                }
            } else if (action == MotionEvent.ACTION_MOVE) {
                if ((y) < notificationHeaderHeight) {
                    mVelocityTracker.computeCurrentVelocity(1000);
                    float yVel = mVelocityTracker.getYVelocity();
                    yVel = Math.abs(yVel);
                    if (yVel < 50.0f) {
                        if (mLinger > BRIGHTNESS_CONTROL_LINGER_THRESHOLD) {
                            adjustBrightness(x);
                        } else {
                            mLinger++;
                        }
                    }
                    int touchSlop = ViewConfiguration.get(mContext).getScaledTouchSlop();
                    if (Math.abs(x - mInitialTouchX) > touchSlop ||
                            Math.abs(y - mInitialTouchY) > touchSlop) {
                        handler.removeCallbacks(mLongPressBrightnessChange);
                    }
                } else {
                    handler.removeCallbacks(mLongPressBrightnessChange);
                }
            } else if (action == MotionEvent.ACTION_UP
                    || action == MotionEvent.ACTION_CANCEL) {
                mVelocityTracker.recycle();
                mVelocityTracker = null;
                handler.removeCallbacks(mLongPressBrightnessChange);
                mLinger = 0;
            }
        } catch (Throwable t) {
            XposedBridge.log(t);
        }
    }
}
