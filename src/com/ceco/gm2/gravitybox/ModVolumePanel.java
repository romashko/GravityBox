package com.ceco.gm2.gravitybox;

import java.lang.reflect.Field;
import java.util.Map;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.TextView;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodHook.Unhook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

public class ModVolumePanel {
    private static final String TAG = "GB:ModVolumePanel";
    public static final String PACKAGE_NAME = "android";
    private static final String CLASS_VOLUME_PANEL = "android.view.VolumePanel";
    private static final String CLASS_STREAM_CONTROL = "android.view.VolumePanel$StreamControl";
    private static final String CLASS_AUDIO_SERVICE = "android.media.AudioService";
    private static final String CLASS_VIEW_GROUP = "android.view.ViewGroup";
    private static final boolean DEBUG = false;

    private static int STREAM_RING = 2;
    private static int STREAM_NOTIFICATION = 5;

    private static Object mVolumePanel;
    private static Object mAudioService;
    private static boolean mVolumesLinked;
    private static Unhook mViewGroupAddViewHook;

    private static void log(String message) {
        XposedBridge.log(TAG + ": " + message);
    }

    private static BroadcastReceiver mBrodcastReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(GravityBoxSettings.ACTION_PREF_VOLUME_PANEL_MODE_CHANGED)) {
                boolean expandable = intent.getBooleanExtra(GravityBoxSettings.EXTRA_EXPANDABLE, false);
                updateVolumePanelMode(expandable);
            } else if (intent.getAction().equals(GravityBoxSettings.ACTION_PREF_LINK_VOLUMES_CHANGED)) {
                mVolumesLinked = intent.getBooleanExtra(GravityBoxSettings.EXTRA_LINKED, true);
                if (DEBUG) log("mVolumesLinked set to: " + mVolumesLinked);
                updateStreamVolumeAlias();
            }
        }
        
    };

    public static void init(final XSharedPreferences prefs, final ClassLoader classLoader) {
        try {
            final Class<?> classVolumePanel = XposedHelpers.findClass(CLASS_VOLUME_PANEL, classLoader);
            final Class<?> classStreamControl = XposedHelpers.findClass(CLASS_STREAM_CONTROL, classLoader);
            final Class<?> classAudioService = XposedHelpers.findClass(CLASS_AUDIO_SERVICE, classLoader);
            final Class<?> classViewGroup = XposedHelpers.findClass(CLASS_VIEW_GROUP, classLoader);

            XposedBridge.hookAllConstructors(classVolumePanel, new XC_MethodHook() {

                @Override
                protected void afterHookedMethod(final MethodHookParam param) throws Throwable {
                    mVolumePanel = param.thisObject;
                    Context context = (Context) XposedHelpers.getObjectField(mVolumePanel, "mContext");
                    if (DEBUG) log("VolumePanel constructed; mVolumePanel set");

                    boolean expandable = prefs.getBoolean(
                            GravityBoxSettings.PREF_KEY_VOLUME_PANEL_EXPANDABLE, true);
                    updateVolumePanelMode(expandable);

                    mVolumesLinked = prefs.getBoolean(GravityBoxSettings.PREF_KEY_LINK_VOLUMES, true);

                    Object[] streams = (Object[]) XposedHelpers.getStaticObjectField(classVolumePanel, "STREAMS");
                    XposedHelpers.setBooleanField(streams[1], "show", true);

                    IntentFilter intentFilter = new IntentFilter();
                    intentFilter.addAction(GravityBoxSettings.ACTION_PREF_VOLUME_PANEL_MODE_CHANGED);
                    intentFilter.addAction(GravityBoxSettings.ACTION_PREF_LINK_VOLUMES_CHANGED);
                    context.registerReceiver(mBrodcastReceiver, intentFilter);
                }
            });

            XposedHelpers.findAndHookMethod(classVolumePanel, "createSliders", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(final MethodHookParam param) throws Throwable {
                    final boolean voiceCapableOrig = XposedHelpers.getBooleanField(param.thisObject, "mVoiceCapable");
                    if (DEBUG) log("createSliders: original mVoiceCapable = " + voiceCapableOrig);
                    XposedHelpers.setAdditionalInstanceField(param.thisObject, "mGbVoiceCapableOrig", voiceCapableOrig);
                    XposedHelpers.setBooleanField(param.thisObject, "mVoiceCapable", false);
                }
                @Override
                protected void afterHookedMethod(final MethodHookParam param) throws Throwable {
                    final Boolean voiceCapableOrig =  (Boolean)XposedHelpers.getAdditionalInstanceField(
                            param.thisObject, "mGbVoiceCapableOrig");
                    if (voiceCapableOrig != null) {
                        if (DEBUG) log("createSliders: restoring original mVoiceCapable");
                        XposedHelpers.setBooleanField(param.thisObject, "mVoiceCapable", voiceCapableOrig);
                    }
                }
            });

            XposedHelpers.findAndHookMethod(classVolumePanel, "expand", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(final MethodHookParam param) throws Throwable {
                    hideNotificationSliderIfLinked();
                }
            });

            try {
                final Field fldVolTitle = XposedHelpers.findField(classStreamControl, "volTitle");
                if (DEBUG) log("Hooking StreamControl constructor for volTitle field initialization");
                XposedBridge.hookAllConstructors(classStreamControl, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(final MethodHookParam param) throws Throwable {
                        Context context = (Context) XposedHelpers.getObjectField(
                                XposedHelpers.getSurroundingThis(param.thisObject), "mContext");
                        if (context != null) {
                            TextView tv = new TextView(context);
                            fldVolTitle.set(param.thisObject, tv);
                            if (DEBUG) log("StreamControl: volTitle field initialized");
                        }
                    }
                });
            } catch(Throwable t) {
                if (DEBUG) log("StreamControl: exception while initializing volTitle field: " + t.getMessage());
            }

            // Samsung bug workaround
            XposedHelpers.findAndHookMethod(classVolumePanel, "addOtherVolumes", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(final MethodHookParam param) throws Throwable {
                    if (DEBUG) log("addOtherVolumes: hooking ViewGroup.addViewInner");

                    mViewGroupAddViewHook = XposedHelpers.findAndHookMethod(classViewGroup, "addViewInner", 
                            View.class, int.class, ViewGroup.LayoutParams.class, 
                            boolean.class, new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(final MethodHookParam param2) throws Throwable {
                            if (DEBUG) log("ViewGroup.addViewInner called from VolumePanel.addOtherVolumes()");
                            View child = (View) param2.args[0];
                            if (child.getParent() != null) {
                                if (DEBUG) log("Ignoring addView for child: " + child.toString());
                                param2.setResult(null);
                                return;
                            }
                        }
                    });
                }
                @Override
                protected void afterHookedMethod(final MethodHookParam param) throws Throwable {
                    if (mViewGroupAddViewHook != null) {
                        if (DEBUG) log("addOtherVolumes: unhooking ViewGroup.addViewInner");
                        mViewGroupAddViewHook.unhook();
                        mViewGroupAddViewHook = null;
                    }
                }
            });

            XposedBridge.hookAllConstructors(classAudioService, new XC_MethodHook() {

                @Override
                protected void afterHookedMethod(final MethodHookParam param) throws Throwable {
                    mAudioService = param.thisObject;
                    mVolumesLinked = prefs.getBoolean(GravityBoxSettings.PREF_KEY_LINK_VOLUMES, true);
                    if (DEBUG) log("AudioService constructed: mAudioService set");
                    updateStreamVolumeAlias();
                }
            });
        } catch (Throwable t) {
            XposedBridge.log(t);
        }
    }

    private static void updateVolumePanelMode(boolean expandable) {
        if (mVolumePanel == null) return;

        View mMoreButton = (View) XposedHelpers.getObjectField(mVolumePanel, "mMoreButton");
        View mDivider = (View) XposedHelpers.getObjectField(mVolumePanel, "mDivider");

        mMoreButton.setVisibility(expandable ? View.VISIBLE : View.GONE);
        if (!mMoreButton.hasOnClickListeners()) {
            mMoreButton.setOnClickListener((OnClickListener) mVolumePanel);
        }
        mDivider.setVisibility(expandable ? View.VISIBLE : View.GONE);

        XposedHelpers.setBooleanField(mVolumePanel, "mShowCombinedVolumes", expandable);
        XposedHelpers.setObjectField(mVolumePanel, "mStreamControls", null);
        if (DEBUG) log("VolumePanel mode changed to: " + ((expandable) ? "EXPANDABLE" : "SIMPLE"));
    }

    private static void hideNotificationSliderIfLinked() {
        if (mVolumePanel == null || !mVolumesLinked) return;

        @SuppressWarnings("unchecked")
        Map<Integer, Object> streamControls = 
                (Map<Integer, Object>) XposedHelpers.getObjectField(mVolumePanel, "mStreamControls");
        if (streamControls == null) return;

        for (Object o : streamControls.values()) {
            if ((Integer) XposedHelpers.getIntField(o, "streamType") == STREAM_NOTIFICATION) {
                View v = (View) XposedHelpers.getObjectField(o, "group");
                if (v != null) {
                    v.setVisibility(View.GONE);
                    if (DEBUG) log("Notification volume slider hidden");
                    break;
                }
            }
        }
    }

    private static void updateStreamVolumeAlias() {
        if (mAudioService == null) return;

        int[] streamVolumeAlias = (int[]) XposedHelpers.getObjectField(mAudioService, "mStreamVolumeAlias");
        streamVolumeAlias[STREAM_NOTIFICATION] = mVolumesLinked ? STREAM_RING : STREAM_NOTIFICATION;
        XposedHelpers.setObjectField(mAudioService, "mStreamVolumeAlias", streamVolumeAlias);
        if (DEBUG) log("AudioService mStreamVolumeAlias updated, STREAM_NOTIFICATION set to: " + 
                streamVolumeAlias[STREAM_NOTIFICATION]);
    }
}