package com.ceco.gm2.gravitybox;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Hashtable;

import com.ceco.gm2.gravitybox.preference.AppPickerPreference;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.content.res.XResources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.util.TypedValue;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.View;
import android.view.ViewManager;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

public class ModLockscreen {
    private static final String TAG = "GB:ModLockscreen";
    private static final String CLASS_KGVIEW_MANAGER = "com.android.internal.policy.impl.keyguard.KeyguardViewManager";
    private static final String CLASS_KG_HOSTVIEW = "com.android.internal.policy.impl.keyguard.KeyguardHostView";
    private static final String CLASS_KG_SELECTOR_VIEW = "com.android.internal.policy.impl.keyguard.KeyguardSelectorView";
    private static final String CLASS_TARGET_DRAWABLE = Utils.isMtkDevice() ?
            "com.android.internal.policy.impl.keyguard.TargetDrawable" :
            "com.android.internal.widget.multiwaveview.TargetDrawable";
    private static final String CLASS_TRIGGER_LISTENER = "com.android.internal.policy.impl.keyguard.KeyguardSelectorView$1";
    private static final boolean DEBUG = false;

    private static XSharedPreferences mPrefs;
    private static Hashtable<String, AppInfo> mAppInfoCache = new Hashtable<String, AppInfo>();
    private static Class<?>[] mLaunchActivityArgs = new Class<?>[] 
            { Intent.class, boolean.class, boolean.class, Handler.class, Runnable.class };
    private static Constructor<?> mTargetDrawableConstructor;

    private static void log(String message) {
        XposedBridge.log(TAG + ": " + message);
    }

    public static void initZygote(final XSharedPreferences prefs) {
        try {
            mPrefs = prefs;
            final Class<?> kgViewManagerClass = XposedHelpers.findClass(CLASS_KGVIEW_MANAGER, null);
            final Class<?> kgHostViewClass = XposedHelpers.findClass(CLASS_KG_HOSTVIEW, null);
            final Class<?> kgSelectorViewClass = XposedHelpers.findClass(CLASS_KG_SELECTOR_VIEW, null);
            final Class<?> triggerListenerClass = XposedHelpers.findClass(CLASS_TRIGGER_LISTENER, null);

            boolean enableMenuKey = prefs.getBoolean(
                    GravityBoxSettings.PREF_KEY_LOCKSCREEN_MENU_KEY, false);
            XResources.setSystemWideReplacement("android", "bool", "config_disableMenuKeyInLockScreen", !enableMenuKey);

            XposedHelpers.findAndHookMethod(kgViewManagerClass, "maybeCreateKeyguardLocked", 
                    boolean.class, boolean.class, Bundle.class, new XC_MethodHook() {

                @Override
                protected void afterHookedMethod(final MethodHookParam param) throws Throwable {
                    mPrefs.reload();
                    ViewManager viewManager = (ViewManager) XposedHelpers.getObjectField(
                            param.thisObject, "mViewManager");
                    FrameLayout keyGuardHost = (FrameLayout) XposedHelpers.getObjectField(
                            param.thisObject, "mKeyguardHost");
                    WindowManager.LayoutParams windowLayoutParams = (WindowManager.LayoutParams) 
                            XposedHelpers.getObjectField(param.thisObject, "mWindowLayoutParams");

                    final String bgType = prefs.getString(
                            GravityBoxSettings.PREF_KEY_LOCKSCREEN_BACKGROUND,
                            GravityBoxSettings.LOCKSCREEN_BG_DEFAULT);

                    if (!bgType.equals(GravityBoxSettings.LOCKSCREEN_BG_DEFAULT)) {
                        windowLayoutParams.flags &= ~WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER;
                    } else {
                        windowLayoutParams.flags |= WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER;
                    }
                    viewManager.updateViewLayout(keyGuardHost, windowLayoutParams);
                    if (DEBUG) log("maybeCreateKeyguardLocked: layout updated");
                }
            });

            XposedHelpers.findAndHookMethod(kgViewManagerClass, "inflateKeyguardView",
                    Bundle.class, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(final MethodHookParam param) throws Throwable {
                    mPrefs.reload();

                    FrameLayout keyguardView = (FrameLayout) XposedHelpers.getObjectField(
                            param.thisObject, "mKeyguardView");

                    final String bgType = mPrefs.getString(
                            GravityBoxSettings.PREF_KEY_LOCKSCREEN_BACKGROUND, 
                            GravityBoxSettings.LOCKSCREEN_BG_DEFAULT);

                    if (bgType.equals(GravityBoxSettings.LOCKSCREEN_BG_COLOR)) {
                        int color = mPrefs.getInt(
                                GravityBoxSettings.PREF_KEY_LOCKSCREEN_BACKGROUND_COLOR, Color.BLACK);
                        keyguardView.setBackgroundColor(color);
                        if (DEBUG) log("inflateKeyguardView: background color set");
                    } else if (bgType.equals(GravityBoxSettings.LOCKSCREEN_BG_IMAGE)) {
                        try {
                            Context context = (Context) XposedHelpers.getObjectField(param.thisObject, "mContext");
                            FrameLayout flayout = new FrameLayout(context);
                            flayout.setLayoutParams(new LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT, 
                                    ViewGroup.LayoutParams.MATCH_PARENT));
                            Context gbContext = context.createPackageContext(
                                    GravityBox.PACKAGE_NAME, 0);
                            String wallpaperFile = gbContext.getFilesDir() + "/lockwallpaper";
                            Bitmap background = BitmapFactory.decodeFile(wallpaperFile);
                            Drawable d = new BitmapDrawable(context.getResources(), background);
                            ImageView mLockScreenWallpaperImage = new ImageView(context);
                            mLockScreenWallpaperImage.setScaleType(ScaleType.CENTER_CROP);
                            mLockScreenWallpaperImage.setImageDrawable(d);
                            flayout.addView(mLockScreenWallpaperImage, -1, -1);
                            keyguardView.addView(flayout,0);
                            if (DEBUG) log("inflateKeyguardView: background image set");
                        } catch (NameNotFoundException e) {
                            XposedBridge.log(e);
                        }
                    }
                }
            });

