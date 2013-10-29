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

import de.robv.android.xposed.XposedBridge;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.content.res.XResources;
import android.net.ConnectivityManager;
import android.os.Build;
import android.os.Vibrator;
import android.telephony.TelephonyManager;
import android.util.DisplayMetrics;
import android.view.WindowManager;
import java.util.*;
import static de.robv.android.xposed.XposedHelpers.callStaticMethod;
import static de.robv.android.xposed.XposedHelpers.findClass;

public class Utils {
    private static final String TAG = "GB:Utils";
    private static final boolean DEBUG = false;

    // Device types
    private static final int DEVICE_PHONE = 0;
    private static final int DEVICE_HYBRID = 1;
    private static final int DEVICE_TABLET = 2;

    // Device type reference
    private static int mDeviceType = -1;
    private static Boolean mIsMtkDevice = null;
    private static Boolean mIsXperiaDevice = null;
    private static Boolean mIsWifiOnly = null;
    private static String mDeviceCharacteristics = null;
    
    // Device features
    private static Boolean mHasGeminiSupport = null;
    private static Boolean mHasTelephonySupport = null;
    private static Boolean mHasVibrator = null;
    private static Boolean mHasFlash = null;
    private static Boolean mHasGPS = null;

    // Supported MTK devices
    private static final Set<String> MTK_DEVICES = new HashSet<String>(Arrays.asList(
        new String[] {"mt6572","mt6575","mt6577","mt8377","mt6589","mt8389"}
    ));

    private static void log(String message) {
        XposedBridge.log(TAG + ": " + message);
    }

    private static int getScreenType(Context con) {
        if (mDeviceType == -1) {
            WindowManager wm = (WindowManager)con.getSystemService(Context.WINDOW_SERVICE);
            DisplayMetrics outMetrics = new DisplayMetrics();
            wm.getDefaultDisplay().getMetrics(outMetrics);
            int shortSize = Math.min(outMetrics.heightPixels, outMetrics.widthPixels);
            int shortSizeDp = shortSize * DisplayMetrics.DENSITY_DEFAULT / outMetrics.densityDpi;
            if (shortSizeDp < 600) {
                // 0-599dp: "phone" UI with a separate status & navigation bar
                mDeviceType =  DEVICE_PHONE;
            } else if (shortSizeDp < 720) {
                // 600-719dp: "phone" UI with modifications for larger screens
                mDeviceType = DEVICE_HYBRID;
            } else {
                // 720dp: "tablet" UI with a single combined status & navigation bar
                mDeviceType = DEVICE_TABLET;
            }
        }
        return mDeviceType;
    }

    public static boolean isPhoneUI(Context con) {
        return getScreenType(con) == DEVICE_PHONE;
    }

    public static boolean isHybridUI(Context con) {
        return getScreenType(con) == DEVICE_HYBRID;
    }

    public static boolean isTabletUI(Context con) {
        return getScreenType(con) == DEVICE_TABLET;
    }

    public static boolean isTablet() {
        String deviceCharacteristics = getDeviceCharacteristics();
        return (deviceCharacteristics != null
                    && deviceCharacteristics.contains("tablet"));
    }

    public static enum MethodState {
        UNKNOWN,
        METHOD_ENTERED,
        METHOD_EXITED
    }

    public static boolean isMtkDevice() {
        if (mIsMtkDevice != null) return mIsMtkDevice;

        mIsMtkDevice = MTK_DEVICES.contains(Build.HARDWARE.toLowerCase());
        return mIsMtkDevice;
    }
    
    public static boolean isXperiaDevice() {
        if (mIsXperiaDevice != null) return mIsXperiaDevice;

        mIsXperiaDevice = Build.MANUFACTURER.equalsIgnoreCase("sony")
                && !isMtkDevice();
        return mIsXperiaDevice;
    }

    public static boolean hasGeminiSupport() {
        if (mHasGeminiSupport != null) return mHasGeminiSupport;

        mHasGeminiSupport = SystemProp.getBoolean("ro.mediatek.gemini_support", false);
        return mHasGeminiSupport;
    }

    public static boolean isWifiOnly(Context con) {
        // returns true if device doesn't support mobile data (is wifi only)
        if (mIsWifiOnly != null) return mIsWifiOnly;

        try {
            ConnectivityManager cm = (ConnectivityManager) con.getSystemService(
                    Context.CONNECTIVITY_SERVICE);
            mIsWifiOnly = (cm.getNetworkInfo(ConnectivityManager.TYPE_MOBILE) == null);
            return mIsWifiOnly;
        } catch (Throwable t) {
            mIsWifiOnly = null;
            return false;
        }
    }

    // to be called from settings or other user activities
    public static boolean hasTelephonySupport(Context con) {
        // returns false if device has no phone radio (no telephony support)
        if (mHasTelephonySupport != null) return mHasTelephonySupport;

        try {
            TelephonyManager tm = (TelephonyManager) con.getSystemService(
                Context.TELEPHONY_SERVICE);
            mHasTelephonySupport = (tm.getPhoneType() != TelephonyManager.PHONE_TYPE_NONE);
            return mHasTelephonySupport;
        } catch (Throwable t) {
            mHasTelephonySupport = null;
            return false;
        }
    }

