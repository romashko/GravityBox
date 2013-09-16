package com.ceco.gm2.gravitybox;

import android.content.res.Resources;
import android.content.res.XModuleResources;
import android.content.res.XResources;
import android.os.Build;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;

public class SystemWideResources {

    public static void initResources(final XSharedPreferences prefs) {
        try {
            Resources systemRes = XResources.getSystem();

            XModuleResources modRes = XModuleResources.createInstance(GravityBox.MODULE_PATH, null);

            if (Build.VERSION.SDK_INT > 16) {
                XResources.setSystemWideReplacement("android", "bool", "config_animateScreenLights", true);
            }

            boolean holoBgDither = prefs.getBoolean(GravityBoxSettings.PREF_KEY_HOLO_BG_DITHER, false);
            if (prefs.getBoolean(GravityBoxSettings.PREF_KEY_HOLO_BG_SOLID_BLACK, false)) {
                XResources.setSystemWideReplacement(
                    "android", "drawable", "background_holo_dark", modRes.fwd(R.drawable.background_holo_dark_solid));
            } else if (holoBgDither) {
                XResources.setSystemWideReplacement(
                        "android", "drawable", "background_holo_dark", modRes.fwd(R.drawable.background_holo_dark));
            }
            if (holoBgDither) {
                XResources.setSystemWideReplacement(
                        "android", "drawable", "background_holo_light", modRes.fwd(R.drawable.background_holo_light));
            }

            if (prefs.getBoolean(GravityBoxSettings.PREF_KEY_NAVBAR_OVERRIDE, false)) {
                XResources.setSystemWideReplacement("android", "bool", "config_showNavigationBar",
                        prefs.getBoolean(GravityBoxSettings.PREF_KEY_NAVBAR_ENABLE, 
                                systemRes.getBoolean(systemRes.getIdentifier(
                                        "config_showNavigationBar", "bool", "android"))));
            }
        } catch (Throwable t) {
            XposedBridge.log(t);
        }
    }

}