            XposedHelpers.findAndHookMethod(kgViewManagerClass, 
                    "shouldEnableScreenRotation", new XC_MethodReplacement() {

                        @Override
                        protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                            prefs.reload();
                            return prefs.getBoolean(GravityBoxSettings.PREF_KEY_LOCKSCREEN_ROTATION, false);
                        }
            });

            XposedHelpers.findAndHookMethod(kgHostViewClass, "onFinishInflate", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(final MethodHookParam param) throws Throwable {
                    Object slidingChallenge = XposedHelpers.getObjectField(
                            param.thisObject, "mSlidingChallengeLayout");
                    minimizeChallengeIfDesired(slidingChallenge);
                }
            });

            XposedHelpers.findAndHookMethod(kgHostViewClass, "onScreenTurnedOn", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(final MethodHookParam param) throws Throwable {
                    Object slidingChallenge = XposedHelpers.getObjectField(
                            param.thisObject, "mSlidingChallengeLayout");
                    minimizeChallengeIfDesired(slidingChallenge);
                }
            });

            XposedHelpers.findAndHookMethod(kgSelectorViewClass, "onFinishInflate", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(final MethodHookParam param) throws Throwable {
                    if (DEBUG) log("KeyGuardSelectorView onFinishInflate()");
                    prefs.reload();
                    if (!prefs.getBoolean(
                            GravityBoxSettings.PREF_KEY_LOCKSCREEN_TARGETS_ENABLE, false)) return;

                    final Context context = (Context) XposedHelpers.getObjectField(param.thisObject, "mContext");
                    final Resources res = context.getResources();
                    final View gpView = (View) XposedHelpers.getObjectField(param.thisObject, "mGlowPadView");

                    // apply custom bottom margin to shift unlock ring upwards
                    try {
                        final FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) gpView.getLayoutParams();
                        final int bottomMarginOffsetPx = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 
                                prefs.getInt(GravityBoxSettings.PREF_KEY_LOCKSCREEN_TARGETS_BOTTOM_OFFSET, 0),
                                res.getDisplayMetrics());
                        lp.setMargins(lp.leftMargin, lp.topMargin, lp.rightMargin, 
                                lp.bottomMargin + bottomMarginOffsetPx);
                        gpView.setLayoutParams(lp);
                    } catch (Throwable t) {
                        log("Lockscreen targets: error while trying to modify GlowPadView layout" + t.getMessage());
                    }

                    @SuppressWarnings("unchecked")
                    final ArrayList<Object> targets = (ArrayList<Object>) XposedHelpers.getObjectField(
                            gpView, "mTargetDrawables");
                    final ArrayList<Object> newTargets = new ArrayList<Object>();
                    final ArrayList<AppInfo> appInfoList = new ArrayList<AppInfo>(5);

                    // fill appInfoList helper with apps from preferences
                    for (int i=0; i<=4; i++) {
                        String app = prefs.getString(
                                GravityBoxSettings.PREF_KEY_LOCKSCREEN_TARGETS_APP[i], null);
                        if (app != null) {
                            appInfoList.add(getAppInfo(context, app));
                            if (DEBUG) log("appInfoList.add: " + app);
                        }
                    }

                    // get target from position 0 supposing it's unlock ring
                    newTargets.add(targets.get(0));

                    // create and fill custom targets with proper layout based on number of targets
                    switch(appInfoList.size()) {
                        case 1:
                            newTargets.add(createTargetDrawable(res, null));
                            newTargets.add(createTargetDrawable(res, appInfoList.get(0)));
                            newTargets.add(createTargetDrawable(res, null));
                            break;
                        case 2:
                            newTargets.add(createTargetDrawable(res, appInfoList.get(0)));
                            newTargets.add(createTargetDrawable(res, appInfoList.get(1)));
                            newTargets.add(createTargetDrawable(res, null));
                            break;
                        case 3:
                        case 4:
                        case 5:
                            for (int i=0; i<=4; i++) {
                                newTargets.add(i >= appInfoList.size() ?
                                        createTargetDrawable(res, null) :
                                            createTargetDrawable(res, appInfoList.get(i)));
                            }
                            break;
                    }
                    XposedHelpers.setObjectField(gpView, "mTargetDrawables", newTargets);
                    XposedHelpers.callMethod(param.thisObject, "updateTargets");
                }
            });

            XposedHelpers.findAndHookMethod(triggerListenerClass, "onTrigger", 
                    View.class, int.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(final MethodHookParam param) throws Throwable {
                    if (DEBUG) log("GlowPadView.OnTriggerListener; index=" + ((Integer) param.args[1]));
                    prefs.reload();
                    if (!prefs.getBoolean(
                            GravityBoxSettings.PREF_KEY_LOCKSCREEN_TARGETS_ENABLE, false)) return;

                    final int index = (Integer) param.args[1];
                    final View gpView = (View) XposedHelpers.getObjectField(
                            XposedHelpers.getSurroundingThis(param.thisObject), "mGlowPadView");
                    @SuppressWarnings("unchecked")
                    final ArrayList<Object> targets = (ArrayList<Object>) XposedHelpers.getObjectField(
                            gpView, "mTargetDrawables");
                    final Object td = targets.get(index);

                    AppInfo appInfo = (AppInfo) XposedHelpers.getAdditionalInstanceField(td, "mGbAppInfo");
                    if (appInfo != null) {
                        final Object activityLauncher = XposedHelpers.getObjectField(
                                XposedHelpers.getSurroundingThis(param.thisObject), "mActivityLauncher");
                        XposedHelpers.callMethod(activityLauncher, "launchActivity", mLaunchActivityArgs,
                                appInfo.intent, false, true, null, null);
                    }
                }
            });
        } catch (Throwable t) {
            XposedBridge.log(t);
        }
    }

    private static void minimizeChallengeIfDesired(Object challenge) {
        if (challenge == null) return;

        mPrefs.reload();
        if (mPrefs.getBoolean(GravityBoxSettings.PREF_KEY_LOCKSCREEN_MAXIMIZE_WIDGETS, false)) {
            if (DEBUG) log("minimizeChallengeIfDesired: challenge minimized");
            XposedHelpers.callMethod(challenge, "showChallenge", false);
        }
    }

    private static class AppInfo {
        public String key;
        public Intent intent;
        public Drawable icon;
    }

    private static AppInfo getAppInfo(Context context, String app) {
        try {
            if (mAppInfoCache.containsKey(app)) {
                if (DEBUG) log("AppInfo: returning from cache for " + app);
                return mAppInfoCache.get(app);
            }

            AppInfo appInfo = new AppInfo();
            appInfo.key = app;

            String[] splitValue = app.split(AppPickerPreference.SEPARATOR);
            ComponentName cn = new ComponentName(splitValue[0], splitValue[1]);
            Intent i = new Intent();
            i.setComponent(cn);
            appInfo.intent = i;

            PackageManager pm = context.getPackageManager();
            Resources res = context.getResources();
            ActivityInfo ai = pm.getActivityInfo(cn, 0);
            Bitmap appIcon = ((BitmapDrawable)ai.loadIcon(pm)).getBitmap();
            int sizePx = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 40, 
                    res.getDisplayMetrics());
            appIcon = Bitmap.createScaledBitmap(appIcon, sizePx, sizePx, true);
            appInfo.icon = new BitmapDrawable(res, appIcon);

            mAppInfoCache.put(appInfo.key, appInfo);
            if (DEBUG) log("AppInfo: storing to cache for " + app);
            return appInfo;
        } catch (Throwable t) {
            XposedBridge.log(t);
            return null;
        }
    }

    private static Object createTargetDrawable(Resources res, AppInfo appInfo) throws Throwable {
        if (mTargetDrawableConstructor == null) {
            mTargetDrawableConstructor = XposedHelpers.findConstructorExact(
                    XposedHelpers.findClass(CLASS_TARGET_DRAWABLE, null), 
                    Resources.class, int.class);
        }

        final Object td = mTargetDrawableConstructor.newInstance(res, 0);
        if (appInfo != null) {
            Drawable d = appInfo.icon == null ? null : appInfo.icon.mutate();
            XposedHelpers.setObjectField(td, "mDrawable", d);
            XposedHelpers.callMethod(td, "resizeDrawables");
            XposedHelpers.setAdditionalInstanceField(td, "mGbAppInfo", appInfo);
        }

        return td;
    }
}