    // to be called from system context only
    public static boolean hasTelephonySupport() {
        try {
            Resources res = XResources.getSystem();
            return res.getBoolean(res.getIdentifier("config_voice_capable", "bool", "android"));
        } catch (Throwable t) {
            log("hasTelephonySupport(): " + t.getMessage());
            return false;
        }
    }

    public static boolean hasVibrator(Context con) {
        if (mHasVibrator != null) return mHasVibrator;

        try {
            Vibrator v = (Vibrator) con.getSystemService(Context.VIBRATOR_SERVICE);
            mHasVibrator = v.hasVibrator();
            return mHasVibrator;
        } catch (Throwable t) {
            mHasVibrator = null;
            return false;
        }
    }

    public static boolean hasFlash(Context con) {
        if (mHasFlash != null) return mHasFlash;

        try {
            PackageManager pm = con.getPackageManager();
            mHasFlash = pm.hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH);
            return mHasFlash;
        } catch (Throwable t) {
            mHasFlash = null;
            return false;
        }
    }

    public static boolean hasGPS(Context con) {
        if (mHasGPS != null) return mHasGPS;

        try {
            PackageManager pm = con.getPackageManager();
            mHasGPS = pm.hasSystemFeature(PackageManager.FEATURE_LOCATION_GPS);
            return mHasGPS;
        } catch (Throwable t) {
            mHasGPS = null;
            return false;
        }
    }

    public static String getDeviceCharacteristics() {
        if (mDeviceCharacteristics != null) return mDeviceCharacteristics;

        mDeviceCharacteristics = SystemProp.get("ro.build.characteristics");
        return mDeviceCharacteristics;
    }

    public static boolean shouldAllowMoreVolumeSteps() {
        return !("GT-I9505G".equals(Build.MODEL) &&
                    !isMtkDevice());
    }

    public static String join(String[] stringArray, String separator) {
        String buf = "";
        for (String s : stringArray) {
            if (!buf.isEmpty()) buf += separator;
            buf += s;
        }
        return buf;
    }

    static class SystemProp extends Utils {
        
        private SystemProp() {

        }

        // Get the value for the given key
        // @param key: key to lookup
        // @return null if the key isn't found
        public static String get(String key) {
            String ret;

            try {
                Class<?> classSystemProperties = findClass("android.os.SystemProperties", null);
                ret = (String) callStaticMethod(classSystemProperties, "get", key);
            } catch (Throwable t) {
                log("SystemProp.get failed: " + t.getMessage());
                ret = null;
            }
            return ret;
        }

        // Get the value for the given key
        // @param key: key to lookup
        // @param def: default value to return
        // @return if the key isn't found, return def if it isn't null, or an empty string otherwise
        public static String get(String key, String def) {
            String ret = def;
            
            try {
                Class<?> classSystemProperties = findClass("android.os.SystemProperties", null);
                ret = (String) callStaticMethod(classSystemProperties, "get", key, def);
            } catch (Throwable t) {
                log("SystemProp.get failed: " + t.getMessage());
                ret = def;
            }
            return ret;
        }

        // Get the value for the given key, and return as an integer
        // @param key: key to lookup
        // @param def: default value to return
        // @return the key parsed as an integer, or def if the key isn't found or cannot be parsed
        public static Integer getInt(String key, Integer def) {
            Integer ret = def;
            
            try {
                Class<?> classSystemProperties = findClass("android.os.SystemProperties", null);
                ret = (Integer) callStaticMethod(classSystemProperties, "getInt", key, def);
            } catch (Throwable t) {
                log("SystemProp.getInt failed: " + t.getMessage());
                ret = def;
            }
            return ret;
        }

        // Get the value for the given key, and return as a long
        // @param key: key to lookup
        // @param def: default value to return
        // @return the key parsed as a long, or def if the key isn't found or cannot be parsed
        public static Long getLong(String key, Long def) {
            Long ret = def;
            
            try {
                Class<?> classSystemProperties = findClass("android.os.SystemProperties", null);
                ret = (Long) callStaticMethod(classSystemProperties, "getLong", key, def);
            } catch (Throwable t) {
                log("SystemProp.getLong failed: " + t.getMessage());
                ret = def;
            }
            return ret;
        }

        // Get the value (case insensitive) for the given key, returned as a boolean
        // Values 'n', 'no', '0', 'false' or 'off' are considered false
        // Values 'y', 'yes', '1', 'true' or 'on' are considered true
        // If the key does not exist, or has any other value, then the default result is returned
        // @param key: key to lookup
        // @param def: default value to return
        // @return the key parsed as a boolean, or def if the key isn't found or cannot be parsed
        public static Boolean getBoolean(String key, boolean def) {
            Boolean ret = def;
            
            try {
                Class<?> classSystemProperties = findClass("android.os.SystemProperties", null);
                ret = (Boolean) callStaticMethod(classSystemProperties, "getBoolean", key, def);
            } catch (Throwable t) {
                log("SystemProp.getBoolean failed: " + t.getMessage());
                ret = def;
            }
            return ret;
        }
    }
}
