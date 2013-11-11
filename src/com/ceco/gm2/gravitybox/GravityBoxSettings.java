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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.ceco.gm2.gravitybox.preference.AppPickerPreference;
import com.ceco.gm2.gravitybox.preference.AutoBrightnessDialogPreference;
import com.ceco.gm2.gravitybox.preference.SeekBarPreference;
import com.ceco.gm2.gravitybox.quicksettings.TileOrderActivity;

import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.MultiSelectListPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.provider.MediaStore;
import android.provider.Settings;
import android.util.Log;
import android.view.Display;
import android.view.Window;
import android.widget.Toast;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Rect;
import net.margaritov.preference.colorpicker.ColorPickerPreference;

public class GravityBoxSettings extends Activity implements GravityBoxResultReceiver.Receiver {
    public static final String PREF_KEY_QUICK_SETTINGS = "pref_quick_settings2";
    public static final String PREF_KEY_QUICK_SETTINGS_TILE_ORDER = "pref_qs_tile_order";
    public static final String PREF_KEY_QUICK_SETTINGS_TILES_PER_ROW = "pref_qs_tiles_per_row";
    public static final String PREF_KEY_QUICK_SETTINGS_AUTOSWITCH = "pref_auto_switch_qs";
    public static final String PREF_KEY_QUICK_PULLDOWN = "pref_quick_pulldown";
    public static final int QUICK_PULLDOWN_OFF = 0;
    public static final int QUICK_PULLDOWN_RIGHT = 1;
    public static final int QUICK_PULLDOWN_LEFT = 2;

    public static final String PREF_KEY_BATTERY_STYLE = "pref_battery_style";
    public static final String PREF_KEY_BATTERY_PERCENT_TEXT = "pref_battery_percent_text";
    public static final int BATTERY_STYLE_STOCK = 1;
    public static final int BATTERY_STYLE_CIRCLE = 2;
    public static final int BATTERY_STYLE_CIRCLE_PERCENT = 3;
    public static final int BATTERY_STYLE_NONE = 0;

    public static final String PREF_KEY_LOW_BATTERY_WARNING_POLICY = "pref_low_battery_warning_policy";
    public static final int BATTERY_WARNING_POPUP = 1;
    public static final int BATTERY_WARNING_SOUND = 2;

    public static final String PREF_KEY_SIGNAL_ICON_AUTOHIDE = "pref_signal_icon_autohide";
    public static final String PREF_KEY_DISABLE_ROAMING_INDICATORS = "pref_disable_roaming_indicators";
    public static final String ACTION_DISABLE_ROAMING_INDICATORS_CHANGED = "gravitybox.intent.action.DISABLE_ROAMING_INDICATORS_CHANGED";
    public static final String EXTRA_INDICATORS_DISABLED = "indicatorsDisabled";
    public static final String PREF_KEY_POWEROFF_ADVANCED = "pref_poweroff_advanced";
    public static final String PREF_KEY_POWERMENU_SCREENSHOT = "pref_powermenu_screenshot";

    public static final String PREF_KEY_VOL_KEY_CURSOR_CONTROL = "pref_vol_key_cursor_control";
    public static final int VOL_KEY_CURSOR_CONTROL_OFF = 0;
    public static final int VOL_KEY_CURSOR_CONTROL_ON = 1;
    public static final int VOL_KEY_CURSOR_CONTROL_ON_REVERSE = 2;

    public static final String PREF_KEY_RECENTS_CLEAR_ALL = "pref_recents_clear_all2";
    public static final String PREF_KEY_RAMBAR = "pref_rambar";
    public static final String PREF_KEY_RECENTS_CLEAR_MARGIN_TOP = "pref_recent_clear_margin_top";
    public static final int RECENT_CLEAR_OFF = 0;
    public static final int RECENT_CLEAR_TOP_LEFT = 51;
    public static final int RECENT_CLEAR_TOP_RIGHT = 53;
    public static final int RECENT_CLEAR_BOTTOM_LEFT = 83;
    public static final int RECENT_CLEAR_BOTTOM_RIGHT = 85;

    public static final String PREF_CAT_KEY_PHONE = "pref_cat_phone";
    public static final String PREF_KEY_CALLER_FULLSCREEN_PHOTO = "pref_caller_fullscreen_photo";
    public static final String PREF_KEY_CALLER_UNKNOWN_PHOTO_ENABLE = "pref_caller_unknown_photo_enable";
    public static final String PREF_KEY_CALLER_UNKNOWN_PHOTO = "pref_caller_unknown_photo";
    public static final String PREF_KEY_ROAMING_WARNING_DISABLE = "pref_roaming_warning_disable";
    public static final String PREF_KEY_NATIONAL_ROAMING = "pref_national_roaming";
    public static final String PREF_CAT_KEY_FIXES = "pref_cat_fixes";
    public static final String PREF_KEY_FIX_DATETIME_CRASH = "pref_fix_datetime_crash";
    public static final String PREF_KEY_FIX_CALLER_ID_PHONE = "pref_fix_caller_id_phone";
    public static final String PREF_KEY_FIX_CALLER_ID_MMS = "pref_fix_caller_id_mms";
    public static final String PREF_KEY_FIX_MMS_WAKELOCK = "pref_mms_fix_wakelock";
    public static final String PREF_KEY_FIX_CALENDAR = "pref_fix_calendar";
    public static final String PREF_CAT_KEY_STATUSBAR = "pref_cat_statusbar";
    public static final String PREF_CAT_KEY_STATUSBAR_QS = "pref_cat_statusbar_qs";
    public static final String PREF_KEY_STATUSBAR_BGCOLOR = "pref_statusbar_bgcolor2";
    public static final String PREF_KEY_STATUSBAR_COLOR_FOLLOW_STOCK_BATTERY = "pref_sbcolor_follow_stock_battery";
    public static final String PREF_KEY_STATUSBAR_ICON_COLOR_ENABLE = "pref_statusbar_icon_color_enable";
    public static final String PREF_KEY_STATUSBAR_ICON_COLOR = "pref_statusbar_icon_color";
    public static final String PREF_KEY_STATUSBAR_DATA_ACTIVITY_COLOR = "pref_statusbar_data_activity_color";
    public static final String PREF_KEY_STATUSBAR_COLOR_SKIP_BATTERY = "pref_statusbar_color_skip_battery";
    public static final String PREF_KEY_STATUSBAR_SIGNAL_COLOR_MODE = "pref_statusbar_signal_color_mode";
    public static final String PREF_KEY_STATUSBAR_CENTER_CLOCK = "pref_statusbar_center_clock";
    public static final String PREF_KEY_STATUSBAR_CLOCK_DOW = "pref_statusbar_clock_dow2";
    public static final int DOW_DISABLED = 0;
    public static final int DOW_STANDARD = 1;
    public static final int DOW_LOWERCASE = 2;
    public static final int DOW_UPPERCASE = 3;
    public static final String PREF_KEY_STATUSBAR_CLOCK_AMPM_HIDE = "pref_clock_ampm_hide";
    public static final String PREF_KEY_STATUSBAR_CLOCK_HIDE = "pref_clock_hide";
    public static final String PREF_KEY_STATUSBAR_CLOCK_LINK = "pref_clock_link_app";
    public static final String PREF_KEY_ALARM_ICON_HIDE = "pref_alarm_icon_hide";
    public static final String PREF_KEY_TM_STATUSBAR_LAUNCHER = "pref_tm_statusbar_launcher";
    public static final String PREF_KEY_TM_STATUSBAR_LOCKSCREEN = "pref_tm_statusbar_lockscreen";
    public static final String PREF_KEY_TM_NAVBAR_LAUNCHER = "pref_tm_navbar_launcher";
    public static final String PREF_KEY_TM_NAVBAR_LOCKSCREEN = "pref_tm_navbar_lockscreen";
    public static final String PREF_KEY_FIX_TTS_SETTINGS = "pref_fix_tts_settings";
    public static final String PREF_KEY_FIX_DEV_OPTS = "pref_fix_dev_opts";
    public static final String PREF_KEY_ABOUT_GRAVITYBOX = "pref_about_gb";
    public static final String PREF_KEY_ABOUT_XPOSED = "pref_about_xposed";
    public static final String PREF_KEY_ABOUT_DONATE = "pref_about_donate";
    public static final String PREF_KEY_CRT_OFF_EFFECT = "pref_crt_off_effect";
    public static final String PREF_KEY_UNPLUG_TURNS_ON_SCREEN = "pref_unplug_turns_on_screen";
    public static final String PREF_KEY_ENGINEERING_MODE = "pref_engineering_mode";
    public static final String APP_MESSAGING = "com.android.mms";
    public static final String APP_ENGINEERING_MODE = "com.mediatek.engineermode";
    public static final String APP_ENGINEERING_MODE_CLASS = "com.mediatek.engineermode.EngineerMode";
    public static final String PREF_KEY_DUAL_SIM_RINGER = "pref_dual_sim_ringer";
    public static final String APP_DUAL_SIM_RINGER = "dualsim.ringer";
    public static final String APP_DUAL_SIM_RINGER_CLASS = "dualsim.ringer.main";

    public static final String PREF_CAT_KEY_LOCKSCREEN = "pref_cat_lockscreen";
    public static final String PREF_CAT_KEY_LOCKSCREEN_BACKGROUND = "pref_cat_lockscreen_background";
    public static final String PREF_KEY_LOCKSCREEN_BACKGROUND = "pref_lockscreen_background";
    public static final String PREF_KEY_LOCKSCREEN_BACKGROUND_COLOR = "pref_lockscreen_bg_color";
    public static final String PREF_KEY_LOCKSCREEN_BACKGROUND_IMAGE = "pref_lockscreen_bg_image";
    public static final String PREF_KEY_LOCKSCREEN_SHADE_DISABLE = "pref_lockscreen_shade_disable";
    public static final String LOCKSCREEN_BG_DEFAULT = "default";
    public static final String LOCKSCREEN_BG_COLOR = "color";
    public static final String LOCKSCREEN_BG_IMAGE = "image";

    public static final String PREF_KEY_LOCKSCREEN_BATTERY_ARC = "pref_lockscreen_battery_arc";
    public static final String PREF_KEY_LOCKSCREEN_MAXIMIZE_WIDGETS = "pref_lockscreen_maximize_widgets";
    public static final String PREF_KEY_LOCKSCREEN_WIDGET_LIMIT_DISABLE = "pref_lockscreen_widget_limit_disable";
    public static final String PREF_KEY_LOCKSCREEN_ROTATION = "pref_lockscreen_rotation";
    public static final String PREF_KEY_LOCKSCREEN_MENU_KEY = "pref_lockscreen_menu_key";
    public static final String PREF_KEY_LOCKSCREEN_QUICK_UNLOCK = "pref_lockscreen_quick_unlock";
    public static final String PREF_KEY_STATUSBAR_LOCK_POLICY = "pref_statusbar_lock_policy";
    public static final int SBL_POLICY_DEFAULT = 0;
    public static final int SBL_POLICY_UNLOCKED = 1;
    public static final int SBL_POLICY_LOCKED = 2;

    public static final String PREF_KEY_FLASHING_LED_DISABLE = "pref_flashing_led_disable";
    public static final String PREF_KEY_CHARGING_LED_DISABLE = "pref_charging_led_disable";

    public static final String PREF_CAT_KEY_DISPLAY = "pref_cat_display";
    public static final String PREF_KEY_EXPANDED_DESKTOP = "pref_expanded_desktop";
    public static final int ED_DISABLED = 0;
    public static final int ED_STATUSBAR = 1;
    public static final int ED_NAVBAR = 2;
    public static final int ED_BOTH = 3;
    public static final String ACTION_PREF_EXPANDED_DESKTOP_MODE_CHANGED = "gravitybox.intent.action.EXPANDED_DESKTOP_MODE_CHANGED";
    public static final String EXTRA_ED_MODE = "expandedDesktopMode";
    public static final String PREF_CAT_KEY_BRIGHTNESS = "pref_cat_brightness";
    public static final String PREF_KEY_BRIGHTNESS_MASTER_SWITCH = "pref_brightness_master_switch";
    public static final String PREF_KEY_BRIGHTNESS_MIN = "pref_brightness_min2";
    public static final String PREF_KEY_SCREEN_DIM_LEVEL = "pref_screen_dim_level";
    public static final String PREF_KEY_AUTOBRIGHTNESS = "pref_autobrightness";
    public static final String PREF_KEY_HOLO_BG_SOLID_BLACK = "pref_holo_bg_solid_black";
    public static final String PREF_KEY_HOLO_BG_DITHER = "pref_holo_bg_dither";

    public static final String PREF_CAT_KEY_MEDIA = "pref_cat_media";
    public static final String PREF_KEY_VOL_MUSIC_CONTROLS = "pref_vol_music_controls";
    public static final String PREF_KEY_MUSIC_VOLUME_STEPS = "pref_music_volume_steps";
    public static final String PREF_KEY_SAFE_MEDIA_VOLUME = "pref_safe_media_volume";
    public static final String PREF_KEY_VOLUME_PANEL_EXPANDABLE = "pref_volume_panel_expandable";
    public static final String PREF_KEY_VOLUME_PANEL_FULLY_EXPANDABLE = "pref_volume_panel_expand_fully";
    public static final String PREF_KEY_VOLUME_PANEL_AUTOEXPAND = "pref_volume_panel_autoexpand";
    public static final String PREF_KEY_VOLUME_ADJUST_MUTE = "pref_volume_adjust_mute";
    public static final String ACTION_PREF_VOLUME_PANEL_MODE_CHANGED = "gravitybox.intent.action.VOLUME_PANEL_MODE_CHANGED";
    public static final String EXTRA_EXPANDABLE = "expandable";
    public static final String EXTRA_EXPANDABLE_FULLY = "expandable_fully";
    public static final String EXTRA_AUTOEXPAND = "autoExpand";
    public static final String EXTRA_MUTED = "muted";
    public static final String PREF_KEY_LINK_VOLUMES = "pref_link_volumes";
    public static final String ACTION_PREF_LINK_VOLUMES_CHANGED = "gravitybox.intent.action.LINK_VOLUMES_CHANGED";
    public static final String EXTRA_LINKED = "linked";

    public static final String PREF_CAT_HWKEY_ACTIONS = "pref_cat_hwkey_actions";
    public static final String PREF_CAT_HWKEY_MENU = "pref_cat_hwkey_menu";
    public static final String PREF_KEY_HWKEY_MENU_LONGPRESS = "pref_hwkey_menu_longpress";
    public static final String PREF_KEY_HWKEY_MENU_DOUBLETAP = "pref_hwkey_menu_doubletap";
    public static final String PREF_CAT_HWKEY_HOME = "pref_cat_hwkey_home";
    public static final String PREF_KEY_HWKEY_HOME_LONGPRESS = "pref_hwkey_home_longpress";
    public static final String PREF_KEY_HWKEY_HOME_DOUBLETAP_DISABLE = "pref_hwkey_home_doubletap_disable";
    public static final String PREF_KEY_HWKEY_HOME_LONGPRESS_KEYGUARD = "pref_hwkey_home_longpress_keyguard";
    public static final String PREF_CAT_HWKEY_BACK = "pref_cat_hwkey_back";
    public static final String PREF_KEY_HWKEY_BACK_LONGPRESS = "pref_hwkey_back_longpress";
    public static final String PREF_CAT_HWKEY_RECENTS = "pref_cat_hwkey_recents";
    public static final String PREF_KEY_HWKEY_RECENTS_SINGLETAP = "pref_hwkey_recents_singletap";
    public static final String PREF_KEY_HWKEY_RECENTS_LONGPRESS = "pref_hwkey_recents_longpress";
    public static final String PREF_KEY_HWKEY_CUSTOM_APP = "pref_hwkey_custom_app";
    public static final String PREF_KEY_HWKEY_CUSTOM_APP2 = "pref_hwkey_custom_app2";
    public static final String PREF_KEY_HWKEY_DOUBLETAP_SPEED = "pref_hwkey_doubletap_speed";
    public static final String PREF_KEY_HWKEY_KILL_DELAY = "pref_hwkey_kill_delay";
    public static final String PREF_CAT_HWKEY_VOLUME = "pref_cat_hwkey_volume";
    public static final String PREF_KEY_VOLUME_ROCKER_WAKE_DISABLE = "pref_volume_rocker_wake_disable";
    public static final int HWKEY_ACTION_DEFAULT = 0;
    public static final int HWKEY_ACTION_SEARCH = 1;
    public static final int HWKEY_ACTION_VOICE_SEARCH = 2;
    public static final int HWKEY_ACTION_PREV_APP = 3;
    public static final int HWKEY_ACTION_KILL = 4;
    public static final int HWKEY_ACTION_SLEEP = 5;
    public static final int HWKEY_ACTION_RECENT_APPS = 6;
    public static final int HWKEY_ACTION_CUSTOM_APP = 7;
    public static final int HWKEY_ACTION_CUSTOM_APP2 = 8;
    public static final int HWKEY_ACTION_MENU = 9;
    public static final int HWKEY_ACTION_EXPANDED_DESKTOP = 10;
    public static final int HWKEY_ACTION_TORCH = 11;
    public static final int HWKEY_ACTION_APP_LAUNCHER = 12;
    public static final int HWKEY_DOUBLETAP_SPEED_DEFAULT = 400;
    public static final int HWKEY_KILL_DELAY_DEFAULT = 1000;
    public static final String ACTION_PREF_HWKEY_MENU_LONGPRESS_CHANGED = "gravitybox.intent.action.HWKEY_MENU_LONGPRESS_CHANGED";
    public static final String ACTION_PREF_HWKEY_MENU_DOUBLETAP_CHANGED = "gravitybox.intent.action.HWKEY_MENU_DOUBLETAP_CHANGED";
    public static final String ACTION_PREF_HWKEY_HOME_LONGPRESS_CHANGED = "gravitybox.intent.action.HWKEY_HOME_LONGPRESS_CHANGED";
    public static final String ACTION_PREF_HWKEY_HOME_DOUBLETAP_CHANGED = "gravitybox.intent.action.HWKEY_HOME_DOUBLETAP_CHANGED";
    public static final String ACTION_PREF_HWKEY_BACK_LONGPRESS_CHANGED = "gravitybox.intent.action.HWKEY_BACK_LONGPRESS_CHANGED";
    public static final String ACTION_PREF_HWKEY_RECENTS_SINGLETAP_CHANGED = "gravitybox.intent.action.HWKEY_RECENTS_SINGLETAP_CHANGED";
    public static final String ACTION_PREF_HWKEY_RECENTS_LONGPRESS_CHANGED = "gravitybox.intent.action.HWKEY_RECENTS_LONGPRESS_CHANGED";
    public static final String ACTION_PREF_HWKEY_DOUBLETAP_SPEED_CHANGED = "gravitybox.intent.action.HWKEY_DOUBLETAP_SPEED_CHANGED";
    public static final String ACTION_PREF_HWKEY_KILL_DELAY_CHANGED = "gravitybox.intent.action.HWKEY_KILL_DELAY_CHANGED";
    public static final String ACTION_PREF_VOLUME_ROCKER_WAKE_CHANGED = "gravitybox.intent.action.VOLUME_ROCKER_WAKE_CHANGED";
    public static final String EXTRA_HWKEY_VALUE = "hwKeyValue";
    public static final String EXTRA_HWKEY_HOME_DOUBLETAP_DISABLE = "hwKeyHomeDoubletapDisable";
    public static final String EXTRA_HWKEY_HOME_LONGPRESS_KG = "hwKeyHomeLongpressKeyguard";
    public static final String EXTRA_VOLUME_ROCKER_WAKE_DISABLE = "volumeRockerWakeDisable";

    public static final String PREF_KEY_PHONE_FLIP = "pref_phone_flip";
    public static final int PHONE_FLIP_ACTION_NONE = 0;
    public static final int PHONE_FLIP_ACTION_MUTE = 1;
    public static final int PHONE_FLIP_ACTION_DISMISS = 2;
    public static final String PREF_KEY_CALL_VIBRATIONS = "pref_call_vibrations";
    public static final String CV_CONNECTED = "connected";
    public static final String CV_DISCONNECTED = "disconnected";
    public static final String CV_WAITING = "waiting";
    public static final String CV_PERIODIC = "periodic";

    public static final String PREF_CAT_KEY_NOTIF_DRAWER_STYLE = "pref_cat_notification_drawer_style";
    public static final String PREF_KEY_NOTIF_BACKGROUND = "pref_notif_background";
    public static final String PREF_KEY_NOTIF_COLOR = "pref_notif_color";
    public static final String PREF_KEY_NOTIF_COLOR_MODE = "pref_notif_color_mode";
    public static final String PREF_KEY_NOTIF_IMAGE_PORTRAIT = "pref_notif_image_portrait";
    public static final String PREF_KEY_NOTIF_IMAGE_LANDSCAPE = "pref_notif_image_landscape";
    public static final String PREF_KEY_NOTIF_BACKGROUND_ALPHA = "pref_notif_background_alpha";
    public static final String NOTIF_BG_DEFAULT = "default";
    public static final String NOTIF_BG_COLOR = "color";
    public static final String NOTIF_BG_IMAGE = "image";
    public static final String NOTIF_BG_COLOR_MODE_OVERLAY = "overlay";
    public static final String NOTIF_BG_COLOR_MODE_UNDERLAY = "underlay";
    public static final String ACTION_NOTIF_BACKGROUND_CHANGED = "gravitybox.intent.action.NOTIF_BACKGROUND_CHANGED";
    public static final String EXTRA_BG_TYPE = "bgType";
    public static final String EXTRA_BG_COLOR = "bgColor";
    public static final String EXTRA_BG_COLOR_MODE = "bgColorMode";
    public static final String EXTRA_BG_ALPHA = "bgAlpha";

    public static final String PREF_KEY_PIE_CONTROL_ENABLE = "pref_pie_control_enable2";
    public static final String PREF_KEY_PIE_CONTROL_CUSTOM_KEY = "pref_pie_control_custom_key";
    public static final String PREF_KEY_PIE_CONTROL_MENU = "pref_pie_control_menu";
    public static final String PREF_KEY_PIE_CONTROL_TRIGGERS = "pref_pie_control_trigger_positions";
    public static final String PREF_KEY_PIE_CONTROL_TRIGGER_SIZE = "pref_pie_control_trigger_size";
    public static final String PREF_KEY_PIE_CONTROL_SIZE = "pref_pie_control_size";
    public static final String PREF_KEY_HWKEYS_DISABLE = "pref_hwkeys_disable";
    public static final String PREF_KEY_PIE_COLOR_BG = "pref_pie_color_bg";
    public static final String PREF_KEY_PIE_COLOR_FG = "pref_pie_color_fg";
    public static final String PREF_KEY_PIE_COLOR_OUTLINE = "pref_pie_color_outline";
    public static final String PREF_KEY_PIE_COLOR_SELECTED = "pref_pie_color_selected";
    public static final String PREF_KEY_PIE_COLOR_TEXT = "pref_pie_color_text";
    public static final String PREF_KEY_PIE_COLOR_RESET = "pref_pie_color_reset";
    public static final int PIE_CUSTOM_KEY_OFF = 0;
    public static final int PIE_CUSTOM_KEY_SEARCH = 1;
    public static final int PIE_CUSTOM_KEY_APP_LAUNCHER = 2;
    public static final String ACTION_PREF_PIE_CHANGED = "gravitybox.intent.action.PREF_PIE_CHANGED";
    public static final String EXTRA_PIE_ENABLE = "pieEnable";
    public static final String EXTRA_PIE_CUSTOM_KEY_MODE = "pieCustomKeyMode";
    public static final String EXTRA_PIE_MENU = "pieMenu";
    public static final String EXTRA_PIE_TRIGGERS = "pieTriggers";
    public static final String EXTRA_PIE_TRIGGER_SIZE = "pieTriggerSize";
    public static final String EXTRA_PIE_SIZE = "pieSize";
    public static final String EXTRA_PIE_HWKEYS_DISABLE = "hwKeysDisable";
    public static final String EXTRA_PIE_COLOR_BG = "pieColorBg";
    public static final String EXTRA_PIE_COLOR_FG = "pieColorFg";
    public static final String EXTRA_PIE_COLOR_OUTLINE = "pieColorOutline";
    public static final String EXTRA_PIE_COLOR_SELECTED = "pieColorSelected";
    public static final String EXTRA_PIE_COLOR_TEXT = "pieColorText";

    public static final String PREF_KEY_BUTTON_BACKLIGHT_MODE = "pref_button_backlight_mode";
    public static final String PREF_KEY_BUTTON_BACKLIGHT_NOTIFICATIONS = "pref_button_backlight_notifications";
    public static final String ACTION_PREF_BUTTON_BACKLIGHT_CHANGED = "gravitybox.intent.action.BUTTON_BACKLIGHT_CHANGED";
    public static final String EXTRA_BB_MODE = "bbMode";
    public static final String EXTRA_BB_NOTIF = "bbNotif";
    public static final String BB_MODE_DEFAULT = "default";
    public static final String BB_MODE_DISABLE = "disable";
    public static final String BB_MODE_ALWAYS_ON = "always_on";

    public static final String PREF_KEY_QUICKAPP_DEFAULT = "pref_quickapp_default";
    public static final String PREF_KEY_QUICKAPP_SLOT1 = "pref_quickapp_slot1";
    public static final String PREF_KEY_QUICKAPP_SLOT2 = "pref_quickapp_slot2";
    public static final String PREF_KEY_QUICKAPP_SLOT3 = "pref_quickapp_slot3";
    public static final String PREF_KEY_QUICKAPP_SLOT4 = "pref_quickapp_slot4";
    public static final String ACTION_PREF_QUICKAPP_CHANGED = "gravitybox.intent.action.QUICKAPP_CHANGED";
    public static final String EXTRA_QUICKAPP_DEFAULT = "quickAppDefault";
    public static final String EXTRA_QUICKAPP_SLOT1 = "quickAppSlot1";
    public static final String EXTRA_QUICKAPP_SLOT2 = "quickAppSlot2";
    public static final String EXTRA_QUICKAPP_SLOT3 = "quickAppSlot3";
    public static final String EXTRA_QUICKAPP_SLOT4 = "quickAppSlot4";

    public static final String PREF_KEY_GB_THEME_DARK = "pref_gb_theme_dark";
    public static final String FILE_THEME_DARK_FLAG = "theme_dark";

    public static final String ACTION_PREF_BATTERY_STYLE_CHANGED = "gravitybox.intent.action.BATTERY_STYLE_CHANGED";
    public static final String ACTION_PREF_SIGNAL_ICON_AUTOHIDE_CHANGED = "gravitybox.intent.action.SIGNAL_ICON_AUTOHIDE_CHANGED";

    public static final String ACTION_PREF_STATUSBAR_COLOR_CHANGED = "gravitybox.intent.action.STATUSBAR_COLOR_CHANGED";
    public static final String EXTRA_SB_BG_COLOR = "bgColor";
    public static final String EXTRA_SB_COLOR_FOLLOW = "sbColorFollow";
    public static final String EXTRA_SB_ICON_COLOR_ENABLE = "iconColorEnable";
    public static final String EXTRA_SB_ICON_COLOR = "iconColor";
    public static final String EXTRA_SB_DATA_ACTIVITY_COLOR = "dataActivityColor";
    public static final String EXTRA_SB_COLOR_SKIP_BATTERY = "skipBattery";
    public static final String EXTRA_SB_SIGNAL_COLOR_MODE = "signalColorMode";
    public static final String EXTRA_TM_SB_LAUNCHER = "tmSbLauncher";
    public static final String EXTRA_TM_SB_LOCKSCREEN = "tmSbLockscreen";
    public static final String EXTRA_TM_NB_LAUNCHER = "tmNbLauncher";
    public static final String EXTRA_TM_NB_LOCKSCREEN = "tmNbLockscreen";

    public static final String ACTION_PREF_QUICKSETTINGS_CHANGED = "gravitybox.intent.action.QUICKSETTINGS_CHANGED";
    public static final String EXTRA_QS_PREFS = "qsPrefs";
    public static final String EXTRA_QS_COLS = "qsCols";
    public static final String EXTRA_QS_AUTOSWITCH = "qsAutoSwitch";
    public static final String EXTRA_QUICK_PULLDOWN = "quickPulldown";

    public static final String ACTION_PREF_CLOCK_CHANGED = "gravitybox.intent.action.CENTER_CLOCK_CHANGED";
    public static final String EXTRA_CENTER_CLOCK = "centerClock";
    public static final String EXTRA_CLOCK_DOW = "clockDow";
    public static final String EXTRA_AMPM_HIDE = "ampmHide";
    public static final String EXTRA_CLOCK_HIDE = "clockHide";
    public static final String EXTRA_CLOCK_LINK = "clockLink";
    public static final String EXTRA_ALARM_HIDE = "alarmHide";

    public static final String ACTION_PREF_SAFE_MEDIA_VOLUME_CHANGED = "gravitybox.intent.action.SAFE_MEDIA_VOLUME_CHANGED";
    public static final String EXTRA_SAFE_MEDIA_VOLUME_ENABLED = "enabled";

    public static final String PREF_CAT_KEY_NAVBAR_KEYS = "pref_cat_navbar_keys";
    public static final String PREF_CAT_KEY_NAVBAR_COLOR = "pref_cat_navbar_color";
    public static final String PREF_CAT_KEY_NAVBAR_DIMEN = "pref_cat_navbar_dimen";
    public static final String PREF_KEY_NAVBAR_OVERRIDE = "pref_navbar_override";
    public static final String PREF_KEY_NAVBAR_ENABLE = "pref_navbar_enable";
    public static final String PREF_KEY_NAVBAR_HEIGHT = "pref_navbar_height";
    public static final String PREF_KEY_NAVBAR_HEIGHT_LANDSCAPE = "pref_navbar_height_landscape";
    public static final String PREF_KEY_NAVBAR_WIDTH = "pref_navbar_width";
    public static final String PREF_KEY_NAVBAR_MENUKEY = "pref_navbar_menukey";
    public static final String PREF_KEY_NAVBAR_LAUNCHER_ENABLE = "pref_navbar_launcher_enable";
    public static final String PREF_KEY_NAVBAR_KEY_COLOR = "pref_navbar_key_color";
    public static final String PREF_KEY_NAVBAR_KEY_GLOW_COLOR = "pref_navbar_key_glow_color";
    public static final String PREF_KEY_NAVBAR_BG_COLOR = "pref_navbar_bg_color";
    public static final String ACTION_PREF_NAVBAR_CHANGED = "gravitybox.intent.action.ACTION_NAVBAR_CHANGED";
    public static final String EXTRA_NAVBAR_HEIGHT = "navbarHeight";
    public static final String EXTRA_NAVBAR_HEIGHT_LANDSCAPE = "navbarHeightLandscape";
    public static final String EXTRA_NAVBAR_WIDTH = "navbarWidth";
    public static final String EXTRA_NAVBAR_MENUKEY = "navbarMenukey";
    public static final String EXTRA_NAVBAR_LAUNCHER_ENABLE = "navbarLauncherEnable";
    public static final String EXTRA_NAVBAR_KEY_COLOR = "navbarKeyColor";
    public static final String EXTRA_NAVBAR_KEY_GLOW_COLOR = "navbarKeyGlowColor";
    public static final String EXTRA_NAVBAR_BG_COLOR = "navbarBgColor";

    public static final String PREF_KEY_LOCKSCREEN_TARGETS_ENABLE = "pref_lockscreen_ring_targets_enable";
    public static final String PREF_KEY_LOCKSCREEN_TARGETS_APP[] = new String[] {
        "pref_lockscreen_ring_targets_app0", "pref_lockscreen_ring_targets_app1", "pref_lockscreen_ring_targets_app2",
        "pref_lockscreen_ring_targets_app3", "pref_lockscreen_ring_targets_app4"
    };
    public static final String PREF_KEY_LOCKSCREEN_TARGETS_VERTICAL_OFFSET = "pref_lockscreen_ring_targets_vertical_offset";
    public static final String PREF_KEY_LOCKSCREEN_TARGETS_HORIZONTAL_OFFSET = "pref_lockscreen_ring_targets_horizontal_offset";

    public static final String PREF_KEY_STATUSBAR_BRIGHTNESS = "pref_statusbar_brightness";
    public static final String ACTION_PREF_STATUSBAR_BRIGHTNESS_CHANGED = "gravitybox.intent.action.STATUSBAR_BRIGHTNESS_CHANGED";
    public static final String EXTRA_SB_BRIGHTNESS = "sbBrightness";

    public static final String PREF_KEY_MMS_UNICODE_STRIPPING = "pref_mms_unicode_stripping";
    public static final String UNISTR_LEAVE_INTACT = "leave_intact";
    public static final String UNISTR_NON_ENCODABLE = "non_encodable";
    public static final String UNISTR_ALL = "all";

    public static final String PREF_CAT_KEY_PHONE_TELEPHONY = "pref_cat_phone_telephony";
    public static final String PREF_CAT_KEY_PHONE_MESSAGING = "pref_cat_phone_messaging";
    public static final String PREF_CAT_KEY_PHONE_MOBILE_DATA = "pref_cat_phone_mobile_data";
    public static final String PREF_KEY_MOBILE_DATA_SLOW2G_DISABLE = "pref_mobile_data_slow2g_disable";

    public static final String PREF_KEY_NETWORK_MODE_TILE_MODE = "pref_network_mode_tile_mode";
    public static final String EXTRA_NMT_MODE = "networkModeTileMode";

    public static final String PREF_KEY_DISPLAY_ALLOW_ALL_ROTATIONS = "pref_display_allow_all_rotations";
    public static final String ACTION_PREF_DISPLAY_ALLOW_ALL_ROTATIONS_CHANGED = 
            "gravitybox.intent.action.DISPLAY_ALLOW_ALL_ROTATIONS_CHANGED";
    public static final String EXTRA_ALLOW_ALL_ROTATIONS = "allowAllRotations";

    public static final String PREF_KEY_QS_TILE_BEHAVIOUR_OVERRIDE = "pref_qs_tile_behaviour_override";

    public static final String PREF_KEY_QS_NETWORK_MODE_SIM_SLOT = "pref_qs_network_mode_sim_slot";
    public static final String ACTION_PREF_QS_NETWORK_MODE_SIM_SLOT_CHANGED =
            "gravitybox.intent.action.QS_NETWORK_MODE_SIM_SLOT_CHANGED";
    public static final String EXTRA_SIM_SLOT = "simSlot";

    public static final String PREF_KEY_ONGOING_NOTIFICATIONS = "pref_ongoing_notifications";
    public static final String ACTION_PREF_ONGOING_NOTIFICATIONS_CHANGED = 
            "gravitybox.intent.action.ONGOING_NOTIFICATIONS_CHANGED";
    public static final String EXTRA_ONGOING_NOTIF = "ongoingNotif";
    public static final String EXTRA_ONGOING_NOTIF_RESET = "ongoingNotifReset";

    public static final String PREF_CAT_KEY_DATA_TRAFFIC = "pref_cat_data_traffic";
    public static final String PREF_KEY_DATA_TRAFFIC_ENABLE = "pref_data_traffic_enable";
    public static final String PREF_KEY_DATA_TRAFFIC_POSITION = "pref_data_traffic_position";
    public static final int DT_POSITION_AUTO = 0;
    public static final int DT_POSITION_LEFT = 1;
    public static final int DT_POSITION_RIGHT = 2;
    public static final String PREF_KEY_DATA_TRAFFIC_SIZE = "pref_data_traffic_size";
    public static final String ACTION_PREF_DATA_TRAFFIC_CHANGED = 
            "gravitybox.intent.action.DATA_TRAFFIC_CHANGED";
    public static final String EXTRA_DT_ENABLE = "dtEnable";
    public static final String EXTRA_DT_POSITION = "dtPosition";
    public static final String EXTRA_DT_SIZE = "dtSize";

    public static final String PREF_CAT_KEY_APP_LAUNCHER = "pref_cat_app_launcher";
    public static final List<String> PREF_KEY_APP_LAUNCHER_SLOT = new ArrayList<String>(Arrays.asList(
            "pref_app_launcher_slot0", "pref_app_launcher_slot1", "pref_app_launcher_slot2",
            "pref_app_launcher_slot3", "pref_app_launcher_slot4", "pref_app_launcher_slot5",
            "pref_app_launcher_slot6", "pref_app_launcher_slot7"));
    public static final String ACTION_PREF_APP_LAUNCHER_CHANGED = "gravitybox.intent.action.APP_LAUNCHER_CHANGED";
    public static final String EXTRA_APP_LAUNCHER_SLOT = "appLauncherSlot";
    public static final String EXTRA_APP_LAUNCHER_APP = "appLauncherApp";

    private static final int REQ_LOCKSCREEN_BACKGROUND = 1024;
    private static final int REQ_NOTIF_BG_IMAGE_PORTRAIT = 1025;
    private static final int REQ_NOTIF_BG_IMAGE_LANDSCAPE = 1026;
    private static final int REQ_CALLER_PHOTO = 1027;

    private static final List<String> rebootKeys = new ArrayList<String>(Arrays.asList(
            PREF_KEY_FIX_DATETIME_CRASH,
            PREF_KEY_FIX_CALENDAR,
            PREF_KEY_FIX_CALLER_ID_PHONE,
            PREF_KEY_FIX_CALLER_ID_MMS,
            PREF_KEY_FIX_TTS_SETTINGS,
            PREF_KEY_FIX_DEV_OPTS,
            PREF_KEY_BRIGHTNESS_MIN,
            PREF_KEY_LOCKSCREEN_MENU_KEY,
            PREF_KEY_FIX_MMS_WAKELOCK,
            PREF_KEY_MUSIC_VOLUME_STEPS,
            PREF_KEY_HOLO_BG_SOLID_BLACK,
            PREF_KEY_HOLO_BG_DITHER,
            PREF_KEY_SCREEN_DIM_LEVEL,
            PREF_KEY_BRIGHTNESS_MASTER_SWITCH,
            PREF_KEY_NAVBAR_OVERRIDE,
            PREF_KEY_NAVBAR_ENABLE,
            PREF_KEY_QS_TILE_BEHAVIOUR_OVERRIDE,
            PREF_KEY_UNPLUG_TURNS_ON_SCREEN
    ));

    private static final class SystemProperties {
        public boolean hasGeminiSupport;
        public boolean isTablet;
        public boolean hasNavigationBar;
        public boolean unplugTurnsOnScreen;

        public SystemProperties(Bundle data) {
            if (data.containsKey("hasGeminiSupport")) {
                hasGeminiSupport = data.getBoolean("hasGeminiSupport");
            }
            if (data.containsKey("isTablet")) {
                isTablet = data.getBoolean("isTablet");
            }
            if (data.containsKey("hasNavigationBar")) {
                hasNavigationBar = data.getBoolean("hasNavigationBar");
            }
            if (data.containsKey("unplugTurnsOnScreen")) {
                unplugTurnsOnScreen = data.getBoolean("unplugTurnsOnScreen");
            }
        }
    }

    private GravityBoxResultReceiver mReceiver;
    private Handler mHandler;
    private static SystemProperties sSystemProperties;
    private Dialog mAlertDialog;
    private ProgressDialog mProgressDialog;
    private Runnable mGetSystemPropertiesTimeout = new Runnable() {
        @Override
        public void run() {
            dismissProgressDialog();
            AlertDialog.Builder builder = new AlertDialog.Builder(GravityBoxSettings.this)
                .setTitle(R.string.app_name)
                .setMessage(R.string.gb_startup_error)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        finish();
                    }
                });
            mAlertDialog = builder.create();
            mAlertDialog.show();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // set Holo Dark theme if flag file exists
        File file = new File(getFilesDir() + "/" + FILE_THEME_DARK_FLAG);
        if (file.exists()) {
            this.setTheme(android.R.style.Theme_Holo);
        }

        super.onCreate(savedInstanceState);

        if (savedInstanceState == null || sSystemProperties == null) {
            mReceiver = new GravityBoxResultReceiver(new Handler());
            mReceiver.setReceiver(this);
            Intent intent = new Intent();
            intent.setAction(SystemPropertyProvider.ACTION_GET_SYSTEM_PROPERTIES);
            intent.putExtra("receiver", mReceiver);
            mProgressDialog = new ProgressDialog(this);
            mProgressDialog.setIndeterminate(true);
            mProgressDialog.setTitle(R.string.app_name);
            mProgressDialog.setMessage(getString(R.string.gb_startup_progress));
            mProgressDialog.setCancelable(false);
            mProgressDialog.show();
            mHandler = new Handler();
            mHandler.postDelayed(mGetSystemPropertiesTimeout, 5000);
            sendBroadcast(intent);
        }
    }

    @Override
    protected void onDestroy() {
        if (mHandler != null) {
            mHandler.removeCallbacks(mGetSystemPropertiesTimeout);
            mHandler = null;
        }
        dismissProgressDialog();
        dismissAlertDialog();

        super.onDestroy();
    }

    @Override
    public void onReceiveResult(int resultCode, Bundle resultData) {
        if (mHandler != null) {
            mHandler.removeCallbacks(mGetSystemPropertiesTimeout);
            mHandler = null;
        }
        dismissProgressDialog();
        Log.d("GravityBox", "result received: resultCode=" + resultCode);
        if (resultCode == SystemPropertyProvider.RESULT_SYSTEM_PROPERTIES) {
            sSystemProperties = new SystemProperties(resultData);
            getFragmentManager().beginTransaction().replace(android.R.id.content, new PrefsFragment()).commit();
        } else {
            finish();
        }
    }

    private void dismissProgressDialog() {
        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            mProgressDialog.dismiss();
        }
        mProgressDialog = null;
    }

    private void dismissAlertDialog() {
        if (mAlertDialog != null && mAlertDialog.isShowing()) {
            mAlertDialog.dismiss();
        }
        mAlertDialog = null;
    }

    public static class PrefsFragment extends PreferenceFragment implements OnSharedPreferenceChangeListener {
        private ListPreference mBatteryStyle;
        private CheckBoxPreference mPrefBatteryPercent;
        private ListPreference mLowBatteryWarning;
        private MultiSelectListPreference mSignalIconAutohide;
        private SharedPreferences mPrefs;
        private AlertDialog mDialog;
        private MultiSelectListPreference mQuickSettings;
        private ColorPickerPreference mStatusbarBgColor;
        private Preference mPrefAboutGb;
        private Preference mPrefAboutXposed;
        private Preference mPrefAboutDonate;
        private Preference mPrefEngMode;
        private Preference mPrefDualSimRinger;
        private PreferenceCategory mPrefCatLockscreenBg;
        private ListPreference mPrefLockscreenBg;
        private ColorPickerPreference mPrefLockscreenBgColor;
        private Preference mPrefLockscreenBgImage;
        private File wallpaperImage;
        private File wallpaperTemporary;
        private File notifBgImagePortrait;
        private File notifBgImageLandscape;
        private PreferenceScreen mPrefCatHwKeyActions;
        private PreferenceCategory mPrefCatHwKeyMenu;
        private ListPreference mPrefHwKeyMenuLongpress;
        private ListPreference mPrefHwKeyMenuDoubletap;
        private PreferenceCategory mPrefCatHwKeyHome;
        private ListPreference mPrefHwKeyHomeLongpress;
        private CheckBoxPreference mPrefHwKeyHomeLongpressKeyguard;
        private PreferenceCategory mPrefCatHwKeyBack;
        private ListPreference mPrefHwKeyBackLongpress;
        private PreferenceCategory mPrefCatHwKeyRecents;
        private ListPreference mPrefHwKeyRecentsSingletap;
        private ListPreference mPrefHwKeyRecentsLongpress;
        private PreferenceCategory mPrefCatHwKeyVolume;
        private ListPreference mPrefHwKeyDoubletapSpeed;
        private ListPreference mPrefHwKeyKillDelay;
        private ListPreference mPrefPhoneFlip;
        private CheckBoxPreference mPrefSbIconColorEnable;
        private ColorPickerPreference mPrefSbIconColor;
        private ColorPickerPreference mPrefSbDaColor;
        private PreferenceScreen mPrefCatFixes;
        private CheckBoxPreference mPrefFixDateTimeCrash;
        private CheckBoxPreference mPrefFixCallerIDPhone;
        private CheckBoxPreference mPrefFixCallerIDMms;
        private CheckBoxPreference mPrefFixMmsWakelock;
        private CheckBoxPreference mPrefFixCalendar;
        private CheckBoxPreference mPrefFixTtsSettings;
        private CheckBoxPreference mPrefFixDevOpts;
        private PreferenceScreen mPrefCatStatusbar;
        private PreferenceScreen mPrefCatStatusbarQs;
        private CheckBoxPreference mPrefAutoSwitchQs;
        private ListPreference mPrefQuickPulldown;
        private PreferenceScreen mPrefCatNotifDrawerStyle;
        private ListPreference mPrefNotifBackground;
        private ColorPickerPreference mPrefNotifColor;
        private Preference mPrefNotifImagePortrait;
        private Preference mPrefNotifImageLandscape;
        private ListPreference mPrefNotifColorMode;
        private CheckBoxPreference mPrefDisableRoamingIndicators;
        private ListPreference mPrefButtonBacklightMode;
        private ListPreference mPrefPieEnabled;
        private ListPreference mPrefPieCustomKey;
        private CheckBoxPreference mPrefPieHwKeysDisabled;
        private ColorPickerPreference mPrefPieColorBg;
        private ColorPickerPreference mPrefPieColorFg;
        private ColorPickerPreference mPrefPieColorOutline;
        private ColorPickerPreference mPrefPieColorSelected;
        private ColorPickerPreference mPrefPieColorText;
        private Preference mPrefPieColorReset;
        private CheckBoxPreference mPrefGbThemeDark;
        private ListPreference mPrefRecentClear;
        private ListPreference mPrefRambar;
        private PreferenceScreen mPrefCatPhone;
        private CheckBoxPreference mPrefRoamingWarningDisable;
        private CheckBoxPreference mPrefBrightnessMasterSwitch;
        private SeekBarPreference mPrefBrightnessMin;
        private SeekBarPreference mPrefScreenDimLevel;
        private AutoBrightnessDialogPreference mPrefAutoBrightness;
        private PreferenceScreen mPrefCatLockscreen;
        private PreferenceScreen mPrefCatDisplay;
        private PreferenceScreen mPrefCatBrightness;
        private CheckBoxPreference mPrefCrtOff;
        private PreferenceScreen mPrefCatMedia;
        private CheckBoxPreference mPrefSafeMediaVolume;
        private ListPreference mPrefExpandedDesktop;
        private PreferenceCategory mPrefCatNavbarKeys;
        private PreferenceCategory mPrefCatNavbarColor;
        private PreferenceCategory mPrefCatNavbarDimen;
        private CheckBoxPreference mPrefNavbarEnable;
        private CheckBoxPreference mPrefMusicVolumeSteps;
        private AppPickerPreference[] mPrefLockscreenTargetsApp;
        private SeekBarPreference mPrefLockscreenTargetsVerticalOffset;
        private SeekBarPreference mPrefLockscreenTargetsHorizontalOffset;
        private CheckBoxPreference mPrefMobileDataSlow2gDisable;
        private PreferenceCategory mPrefCatPhoneTelephony;
        private PreferenceCategory mPrefCatPhoneMessaging;
        private PreferenceCategory mPrefCatPhoneMobileData;
        private ListPreference mPrefNetworkModeTileMode;
        private MultiSelectListPreference mPrefQsTileBehaviourOverride;
        private ListPreference mPrefQsNetworkModeSimSlot;
        private CheckBoxPreference mPrefSbColorSkipBattery;
        private ListPreference mPrefSbSignalColorMode;
        private CheckBoxPreference mPrefUnplugTurnsOnScreen;
        private MultiSelectListPreference mPrefCallVibrations;
        private Preference mPrefQsTileOrder;
        private ListPreference mPrefSbClockDow;
        private ListPreference mPrefSbLockPolicy;
        private ListPreference mPrefDataTrafficPosition;
        private ListPreference mPrefDataTrafficSize;
        private CheckBoxPreference mPrefLinkVolumes;
        private CheckBoxPreference mPrefVolumePanelExpandable;
        private CheckBoxPreference mPrefVolumePanelFullyExpandable;
        private CheckBoxPreference mPrefVolumePanelAutoexpand;
        private CheckBoxPreference mPrefHomeDoubletapDisable;
        private PreferenceScreen mPrefCatAppLauncher;
        private AppPickerPreference[] mPrefAppLauncherSlot;
        private File callerPhotoFile;
        private CheckBoxPreference mPrefCallerUnknownPhotoEnable;
        private Preference mPrefCallerUnknownPhoto;

        @SuppressWarnings("deprecation")
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            // this is important because although the handler classes that read these settings
            // are in the same package, they are executed in the context of the hooked package
            getPreferenceManager().setSharedPreferencesMode(Context.MODE_WORLD_READABLE);
            addPreferencesFromResource(R.xml.gravitybox);

            mPrefs = getPreferenceScreen().getSharedPreferences();

            mBatteryStyle = (ListPreference) findPreference(PREF_KEY_BATTERY_STYLE);
            mPrefBatteryPercent = (CheckBoxPreference) findPreference(PREF_KEY_BATTERY_PERCENT_TEXT);
            mLowBatteryWarning = (ListPreference) findPreference(PREF_KEY_LOW_BATTERY_WARNING_POLICY);
            mSignalIconAutohide = (MultiSelectListPreference) findPreference(PREF_KEY_SIGNAL_ICON_AUTOHIDE);
            mQuickSettings = (MultiSelectListPreference) findPreference(PREF_KEY_QUICK_SETTINGS);
            mStatusbarBgColor = (ColorPickerPreference) findPreference(PREF_KEY_STATUSBAR_BGCOLOR);

            mPrefAboutGb = (Preference) findPreference(PREF_KEY_ABOUT_GRAVITYBOX);
            
            String version = "";
            try {
                PackageInfo pInfo = getActivity().getPackageManager().getPackageInfo(getActivity().getPackageName(), 0);
                version = " v" + pInfo.versionName;
            } catch (NameNotFoundException e) {
                e.printStackTrace();
            } finally {
                mPrefAboutGb.setTitle(getActivity().getTitle() + version);
            }

            mPrefAboutXposed = (Preference) findPreference(PREF_KEY_ABOUT_XPOSED);
            mPrefAboutDonate = (Preference) findPreference(PREF_KEY_ABOUT_DONATE);

            mPrefEngMode = (Preference) findPreference(PREF_KEY_ENGINEERING_MODE);
            if (!isAppInstalled(APP_ENGINEERING_MODE)) {
                getPreferenceScreen().removePreference(mPrefEngMode);
            }

            mPrefDualSimRinger = (Preference) findPreference(PREF_KEY_DUAL_SIM_RINGER);
            if (!isAppInstalled(APP_DUAL_SIM_RINGER)) {
                getPreferenceScreen().removePreference(mPrefDualSimRinger);
            }

            mPrefCatLockscreenBg = 
                    (PreferenceCategory) findPreference(PREF_CAT_KEY_LOCKSCREEN_BACKGROUND);
            mPrefLockscreenBg = (ListPreference) findPreference(PREF_KEY_LOCKSCREEN_BACKGROUND);
            mPrefLockscreenBgColor = 
                    (ColorPickerPreference) findPreference(PREF_KEY_LOCKSCREEN_BACKGROUND_COLOR);
            mPrefLockscreenBgImage = 
                    (Preference) findPreference(PREF_KEY_LOCKSCREEN_BACKGROUND_IMAGE);

            wallpaperImage = new File(getActivity().getFilesDir() + "/lockwallpaper"); 
            wallpaperTemporary = new File(getActivity().getCacheDir() + "/lockwallpaper.tmp");
            notifBgImagePortrait = new File(getActivity().getFilesDir() + "/notifwallpaper");
            notifBgImageLandscape = new File(getActivity().getFilesDir() + "/notifwallpaper_landscape");
            callerPhotoFile = new File(getActivity().getFilesDir() + "/caller_photo");

            mPrefCatHwKeyActions = (PreferenceScreen) findPreference(PREF_CAT_HWKEY_ACTIONS);
            mPrefCatHwKeyMenu = (PreferenceCategory) findPreference(PREF_CAT_HWKEY_MENU);
            mPrefHwKeyMenuLongpress = (ListPreference) findPreference(PREF_KEY_HWKEY_MENU_LONGPRESS);
            mPrefHwKeyMenuDoubletap = (ListPreference) findPreference(PREF_KEY_HWKEY_MENU_DOUBLETAP);
            mPrefCatHwKeyHome = (PreferenceCategory) findPreference(PREF_CAT_HWKEY_HOME);
            mPrefHwKeyHomeLongpress = (ListPreference) findPreference(PREF_KEY_HWKEY_HOME_LONGPRESS);
            mPrefHwKeyHomeLongpressKeyguard = (CheckBoxPreference) findPreference(PREF_KEY_HWKEY_HOME_LONGPRESS_KEYGUARD);
            mPrefCatHwKeyBack = (PreferenceCategory) findPreference(PREF_CAT_HWKEY_BACK);
            mPrefHwKeyBackLongpress = (ListPreference) findPreference(PREF_KEY_HWKEY_BACK_LONGPRESS);
            mPrefCatHwKeyRecents = (PreferenceCategory) findPreference(PREF_CAT_HWKEY_RECENTS);
            mPrefHwKeyRecentsSingletap = (ListPreference) findPreference(PREF_KEY_HWKEY_RECENTS_SINGLETAP);
            mPrefHwKeyRecentsLongpress = (ListPreference) findPreference(PREF_KEY_HWKEY_RECENTS_LONGPRESS);
            mPrefHwKeyDoubletapSpeed = (ListPreference) findPreference(PREF_KEY_HWKEY_DOUBLETAP_SPEED);
            mPrefHwKeyKillDelay = (ListPreference) findPreference(PREF_KEY_HWKEY_KILL_DELAY);
            mPrefCatHwKeyVolume = (PreferenceCategory) findPreference(PREF_CAT_HWKEY_VOLUME);
            mPrefHomeDoubletapDisable = (CheckBoxPreference) findPreference(PREF_KEY_HWKEY_HOME_DOUBLETAP_DISABLE);

            mPrefPhoneFlip = (ListPreference) findPreference(PREF_KEY_PHONE_FLIP);

            mPrefSbIconColorEnable = (CheckBoxPreference) findPreference(PREF_KEY_STATUSBAR_ICON_COLOR_ENABLE);
            mPrefSbIconColor = (ColorPickerPreference) findPreference(PREF_KEY_STATUSBAR_ICON_COLOR);
            mPrefSbDaColor = (ColorPickerPreference) findPreference(PREF_KEY_STATUSBAR_DATA_ACTIVITY_COLOR);
            mPrefSbColorSkipBattery = (CheckBoxPreference) findPreference(PREF_KEY_STATUSBAR_COLOR_SKIP_BATTERY);
            mPrefSbSignalColorMode = (ListPreference) findPreference(PREF_KEY_STATUSBAR_SIGNAL_COLOR_MODE);

            mPrefCatFixes = (PreferenceScreen) findPreference(PREF_CAT_KEY_FIXES);
            mPrefFixDateTimeCrash = (CheckBoxPreference) findPreference(PREF_KEY_FIX_DATETIME_CRASH);
            mPrefFixCallerIDPhone = (CheckBoxPreference) findPreference(PREF_KEY_FIX_CALLER_ID_PHONE);
            mPrefFixCallerIDMms = (CheckBoxPreference) findPreference(PREF_KEY_FIX_CALLER_ID_MMS);
            mPrefFixMmsWakelock = (CheckBoxPreference) findPreference(PREF_KEY_FIX_MMS_WAKELOCK);
            mPrefFixCalendar = (CheckBoxPreference) findPreference(PREF_KEY_FIX_CALENDAR);
            mPrefFixTtsSettings = (CheckBoxPreference) findPreference(PREF_KEY_FIX_TTS_SETTINGS);
            mPrefFixDevOpts = (CheckBoxPreference) findPreference(PREF_KEY_FIX_DEV_OPTS);
            mPrefCatStatusbar = (PreferenceScreen) findPreference(PREF_CAT_KEY_STATUSBAR);
            mPrefCatStatusbarQs = (PreferenceScreen) findPreference(PREF_CAT_KEY_STATUSBAR_QS);
            mPrefAutoSwitchQs = (CheckBoxPreference) findPreference(PREF_KEY_QUICK_SETTINGS_AUTOSWITCH);
            mPrefQuickPulldown = (ListPreference) findPreference(PREF_KEY_QUICK_PULLDOWN);

            mPrefCatNotifDrawerStyle = (PreferenceScreen) findPreference(PREF_CAT_KEY_NOTIF_DRAWER_STYLE);
            mPrefNotifBackground = (ListPreference) findPreference(PREF_KEY_NOTIF_BACKGROUND);
            mPrefNotifColor = (ColorPickerPreference) findPreference(PREF_KEY_NOTIF_COLOR);
            mPrefNotifImagePortrait = (Preference) findPreference(PREF_KEY_NOTIF_IMAGE_PORTRAIT);
            mPrefNotifImageLandscape = (Preference) findPreference(PREF_KEY_NOTIF_IMAGE_LANDSCAPE);
            mPrefNotifColorMode = (ListPreference) findPreference(PREF_KEY_NOTIF_COLOR_MODE);

            mPrefDisableRoamingIndicators = (CheckBoxPreference) findPreference(PREF_KEY_DISABLE_ROAMING_INDICATORS);
            mPrefButtonBacklightMode = (ListPreference) findPreference(PREF_KEY_BUTTON_BACKLIGHT_MODE);

            mPrefPieEnabled = (ListPreference) findPreference(PREF_KEY_PIE_CONTROL_ENABLE);
            mPrefPieHwKeysDisabled = (CheckBoxPreference) findPreference(PREF_KEY_HWKEYS_DISABLE);
            mPrefPieCustomKey = (ListPreference) findPreference(PREF_KEY_PIE_CONTROL_CUSTOM_KEY);
            mPrefPieColorBg = (ColorPickerPreference) findPreference(PREF_KEY_PIE_COLOR_BG);
            mPrefPieColorFg = (ColorPickerPreference) findPreference(PREF_KEY_PIE_COLOR_FG);
            mPrefPieColorOutline = (ColorPickerPreference) findPreference(PREF_KEY_PIE_COLOR_OUTLINE);
            mPrefPieColorSelected = (ColorPickerPreference) findPreference(PREF_KEY_PIE_COLOR_SELECTED);
            mPrefPieColorText = (ColorPickerPreference) findPreference(PREF_KEY_PIE_COLOR_TEXT);
            mPrefPieColorReset = (Preference) findPreference(PREF_KEY_PIE_COLOR_RESET);

            mPrefGbThemeDark = (CheckBoxPreference) findPreference(PREF_KEY_GB_THEME_DARK);
            File file = new File(getActivity().getFilesDir() + "/" + FILE_THEME_DARK_FLAG);
            mPrefGbThemeDark.setChecked(file.exists());

            mPrefRecentClear = (ListPreference) findPreference(PREF_KEY_RECENTS_CLEAR_ALL);
            mPrefRambar = (ListPreference) findPreference(PREF_KEY_RAMBAR);

            mPrefCatPhone = (PreferenceScreen) findPreference(PREF_CAT_KEY_PHONE);
            mPrefRoamingWarningDisable = (CheckBoxPreference) findPreference(PREF_KEY_ROAMING_WARNING_DISABLE);

            mPrefBrightnessMasterSwitch = (CheckBoxPreference) findPreference(PREF_KEY_BRIGHTNESS_MASTER_SWITCH);
            mPrefBrightnessMin = (SeekBarPreference) findPreference(PREF_KEY_BRIGHTNESS_MIN);
            mPrefBrightnessMin.setMinimum(getResources().getInteger(R.integer.screen_brightness_min));
            mPrefScreenDimLevel = (SeekBarPreference) findPreference(PREF_KEY_SCREEN_DIM_LEVEL);
            mPrefScreenDimLevel.setMinimum(getResources().getInteger(R.integer.screen_brightness_dim_min));
            mPrefAutoBrightness = (AutoBrightnessDialogPreference) findPreference(PREF_KEY_AUTOBRIGHTNESS);

            mPrefCatLockscreen = (PreferenceScreen) findPreference(PREF_CAT_KEY_LOCKSCREEN);
            mPrefCatDisplay = (PreferenceScreen) findPreference(PREF_CAT_KEY_DISPLAY);
            mPrefCatBrightness = (PreferenceScreen) findPreference(PREF_CAT_KEY_BRIGHTNESS);
            mPrefCrtOff = (CheckBoxPreference) findPreference(PREF_KEY_CRT_OFF_EFFECT);
            mPrefUnplugTurnsOnScreen = (CheckBoxPreference) findPreference(PREF_KEY_UNPLUG_TURNS_ON_SCREEN);
            mPrefCatMedia = (PreferenceScreen) findPreference(PREF_CAT_KEY_MEDIA);
            mPrefSafeMediaVolume = (CheckBoxPreference) findPreference(PREF_KEY_SAFE_MEDIA_VOLUME);
            mPrefMusicVolumeSteps = (CheckBoxPreference) findPreference(PREF_KEY_MUSIC_VOLUME_STEPS);
            mPrefLinkVolumes = (CheckBoxPreference) findPreference(PREF_KEY_LINK_VOLUMES);
            mPrefVolumePanelExpandable = (CheckBoxPreference) findPreference(PREF_KEY_VOLUME_PANEL_EXPANDABLE);
            mPrefVolumePanelFullyExpandable = (CheckBoxPreference) findPreference(PREF_KEY_VOLUME_PANEL_FULLY_EXPANDABLE);
            mPrefVolumePanelAutoexpand = (CheckBoxPreference) findPreference(PREF_KEY_VOLUME_PANEL_AUTOEXPAND);

            mPrefExpandedDesktop = (ListPreference) findPreference(PREF_KEY_EXPANDED_DESKTOP);

            mPrefCatNavbarKeys = (PreferenceCategory) findPreference(PREF_CAT_KEY_NAVBAR_KEYS);
            mPrefCatNavbarColor = (PreferenceCategory) findPreference(PREF_CAT_KEY_NAVBAR_COLOR);
            mPrefCatNavbarDimen = (PreferenceCategory) findPreference(PREF_CAT_KEY_NAVBAR_DIMEN);
            mPrefNavbarEnable = (CheckBoxPreference) findPreference(PREF_KEY_NAVBAR_ENABLE);

            mPrefLockscreenTargetsApp = new AppPickerPreference[5];
            for (int i=0; i<=4; i++) {
                mPrefLockscreenTargetsApp[i] = (AppPickerPreference) findPreference(
                        PREF_KEY_LOCKSCREEN_TARGETS_APP[i]);
                String title = String.format(
                        getString(R.string.pref_lockscreen_ring_targets_app_title), (i+1));
                mPrefLockscreenTargetsApp[i].setTitle(title);
                mPrefLockscreenTargetsApp[i].setDialogTitle(title);
            }
            mPrefLockscreenTargetsVerticalOffset = (SeekBarPreference) findPreference(
                    PREF_KEY_LOCKSCREEN_TARGETS_VERTICAL_OFFSET);
            mPrefLockscreenTargetsHorizontalOffset = (SeekBarPreference) findPreference(
                    PREF_KEY_LOCKSCREEN_TARGETS_HORIZONTAL_OFFSET);

            mPrefCatPhoneTelephony = (PreferenceCategory) findPreference(PREF_CAT_KEY_PHONE_TELEPHONY);
            mPrefCatPhoneMessaging = (PreferenceCategory) findPreference(PREF_CAT_KEY_PHONE_MESSAGING);
            mPrefCatPhoneMobileData = (PreferenceCategory) findPreference(PREF_CAT_KEY_PHONE_MOBILE_DATA);
            mPrefMobileDataSlow2gDisable = (CheckBoxPreference) findPreference(PREF_KEY_MOBILE_DATA_SLOW2G_DISABLE);
            mPrefCallVibrations = (MultiSelectListPreference) findPreference(PREF_KEY_CALL_VIBRATIONS);
            mPrefCallerUnknownPhotoEnable = (CheckBoxPreference) findPreference(PREF_KEY_CALLER_UNKNOWN_PHOTO_ENABLE);
            mPrefCallerUnknownPhoto = (Preference) findPreference(PREF_KEY_CALLER_UNKNOWN_PHOTO);

            mPrefNetworkModeTileMode = (ListPreference) findPreference(PREF_KEY_NETWORK_MODE_TILE_MODE);
            mPrefQsTileBehaviourOverride = 
                    (MultiSelectListPreference) findPreference(PREF_KEY_QS_TILE_BEHAVIOUR_OVERRIDE);
            mPrefQsNetworkModeSimSlot = (ListPreference) findPreference(PREF_KEY_QS_NETWORK_MODE_SIM_SLOT);
            mPrefQsTileOrder = (Preference) findPreference(PREF_KEY_QUICK_SETTINGS_TILE_ORDER);

            mPrefSbClockDow = (ListPreference) findPreference(PREF_KEY_STATUSBAR_CLOCK_DOW);
            mPrefSbLockPolicy = (ListPreference) findPreference(PREF_KEY_STATUSBAR_LOCK_POLICY);
            mPrefDataTrafficPosition = (ListPreference) findPreference(PREF_KEY_DATA_TRAFFIC_POSITION);
            mPrefDataTrafficSize = (ListPreference) findPreference(PREF_KEY_DATA_TRAFFIC_SIZE);

            mPrefCatAppLauncher = (PreferenceScreen) findPreference(PREF_CAT_KEY_APP_LAUNCHER);
            mPrefAppLauncherSlot = new AppPickerPreference[PREF_KEY_APP_LAUNCHER_SLOT.size()];
            for (int i = 0; i < mPrefAppLauncherSlot.length; i++) {
                AppPickerPreference appPref = new AppPickerPreference(getActivity(), null);
                appPref.setKey(PREF_KEY_APP_LAUNCHER_SLOT.get(i));
                appPref.setTitle(String.format(
                        getActivity().getString(R.string.pref_app_launcher_slot_title), i + 1));
                appPref.setDialogTitle(appPref.getTitle());
                appPref.setDefaultSummary(getActivity().getString(R.string.app_picker_none));
                appPref.setSummary(getActivity().getString(R.string.app_picker_none));
                mPrefAppLauncherSlot[i] = appPref;
                mPrefCatAppLauncher.addPreference(mPrefAppLauncherSlot[i]);
            }

            // Remove Phone specific preferences on Tablet devices
            if (sSystemProperties.isTablet) {
                mPrefCatStatusbarQs.removePreference(mPrefAutoSwitchQs);
                mPrefCatStatusbarQs.removePreference(mPrefQuickPulldown);
            }

            // Filter preferences according to feature availability 
            if (!Utils.hasFlash(getActivity())) {
                mPrefCatHwKeyHome.removePreference(mPrefHwKeyHomeLongpressKeyguard);
            }
            if (!Utils.hasVibrator(getActivity())) {
                mPrefCatPhoneTelephony.removePreference(mPrefCallVibrations);
            }
            if (!Utils.hasTelephonySupport(getActivity())) {
                mPrefCatPhone.removePreference(mPrefCatPhoneTelephony);
                mPrefCatMedia.removePreference(mPrefLinkVolumes);
                mPrefCatFixes.removePreference(mPrefFixCallerIDPhone);
            }
            if (!isAppInstalled(APP_MESSAGING)) {
                mPrefCatPhone.removePreference(mPrefCatPhoneMessaging);
                mPrefCatFixes.removePreference(mPrefFixCallerIDMms);
                mPrefCatFixes.removePreference(mPrefFixMmsWakelock);
            }
            if (Utils.isWifiOnly(getActivity())) {
                // Remove preferences that don't apply to wifi-only devices
                getPreferenceScreen().removePreference(mPrefCatPhone);
                mPrefCatStatusbarQs.removePreference(mPrefNetworkModeTileMode);
                mPrefCatStatusbar.removePreference(mSignalIconAutohide);
                mPrefCatStatusbar.removePreference(mPrefDisableRoamingIndicators);
                mPrefCatPhoneMobileData.removePreference(mPrefMobileDataSlow2gDisable);
                mPrefCatStatusbarQs.removePreference(mPrefQsNetworkModeSimSlot);
                mPrefCatFixes.removePreference(mPrefFixCallerIDPhone);
                mPrefCatFixes.removePreference(mPrefFixCallerIDMms);
                mPrefCatFixes.removePreference(mPrefFixMmsWakelock);
           	}

            // Remove MTK specific preferences for non-MTK devices
            if (!Utils.isMtkDevice()) {
                getPreferenceScreen().removePreference(mPrefCatFixes);
                mPrefCatStatusbar.removePreference(mSignalIconAutohide);
                mPrefCatStatusbar.removePreference(mPrefDisableRoamingIndicators);
                mQuickSettings.setEntries(R.array.qs_tile_aosp_entries);
                mQuickSettings.setEntryValues(R.array.qs_tile_aosp_values);
                mPrefCatPhoneTelephony.removePreference(mPrefRoamingWarningDisable);
                mPrefCatStatusbarQs.removePreference(mPrefQsNetworkModeSimSlot);
            } else {
                // Remove Gemini specific preferences for non-Gemini MTK devices
                if (!sSystemProperties.hasGeminiSupport) {
                    mPrefCatStatusbar.removePreference(mSignalIconAutohide);
                    mPrefCatStatusbar.removePreference(mPrefDisableRoamingIndicators);
                    mPrefCatPhoneMobileData.removePreference(mPrefMobileDataSlow2gDisable);
                    mPrefCatStatusbarQs.removePreference(mPrefQsNetworkModeSimSlot);
                }

                // Remove preferences not needed for ZTE V987
                if (Build.MODEL.contains("V987") && Build.DISPLAY.contains("ZTE-CN-9B18D-P188F04")) {
                    mPrefCatFixes.removePreference(mPrefFixDateTimeCrash);
                    mPrefCatFixes.removePreference(mPrefFixTtsSettings);
                    mPrefCatFixes.removePreference(mPrefFixDevOpts);
                }

                mQuickSettings.setEntries(R.array.qs_tile_entries);
                mQuickSettings.setEntryValues(R.array.qs_tile_values);
                mPrefCatStatusbarQs.removePreference(mPrefQsTileBehaviourOverride);
            }

            // Remove preferences not compatible with Android 4.1
            if (Build.VERSION.SDK_INT < 17) {
                getPreferenceScreen().removePreference(mPrefCatLockscreen);
                mPrefCatStatusbar.removePreference(mPrefCatStatusbarQs);
                mPrefCatStatusbar.removePreference(mPrefCatNotifDrawerStyle);
                mPrefCatDisplay.removePreference(mPrefCatBrightness);
                mPrefCatDisplay.removePreference(mPrefCrtOff);
                mPrefCatMedia.removePreference(mPrefSafeMediaVolume);
            }

            // Remove preferences not compatible with Android < 4.3+
            if (Build.VERSION.SDK_INT < 18) {
                mPrefCatHwKeyHome.removePreference(mPrefHomeDoubletapDisable);
            }

            // Remove more music volume steps option if necessary
            if (!Utils.shouldAllowMoreVolumeSteps()) {
                mPrefs.edit().putBoolean(PREF_KEY_MUSIC_VOLUME_STEPS, false).commit();
                mPrefCatMedia.removePreference(mPrefMusicVolumeSteps);
            }

            // Remove tiles based on device features
            List<CharSequence> qsEntries = new ArrayList<CharSequence>(Arrays.asList(
                    mQuickSettings.getEntries()));
            List<CharSequence> qsEntryValues = new ArrayList<CharSequence>(Arrays.asList(
                    mQuickSettings.getEntryValues()));
            Set<String> qsPrefs = mPrefs.getStringSet(PREF_KEY_QUICK_SETTINGS, null);
            if (!Utils.hasFlash(getActivity())) {
                qsEntries.remove(getString(R.string.qs_tile_torch));
                qsEntryValues.remove("torch_tileview");
                if (qsPrefs != null && qsPrefs.contains("torch_tileview")) {
                    qsPrefs.remove("torch_tileview");
                }
            }
            if (!Utils.hasGPS(getActivity())) {
                qsEntries.remove(getString(R.string.qs_tile_gps));
                qsEntryValues.remove("gps_tileview");
                if (Utils.isMtkDevice()) {
                    qsEntries.remove(getString(R.string.qs_tile_gps_alt));
                    qsEntryValues.remove("gps_textview");
                }
                if (qsPrefs != null) {
                    if (qsPrefs.contains("gps_tileview")) qsPrefs.remove("gps_tileview");
                    if (qsPrefs.contains("gps_textview")) qsPrefs.remove("gps_textview");
                }
            }
            if (Utils.isWifiOnly(getActivity())) {
                qsEntries.remove(getString(R.string.qs_tile_mobile_data));
                qsEntries.remove(getString(R.string.qs_tile_network_mode));
                qsEntryValues.remove("data_conn_textview");
                qsEntryValues.remove("network_mode_tileview");
                if (qsPrefs != null) {
                    if (qsPrefs.contains("data_conn_textview")) qsPrefs.remove("data_conn_textview");
                    if (qsPrefs.contains("network_mode_tileview")) qsPrefs.remove("network_mode_tileview");
                }
            }
            // and update saved prefs in case it was previously checked in previous versions
            mPrefs.edit().putStringSet(PREF_KEY_QUICK_SETTINGS, qsPrefs).commit();
            mQuickSettings.setEntries(qsEntries.toArray(new CharSequence[qsEntries.size()]));
            mQuickSettings.setEntryValues(qsEntryValues.toArray(new CharSequence[qsEntryValues.size()]));

            // Remove actions for HW keys based on device features
            mPrefHwKeyMenuLongpress.setEntries(R.array.hwkey_action_entries);
            mPrefHwKeyMenuLongpress.setEntryValues(R.array.hwkey_action_values);
            List<CharSequence> actEntries = new ArrayList<CharSequence>(Arrays.asList(
                    mPrefHwKeyMenuLongpress.getEntries()));
            List<CharSequence> actEntryValues = new ArrayList<CharSequence>(Arrays.asList(
                    mPrefHwKeyMenuLongpress.getEntryValues()));
            if (!Utils.hasFlash(getActivity())) {
                actEntries.remove(getString(R.string.hwkey_action_torch));
                actEntryValues.remove("11");
            }
            CharSequence[] actionEntries = actEntries.toArray(new CharSequence[actEntries.size()]);
            CharSequence[] actionEntryValues = actEntryValues.toArray(new CharSequence[actEntryValues.size()]);
            mPrefHwKeyMenuLongpress.setEntries(actionEntries);
            mPrefHwKeyMenuLongpress.setEntryValues(actionEntryValues);
            // other preferences have the exact same entries and entry values
            mPrefHwKeyMenuDoubletap.setEntries(actionEntries);
            mPrefHwKeyMenuDoubletap.setEntryValues(actionEntryValues);
            mPrefHwKeyHomeLongpress.setEntries(actionEntries);
            mPrefHwKeyHomeLongpress.setEntryValues(actionEntryValues);
            mPrefHwKeyBackLongpress.setEntries(actionEntries);
            mPrefHwKeyBackLongpress.setEntryValues(actionEntryValues);
            mPrefHwKeyRecentsSingletap.setEntries(actionEntries);
            mPrefHwKeyRecentsSingletap.setEntryValues(actionEntryValues);
            mPrefHwKeyRecentsLongpress.setEntries(actionEntries);
            mPrefHwKeyRecentsLongpress.setEntryValues(actionEntryValues);

            setDefaultValues();
        }

        @Override
        public void onResume() {
            super.onResume();

            updatePreferences(null);
            mPrefs.registerOnSharedPreferenceChangeListener(this);
        }

        @Override
        public void onPause() {
            mPrefs.unregisterOnSharedPreferenceChangeListener(this);

            if (mDialog != null && mDialog.isShowing()) {
                mDialog.dismiss();
                mDialog = null;
            }

            super.onPause();
        }

        private void setDefaultValues() {
            if (mPrefs.getStringSet(PREF_KEY_QUICK_SETTINGS, null) == null) {
                Editor e = mPrefs.edit();
                String[] values = getResources().getStringArray(
                        Utils.isMtkDevice() ? R.array.qs_tile_values : R.array.qs_tile_aosp_values);
                Set<String> defVal = new HashSet<String>(Arrays.asList(values));
                e.putStringSet(PREF_KEY_QUICK_SETTINGS, defVal);
                e.putString(TileOrderActivity.PREF_KEY_TILE_ORDER, Utils.join(values, ","));
                e.commit();
                mQuickSettings.setValues(defVal);
            }

            boolean value = mPrefs.getBoolean(PREF_KEY_NAVBAR_ENABLE, sSystemProperties.hasNavigationBar);
            mPrefs.edit().putBoolean(PREF_KEY_NAVBAR_ENABLE, value).commit();
            mPrefNavbarEnable.setChecked(value);

            value = mPrefs.getBoolean(PREF_KEY_UNPLUG_TURNS_ON_SCREEN, sSystemProperties.unplugTurnsOnScreen);
            mPrefs.edit().putBoolean(PREF_KEY_UNPLUG_TURNS_ON_SCREEN, value).commit();
            mPrefUnplugTurnsOnScreen.setChecked(value);
        }

        private void updatePreferences(String key) {
            if (key == null || key.equals(PREF_KEY_BATTERY_STYLE)) {
                mBatteryStyle.setSummary(mBatteryStyle.getEntry());
            }

            if (key == null || key.equals(PREF_KEY_LOW_BATTERY_WARNING_POLICY)) {
                mLowBatteryWarning.setSummary(mLowBatteryWarning.getEntry());
            }

            if (key == null || key.equals(PREF_KEY_SIGNAL_ICON_AUTOHIDE)) {
                Set<String> autoHide =  mSignalIconAutohide.getValues();
                String summary = "";
                if (autoHide.contains("notifications_disabled")) {
                    summary += getString(R.string.sim_disable_notifications_summary);
                }
                if (autoHide.contains("sim1")) {
                    if (!summary.isEmpty()) summary += ", ";
                    summary += getString(R.string.sim_slot_1);
                }
                if (autoHide.contains("sim2")) {
                    if (!summary.isEmpty()) summary += ", ";
                    summary += getString(R.string.sim_slot_2);
                }
                if (summary.isEmpty()) {
                    summary = getString(R.string.signal_icon_autohide_summary);
                }
                mSignalIconAutohide.setSummary(summary);
            }

            if (key == null || key.equals(PREF_KEY_LOCKSCREEN_BACKGROUND)) {
                mPrefLockscreenBg.setSummary(mPrefLockscreenBg.getEntry());
                mPrefCatLockscreenBg.removePreference(mPrefLockscreenBgColor);
                mPrefCatLockscreenBg.removePreference(mPrefLockscreenBgImage);
                String option = mPrefs.getString(PREF_KEY_LOCKSCREEN_BACKGROUND, LOCKSCREEN_BG_DEFAULT);
                if (option.equals(LOCKSCREEN_BG_COLOR)) {
                    mPrefCatLockscreenBg.addPreference(mPrefLockscreenBgColor);
                } else if (option.equals(LOCKSCREEN_BG_IMAGE)) {
                    mPrefCatLockscreenBg.addPreference(mPrefLockscreenBgImage);
                }
            }

            if (key == null || key.equals(PREF_KEY_HWKEY_MENU_LONGPRESS)) {
                mPrefHwKeyMenuLongpress.setSummary(mPrefHwKeyMenuLongpress.getEntry());
            }

            if (key == null || key.equals(PREF_KEY_HWKEY_MENU_DOUBLETAP)) {
                mPrefHwKeyMenuDoubletap.setSummary(mPrefHwKeyMenuDoubletap.getEntry());
            }

            if (key == null || key.equals(PREF_KEY_HWKEY_HOME_LONGPRESS)) {
                mPrefHwKeyHomeLongpress.setSummary(mPrefHwKeyHomeLongpress.getEntry());
            }

            if (key == null || key.equals(PREF_KEY_HWKEY_BACK_LONGPRESS)) {
                mPrefHwKeyBackLongpress.setSummary(mPrefHwKeyBackLongpress.getEntry());
            }

            if (key == null || key.equals(PREF_KEY_HWKEY_RECENTS_SINGLETAP)) {
                mPrefHwKeyRecentsSingletap.setSummary(mPrefHwKeyRecentsSingletap.getEntry());
            }

            if (key == null || key.equals(PREF_KEY_HWKEY_RECENTS_LONGPRESS)) {
                mPrefHwKeyRecentsLongpress.setSummary(mPrefHwKeyRecentsLongpress.getEntry());
            }

            if (key == null || key.equals(PREF_KEY_HWKEY_DOUBLETAP_SPEED)) {
                mPrefHwKeyDoubletapSpeed.setSummary(getString(R.string.pref_hwkey_doubletap_speed_summary)
                        + " (" + mPrefHwKeyDoubletapSpeed.getEntry() + ")");
            }

            if (key == null || key.equals(PREF_KEY_HWKEY_KILL_DELAY)) {
                mPrefHwKeyKillDelay.setSummary(getString(R.string.pref_hwkey_kill_delay_summary)
                        + " (" + mPrefHwKeyKillDelay.getEntry() + ")");
            }

            if (key == null || key.equals(PREF_KEY_PHONE_FLIP)) {
                mPrefPhoneFlip.setSummary(getString(R.string.pref_phone_flip_summary)
                        + " (" + mPrefPhoneFlip.getEntry() + ")");
            }

            if (key == null || key.equals(PREF_KEY_STATUSBAR_ICON_COLOR_ENABLE)) {
                mPrefSbIconColor.setEnabled(mPrefSbIconColorEnable.isChecked());
                mPrefSbDaColor.setEnabled(mPrefSbIconColorEnable.isChecked());
                mPrefSbColorSkipBattery.setEnabled(mPrefSbIconColorEnable.isChecked());
                mPrefSbSignalColorMode.setEnabled(mPrefSbIconColorEnable.isChecked());
            }

            if (key == null || key.equals(PREF_KEY_NOTIF_BACKGROUND)) {
                mPrefNotifBackground.setSummary(mPrefNotifBackground.getEntry());
                mPrefCatNotifDrawerStyle.removePreference(mPrefNotifColor);
                mPrefCatNotifDrawerStyle.removePreference(mPrefNotifColorMode);
                mPrefCatNotifDrawerStyle.removePreference(mPrefNotifImagePortrait);
                mPrefCatNotifDrawerStyle.removePreference(mPrefNotifImageLandscape);
                String option = mPrefs.getString(PREF_KEY_NOTIF_BACKGROUND, NOTIF_BG_DEFAULT);
                if (option.equals(NOTIF_BG_COLOR)) {
                    mPrefCatNotifDrawerStyle.addPreference(mPrefNotifColor);
                    mPrefCatNotifDrawerStyle.addPreference(mPrefNotifColorMode);
                } else if (option.equals(NOTIF_BG_IMAGE)) {
                    mPrefCatNotifDrawerStyle.addPreference(mPrefNotifImagePortrait);
                    mPrefCatNotifDrawerStyle.addPreference(mPrefNotifImageLandscape);
                    mPrefCatNotifDrawerStyle.addPreference(mPrefNotifColorMode);
                }
            }

            if (key == null || key.equals(PREF_KEY_NOTIF_COLOR_MODE)) {
                mPrefNotifColorMode.setSummary(mPrefNotifColorMode.getEntry());
            }

            if (key == null || key.equals(PREF_KEY_BUTTON_BACKLIGHT_MODE)) {
                mPrefButtonBacklightMode.setSummary(mPrefButtonBacklightMode.getEntry());
            }

            if (key == null || key.equals(PREF_KEY_PIE_CONTROL_ENABLE)) {
                final int pieMode = 
                        Integer.valueOf(mPrefs.getString(PREF_KEY_PIE_CONTROL_ENABLE, "0"));
                if (pieMode == 0) {
                    if (mPrefPieHwKeysDisabled.isChecked()) {
                        Editor e = mPrefs.edit();
                        e.putBoolean(PREF_KEY_HWKEYS_DISABLE, false);
                        e.commit();
                        mPrefPieHwKeysDisabled.setChecked(false);
                    }
                    mPrefPieHwKeysDisabled.setEnabled(false);
                } else {
                    mPrefPieHwKeysDisabled.setEnabled(true);
                }
                mPrefPieEnabled.setSummary(mPrefPieEnabled.getEntry());
            }

            if (key == null || key.equals(PREF_KEY_RECENTS_CLEAR_ALL)) {
                mPrefRecentClear.setSummary(mPrefRecentClear.getEntry());
            }

            if (key == null || key.equals(PREF_KEY_RAMBAR)) {
                mPrefRambar.setSummary(mPrefRambar.getEntry());
            }

            if (key == null || key.equals(PREF_KEY_BRIGHTNESS_MASTER_SWITCH)) {
                final boolean enabled = mPrefBrightnessMasterSwitch.isChecked();
                mPrefBrightnessMin.setEnabled(enabled);
                mPrefScreenDimLevel.setEnabled(enabled);
                mPrefAutoBrightness.setEnabled(enabled);
            }

            if (key == null || key.equals(PREF_KEY_EXPANDED_DESKTOP)) {
                mPrefExpandedDesktop.setSummary(mPrefExpandedDesktop.getEntry());
            }

            if (key == null || key.equals(PREF_KEY_NAVBAR_OVERRIDE)
                    || key.equals(PREF_KEY_NAVBAR_ENABLE)) {
                final boolean override = mPrefs.getBoolean(PREF_KEY_NAVBAR_OVERRIDE, false);
                mPrefNavbarEnable.setEnabled(override);
                mPrefCatNavbarKeys.setEnabled(override && mPrefNavbarEnable.isChecked());
                mPrefCatNavbarColor.setEnabled(override && mPrefNavbarEnable.isChecked());
                mPrefCatNavbarDimen.setEnabled(override && mPrefNavbarEnable.isChecked());
            }

            if (key == null || key.equals(PREF_KEY_LOCKSCREEN_TARGETS_ENABLE)) {
                final boolean enabled = mPrefs.getBoolean(PREF_KEY_LOCKSCREEN_TARGETS_ENABLE, false);
                for(Preference p : mPrefLockscreenTargetsApp) {
                    p.setEnabled(enabled);
                }
            }
            mPrefLockscreenTargetsVerticalOffset.setEnabled(true);
            mPrefLockscreenTargetsHorizontalOffset.setEnabled(true);

            if (key == null || key.equals(PREF_KEY_NETWORK_MODE_TILE_MODE)) {
                mPrefNetworkModeTileMode.setSummary(mPrefNetworkModeTileMode.getEntry());
            }

            if (key == null || key.equals(PREF_KEY_QS_NETWORK_MODE_SIM_SLOT)) {
                mPrefQsNetworkModeSimSlot.setSummary(
                        String.format(getString(R.string.pref_qs_network_mode_sim_slot_summary),
                                mPrefQsNetworkModeSimSlot.getEntry()));
            }

            if (Utils.isMtkDevice()) {
                final boolean mtkBatteryPercent = Settings.Secure.getInt(getActivity().getContentResolver(), 
                        ModBatteryStyle.SETTING_MTK_BATTERY_PERCENTAGE, 0) == 1;
                if (mtkBatteryPercent) {
                    mPrefs.edit().putBoolean(PREF_KEY_BATTERY_PERCENT_TEXT, false).commit();
                    mPrefBatteryPercent.setChecked(false);
                    Intent intent = new Intent();
                    intent.setAction(ACTION_PREF_BATTERY_STYLE_CHANGED);
                    intent.putExtra("batteryPercent", false);
                    getActivity().sendBroadcast(intent);
                }
                mPrefBatteryPercent.setEnabled(!mtkBatteryPercent);
            }

            if (key == null || key.equals(PREF_KEY_STATUSBAR_CLOCK_DOW)) {
                mPrefSbClockDow.setSummary(mPrefSbClockDow.getEntry());
            }

            if (key == null || key.equals(PREF_KEY_STATUSBAR_LOCK_POLICY)) {
                mPrefSbLockPolicy.setSummary(mPrefSbLockPolicy.getEntry());
            }

            if (key == null || key.equals(PREF_KEY_DATA_TRAFFIC_POSITION)) {
                mPrefDataTrafficPosition.setSummary(mPrefDataTrafficPosition.getEntry());
            }

            if (key == null || key.equals(PREF_KEY_DATA_TRAFFIC_SIZE)) {
                mPrefDataTrafficSize.setSummary(mPrefDataTrafficSize.getEntry());
            }

            if (key == null || key.equals(PREF_KEY_VOLUME_PANEL_EXPANDABLE)) {
                mPrefVolumePanelAutoexpand.setEnabled(mPrefVolumePanelExpandable.isChecked());
                mPrefVolumePanelFullyExpandable.setEnabled(mPrefVolumePanelExpandable.isChecked());
            }

            if (key == null || key.equals(PREF_KEY_STATUSBAR_SIGNAL_COLOR_MODE)) {
                mPrefSbSignalColorMode.setSummary(mPrefSbSignalColorMode.getEntry());
            }

            if (key == null || key.equals(PREF_KEY_PIE_CONTROL_CUSTOM_KEY)) {
                mPrefPieCustomKey.setSummary(mPrefPieCustomKey.getEntry());
            }

            if (key == null || key.equals(PREF_KEY_CALLER_UNKNOWN_PHOTO_ENABLE)) {
                mPrefCallerUnknownPhoto.setEnabled(mPrefCallerUnknownPhotoEnable.isChecked());
            }
        }

        @Override
        public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
            updatePreferences(key);

            Intent intent = new Intent();
            if (key.equals(PREF_KEY_BATTERY_STYLE)) {
                intent.setAction(ACTION_PREF_BATTERY_STYLE_CHANGED);
                int batteryStyle = Integer.valueOf(prefs.getString(PREF_KEY_BATTERY_STYLE, "1"));
                intent.putExtra("batteryStyle", batteryStyle);
            } else if (key.equals(PREF_KEY_BATTERY_PERCENT_TEXT)) {
                intent.setAction(ACTION_PREF_BATTERY_STYLE_CHANGED);
                intent.putExtra("batteryPercent", prefs.getBoolean(PREF_KEY_BATTERY_PERCENT_TEXT, false));
            } else if (key.equals(PREF_KEY_SIGNAL_ICON_AUTOHIDE)) {
                intent.setAction(ACTION_PREF_SIGNAL_ICON_AUTOHIDE_CHANGED);
                String[] autohidePrefs = mSignalIconAutohide.getValues().toArray(new String[0]);
                intent.putExtra("autohidePrefs", autohidePrefs);
            } else if (key.equals(PREF_KEY_QUICK_SETTINGS)) {
                intent.setAction(ACTION_PREF_QUICKSETTINGS_CHANGED);
                intent.putExtra(EXTRA_QS_PREFS, TileOrderActivity.updateTileList(prefs));
            } else if (key.equals(PREF_KEY_QUICK_SETTINGS_TILES_PER_ROW)) {
                intent.setAction(ACTION_PREF_QUICKSETTINGS_CHANGED);
                intent.putExtra(EXTRA_QS_COLS, Integer.valueOf(
                        prefs.getString(PREF_KEY_QUICK_SETTINGS_TILES_PER_ROW, "3")));
            } else if (key.equals(PREF_KEY_QUICK_SETTINGS_AUTOSWITCH)) {
                intent.setAction(ACTION_PREF_QUICKSETTINGS_CHANGED);
                intent.putExtra(EXTRA_QS_AUTOSWITCH,
                        prefs.getBoolean(PREF_KEY_QUICK_SETTINGS_AUTOSWITCH, false));
            } else if (key.equals(PREF_KEY_QUICK_PULLDOWN)) {
                intent.setAction(ACTION_PREF_QUICKSETTINGS_CHANGED);
                intent.putExtra(EXTRA_QUICK_PULLDOWN, Integer.valueOf(
                        prefs.getString(PREF_KEY_QUICK_PULLDOWN, "0")));
            } else if (key.equals(PREF_KEY_STATUSBAR_BGCOLOR)) {
                intent.setAction(ACTION_PREF_STATUSBAR_COLOR_CHANGED);
                intent.putExtra(EXTRA_SB_BG_COLOR, prefs.getInt(PREF_KEY_STATUSBAR_BGCOLOR, Color.BLACK));
            } else if (key.equals(PREF_KEY_STATUSBAR_COLOR_FOLLOW_STOCK_BATTERY)) {
                intent.setAction(ACTION_PREF_STATUSBAR_COLOR_CHANGED);
                intent.putExtra(EXTRA_SB_COLOR_FOLLOW, prefs.getBoolean(
                        PREF_KEY_STATUSBAR_COLOR_FOLLOW_STOCK_BATTERY, false));
            } else if (key.equals(PREF_KEY_STATUSBAR_ICON_COLOR_ENABLE)) {
                intent.setAction(ACTION_PREF_STATUSBAR_COLOR_CHANGED);
                intent.putExtra(EXTRA_SB_ICON_COLOR_ENABLE,
                        prefs.getBoolean(PREF_KEY_STATUSBAR_ICON_COLOR_ENABLE, false));
            } else if (key.equals(PREF_KEY_STATUSBAR_ICON_COLOR)) {
                intent.setAction(ACTION_PREF_STATUSBAR_COLOR_CHANGED);
                intent.putExtra(EXTRA_SB_ICON_COLOR, prefs.getInt(PREF_KEY_STATUSBAR_ICON_COLOR, 
                        getResources().getInteger(R.integer.COLOR_HOLO_BLUE_LIGHT)));
            } else if (key.equals(PREF_KEY_STATUSBAR_DATA_ACTIVITY_COLOR)) {
                intent.setAction(ACTION_PREF_STATUSBAR_COLOR_CHANGED);
                intent.putExtra(EXTRA_SB_DATA_ACTIVITY_COLOR,
                        prefs.getInt(PREF_KEY_STATUSBAR_DATA_ACTIVITY_COLOR, Color.WHITE));
            } else if (key.equals(PREF_KEY_STATUSBAR_COLOR_SKIP_BATTERY)) {
                intent.setAction(ACTION_PREF_STATUSBAR_COLOR_CHANGED);
                intent.putExtra(EXTRA_SB_COLOR_SKIP_BATTERY,
                        prefs.getBoolean(PREF_KEY_STATUSBAR_COLOR_SKIP_BATTERY, false));
            } else if (key.equals(PREF_KEY_STATUSBAR_SIGNAL_COLOR_MODE)) {
                intent.setAction(ACTION_PREF_STATUSBAR_COLOR_CHANGED);
                intent.putExtra(EXTRA_SB_SIGNAL_COLOR_MODE,
                        Integer.valueOf(prefs.getString(PREF_KEY_STATUSBAR_SIGNAL_COLOR_MODE, "0")));
            } else if (key.equals(PREF_KEY_TM_STATUSBAR_LAUNCHER)) {
                intent.setAction(ACTION_PREF_STATUSBAR_COLOR_CHANGED);
                intent.putExtra(EXTRA_TM_SB_LAUNCHER, prefs.getInt(PREF_KEY_TM_STATUSBAR_LAUNCHER, 0));
            } else if (key.equals(PREF_KEY_TM_STATUSBAR_LOCKSCREEN)) {
                intent.setAction(ACTION_PREF_STATUSBAR_COLOR_CHANGED);
                intent.putExtra(EXTRA_TM_SB_LOCKSCREEN, prefs.getInt(PREF_KEY_TM_STATUSBAR_LOCKSCREEN, 0));
            } else if (key.equals(PREF_KEY_TM_NAVBAR_LAUNCHER)) {
                intent.setAction(ACTION_PREF_STATUSBAR_COLOR_CHANGED);
                intent.putExtra(EXTRA_TM_NB_LAUNCHER, prefs.getInt(PREF_KEY_TM_NAVBAR_LAUNCHER, 0));
            } else if (key.equals(PREF_KEY_TM_NAVBAR_LOCKSCREEN)) {
                intent.setAction(ACTION_PREF_STATUSBAR_COLOR_CHANGED);
                intent.putExtra(EXTRA_TM_NB_LOCKSCREEN, prefs.getInt(PREF_KEY_TM_NAVBAR_LOCKSCREEN, 0));
            } else if (key.equals(PREF_KEY_STATUSBAR_CENTER_CLOCK)) {
                intent.setAction(ACTION_PREF_CLOCK_CHANGED);
                intent.putExtra(EXTRA_CENTER_CLOCK, 
                        prefs.getBoolean(PREF_KEY_STATUSBAR_CENTER_CLOCK, false));
            } else if (key.equals(PREF_KEY_STATUSBAR_CLOCK_DOW)) {
                intent.setAction(ACTION_PREF_CLOCK_CHANGED);
                intent.putExtra(EXTRA_CLOCK_DOW, Integer.valueOf(
                        prefs.getString(PREF_KEY_STATUSBAR_CLOCK_DOW, "0")));
            } else if (key.equals(PREF_KEY_STATUSBAR_CLOCK_AMPM_HIDE)) {
                intent.setAction(ACTION_PREF_CLOCK_CHANGED);
                intent.putExtra(EXTRA_AMPM_HIDE, prefs.getBoolean(
                        PREF_KEY_STATUSBAR_CLOCK_AMPM_HIDE, false));
            } else if (key.equals(PREF_KEY_STATUSBAR_CLOCK_HIDE)) {
                intent.setAction(ACTION_PREF_CLOCK_CHANGED);
                intent.putExtra(EXTRA_CLOCK_HIDE, prefs.getBoolean(PREF_KEY_STATUSBAR_CLOCK_HIDE, false));
            } else if (key.equals(PREF_KEY_STATUSBAR_CLOCK_LINK)) {
                intent.setAction(ACTION_PREF_CLOCK_CHANGED);
                intent.putExtra(EXTRA_CLOCK_LINK, prefs.getString(PREF_KEY_STATUSBAR_CLOCK_LINK, null));
            } else if (key.equals(PREF_KEY_ALARM_ICON_HIDE)) {
                intent.setAction(ACTION_PREF_CLOCK_CHANGED);
                intent.putExtra(EXTRA_ALARM_HIDE, prefs.getBoolean(PREF_KEY_ALARM_ICON_HIDE, false));
            } else if (key.equals(PREF_KEY_SAFE_MEDIA_VOLUME)) {
                intent.setAction(ACTION_PREF_SAFE_MEDIA_VOLUME_CHANGED);
                intent.putExtra(EXTRA_SAFE_MEDIA_VOLUME_ENABLED,
                        prefs.getBoolean(PREF_KEY_SAFE_MEDIA_VOLUME, false));
            } else if (key.equals(PREF_KEY_HWKEY_MENU_LONGPRESS)) {
                intent.setAction(ACTION_PREF_HWKEY_MENU_LONGPRESS_CHANGED);
                intent.putExtra(EXTRA_HWKEY_VALUE, Integer.valueOf(
                        prefs.getString(PREF_KEY_HWKEY_MENU_LONGPRESS, "0")));
            } else if (key.equals(PREF_KEY_HWKEY_MENU_DOUBLETAP)) {
                intent.setAction(ACTION_PREF_HWKEY_MENU_DOUBLETAP_CHANGED);
                intent.putExtra(EXTRA_HWKEY_VALUE, Integer.valueOf(
                        prefs.getString(PREF_KEY_HWKEY_MENU_DOUBLETAP, "0")));
            } else if (key.equals(PREF_KEY_HWKEY_HOME_LONGPRESS)) {
                intent.setAction(ACTION_PREF_HWKEY_HOME_LONGPRESS_CHANGED);
                intent.putExtra(EXTRA_HWKEY_VALUE, Integer.valueOf(
                        prefs.getString(PREF_KEY_HWKEY_HOME_LONGPRESS, "0")));
            } else if (key.equals(PREF_KEY_HWKEY_HOME_LONGPRESS_KEYGUARD)) {
                intent.setAction(ACTION_PREF_HWKEY_HOME_LONGPRESS_CHANGED);
                intent.putExtra(EXTRA_HWKEY_HOME_LONGPRESS_KG, prefs.getBoolean(
                        GravityBoxSettings.PREF_KEY_HWKEY_HOME_LONGPRESS_KEYGUARD, false));
            } else if (key.equals(PREF_KEY_HWKEY_HOME_DOUBLETAP_DISABLE)) {
                intent.setAction(ACTION_PREF_HWKEY_HOME_DOUBLETAP_CHANGED);
                intent.putExtra(EXTRA_HWKEY_HOME_DOUBLETAP_DISABLE,
                        prefs.getBoolean(PREF_KEY_HWKEY_HOME_DOUBLETAP_DISABLE, false));
            } else if (key.equals(PREF_KEY_HWKEY_BACK_LONGPRESS)) {
                intent.setAction(ACTION_PREF_HWKEY_BACK_LONGPRESS_CHANGED);
                intent.putExtra(EXTRA_HWKEY_VALUE, Integer.valueOf(
                        prefs.getString(PREF_KEY_HWKEY_BACK_LONGPRESS, "0")));
            } else if (key.equals(PREF_KEY_HWKEY_RECENTS_SINGLETAP)) {
                intent.setAction(ACTION_PREF_HWKEY_RECENTS_SINGLETAP_CHANGED);
                intent.putExtra(EXTRA_HWKEY_VALUE, Integer.valueOf(
                        prefs.getString(PREF_KEY_HWKEY_RECENTS_SINGLETAP, "0")));
            } else if (key.equals(PREF_KEY_HWKEY_RECENTS_LONGPRESS)) {
                intent.setAction(ACTION_PREF_HWKEY_RECENTS_LONGPRESS_CHANGED);
                intent.putExtra(EXTRA_HWKEY_VALUE, Integer.valueOf(
                        prefs.getString(PREF_KEY_HWKEY_RECENTS_LONGPRESS, "0")));
            } else if (key.equals(PREF_KEY_HWKEY_DOUBLETAP_SPEED)) {
                intent.setAction(ACTION_PREF_HWKEY_DOUBLETAP_SPEED_CHANGED);
                intent.putExtra(EXTRA_HWKEY_VALUE, Integer.valueOf(
                        prefs.getString(PREF_KEY_HWKEY_DOUBLETAP_SPEED, "400")));
            } else if (key.equals(PREF_KEY_HWKEY_KILL_DELAY)) {
                intent.setAction(ACTION_PREF_HWKEY_KILL_DELAY_CHANGED);
                intent.putExtra(EXTRA_HWKEY_VALUE, Integer.valueOf(
                        prefs.getString(PREF_KEY_HWKEY_KILL_DELAY, "1000")));
            } else if (key.equals(PREF_KEY_VOLUME_ROCKER_WAKE_DISABLE)) {
                intent.setAction(ACTION_PREF_VOLUME_ROCKER_WAKE_CHANGED);
                intent.putExtra(EXTRA_VOLUME_ROCKER_WAKE_DISABLE,
                        prefs.getBoolean(PREF_KEY_VOLUME_ROCKER_WAKE_DISABLE, false));
            } else if (key.equals(PREF_KEY_VOLUME_PANEL_EXPANDABLE)) {
                intent.setAction(ACTION_PREF_VOLUME_PANEL_MODE_CHANGED);
                intent.putExtra(EXTRA_EXPANDABLE,
                        prefs.getBoolean(PREF_KEY_VOLUME_PANEL_EXPANDABLE, false));
            } else if (key.equals(PREF_KEY_VOLUME_PANEL_FULLY_EXPANDABLE)) {
                intent.setAction(ACTION_PREF_VOLUME_PANEL_MODE_CHANGED);
                intent.putExtra(EXTRA_EXPANDABLE_FULLY,
                        prefs.getBoolean(PREF_KEY_VOLUME_PANEL_FULLY_EXPANDABLE, false));
            } else if (key.equals(PREF_KEY_VOLUME_PANEL_AUTOEXPAND)) {
                intent.setAction(ACTION_PREF_VOLUME_PANEL_MODE_CHANGED);
                intent.putExtra(EXTRA_AUTOEXPAND, 
                        prefs.getBoolean(PREF_KEY_VOLUME_PANEL_AUTOEXPAND, false));
            } else if (key.equals(PREF_KEY_VOLUME_ADJUST_MUTE)) {
                intent.setAction(ACTION_PREF_VOLUME_PANEL_MODE_CHANGED);
                intent.putExtra(EXTRA_MUTED, prefs.getBoolean(PREF_KEY_VOLUME_ADJUST_MUTE, false));
            } else if (key.equals(PREF_KEY_LINK_VOLUMES)) {
                intent.setAction(ACTION_PREF_LINK_VOLUMES_CHANGED);
                intent.putExtra(EXTRA_LINKED,
                        prefs.getBoolean(PREF_KEY_LINK_VOLUMES, true));
            } else if (key.equals(PREF_KEY_NOTIF_BACKGROUND)) {
                intent.setAction(ACTION_NOTIF_BACKGROUND_CHANGED);
                intent.putExtra(EXTRA_BG_TYPE, prefs.getString(
                        PREF_KEY_NOTIF_BACKGROUND, NOTIF_BG_DEFAULT));
            } else if (key.equals(PREF_KEY_NOTIF_COLOR)) {
                intent.setAction(ACTION_NOTIF_BACKGROUND_CHANGED);
                intent.putExtra(EXTRA_BG_COLOR, prefs.getInt(PREF_KEY_NOTIF_COLOR, Color.BLACK));
            } else if (key.equals(PREF_KEY_NOTIF_COLOR_MODE)) {
                intent.setAction(ACTION_NOTIF_BACKGROUND_CHANGED);
                intent.putExtra(EXTRA_BG_COLOR_MODE, prefs.getString(
                        PREF_KEY_NOTIF_COLOR_MODE, NOTIF_BG_COLOR_MODE_OVERLAY));
            } else if (key.equals(PREF_KEY_NOTIF_BACKGROUND_ALPHA)) {
                intent.setAction(ACTION_NOTIF_BACKGROUND_CHANGED);
                intent.putExtra(EXTRA_BG_ALPHA, prefs.getInt(PREF_KEY_NOTIF_BACKGROUND_ALPHA, 60));
            } else if (key.equals(PREF_KEY_DISABLE_ROAMING_INDICATORS)) {
                intent.setAction(ACTION_DISABLE_ROAMING_INDICATORS_CHANGED);
                intent.putExtra(EXTRA_INDICATORS_DISABLED,
                        prefs.getBoolean(PREF_KEY_DISABLE_ROAMING_INDICATORS, false));
            } else if (key.equals(PREF_KEY_PIE_CONTROL_ENABLE)) {
                intent.setAction(ACTION_PREF_PIE_CHANGED);
                int mode = Integer.valueOf(prefs.getString(PREF_KEY_PIE_CONTROL_ENABLE, "0"));
                intent.putExtra(EXTRA_PIE_ENABLE, mode);
                if (mode == 0) {
                    intent.putExtra(EXTRA_PIE_HWKEYS_DISABLE, false);
                }
            } else if (key.equals(PREF_KEY_PIE_CONTROL_CUSTOM_KEY)) {
                intent.setAction(ACTION_PREF_PIE_CHANGED);
                intent.putExtra(EXTRA_PIE_CUSTOM_KEY_MODE, Integer.valueOf( 
                        prefs.getString(PREF_KEY_PIE_CONTROL_CUSTOM_KEY, "0")));
            } else if (key.equals(PREF_KEY_PIE_CONTROL_MENU)) {
                intent.setAction(ACTION_PREF_PIE_CHANGED);
                intent.putExtra(EXTRA_PIE_MENU, prefs.getBoolean(PREF_KEY_PIE_CONTROL_MENU, false));
            } else if (key.equals(PREF_KEY_PIE_CONTROL_TRIGGERS)) {
                intent.setAction(ACTION_PREF_PIE_CHANGED);
                String[] triggers = prefs.getStringSet(
                        PREF_KEY_PIE_CONTROL_TRIGGERS, new HashSet<String>()).toArray(new String[0]);
                intent.putExtra(EXTRA_PIE_TRIGGERS, triggers);
            } else if (key.equals(PREF_KEY_PIE_CONTROL_TRIGGER_SIZE)) {
                intent.setAction(ACTION_PREF_PIE_CHANGED);
                intent.putExtra(EXTRA_PIE_TRIGGER_SIZE, 
                        prefs.getInt(PREF_KEY_PIE_CONTROL_TRIGGER_SIZE, 5));
            } else if (key.equals(PREF_KEY_PIE_CONTROL_SIZE)) {
                intent.setAction(ACTION_PREF_PIE_CHANGED);
                intent.putExtra(EXTRA_PIE_SIZE, prefs.getInt(PREF_KEY_PIE_CONTROL_SIZE, 1000));
            } else if (key.equals(PREF_KEY_HWKEYS_DISABLE)) {
                intent.setAction(ACTION_PREF_PIE_CHANGED);
                intent.putExtra(EXTRA_PIE_HWKEYS_DISABLE, prefs.getBoolean(PREF_KEY_HWKEYS_DISABLE, false));
            } else if (key.equals(PREF_KEY_PIE_COLOR_BG)) {
                intent.setAction(ACTION_PREF_PIE_CHANGED);
                intent.putExtra(EXTRA_PIE_COLOR_BG, prefs.getInt(PREF_KEY_PIE_COLOR_BG, 
                        getResources().getColor(R.color.pie_background_color)));
            } else if (key.equals(PREF_KEY_PIE_COLOR_FG)) {
                intent.setAction(ACTION_PREF_PIE_CHANGED);
                intent.putExtra(EXTRA_PIE_COLOR_FG, prefs.getInt(PREF_KEY_PIE_COLOR_FG, 
                        getResources().getColor(R.color.pie_foreground_color)));
            } else if (key.equals(PREF_KEY_PIE_COLOR_OUTLINE)) {
                intent.setAction(ACTION_PREF_PIE_CHANGED);
                intent.putExtra(EXTRA_PIE_COLOR_OUTLINE, prefs.getInt(PREF_KEY_PIE_COLOR_OUTLINE, 
                        getResources().getColor(R.color.pie_outline_color)));
            } else if (key.equals(PREF_KEY_PIE_COLOR_SELECTED)) {
                intent.setAction(ACTION_PREF_PIE_CHANGED);
                intent.putExtra(EXTRA_PIE_COLOR_SELECTED, prefs.getInt(PREF_KEY_PIE_COLOR_SELECTED, 
                        getResources().getColor(R.color.pie_selected_color)));
            } else if (key.equals(PREF_KEY_PIE_COLOR_TEXT)) {
                intent.setAction(ACTION_PREF_PIE_CHANGED);
                intent.putExtra(EXTRA_PIE_COLOR_TEXT, prefs.getInt(PREF_KEY_PIE_COLOR_TEXT, 
                        getResources().getColor(R.color.pie_text_color)));
            } else if (key.equals(PREF_KEY_BUTTON_BACKLIGHT_MODE)) {
                intent.setAction(ACTION_PREF_BUTTON_BACKLIGHT_CHANGED);
                intent.putExtra(EXTRA_BB_MODE, prefs.getString(
                        PREF_KEY_BUTTON_BACKLIGHT_MODE, BB_MODE_DEFAULT));
            } else if (key.equals(PREF_KEY_BUTTON_BACKLIGHT_NOTIFICATIONS)) {
                intent.setAction(ACTION_PREF_BUTTON_BACKLIGHT_CHANGED);
                intent.putExtra(EXTRA_BB_NOTIF, prefs.getBoolean(
                        PREF_KEY_BUTTON_BACKLIGHT_NOTIFICATIONS, false));
            } else if (key.equals(PREF_KEY_QUICKAPP_DEFAULT)) {
                intent.setAction(ACTION_PREF_QUICKAPP_CHANGED);
                intent.putExtra(EXTRA_QUICKAPP_DEFAULT, prefs.getString(PREF_KEY_QUICKAPP_DEFAULT, null));
            } else if (key.equals(PREF_KEY_QUICKAPP_SLOT1)) {
                intent.setAction(ACTION_PREF_QUICKAPP_CHANGED);
                intent.putExtra(EXTRA_QUICKAPP_SLOT1, prefs.getString(PREF_KEY_QUICKAPP_SLOT1, null));
            } else if (key.equals(PREF_KEY_QUICKAPP_SLOT2)) {
                intent.setAction(ACTION_PREF_QUICKAPP_CHANGED);
                intent.putExtra(EXTRA_QUICKAPP_SLOT2, prefs.getString(PREF_KEY_QUICKAPP_SLOT2, null));
            } else if (key.equals(PREF_KEY_QUICKAPP_SLOT3)) {
                intent.setAction(ACTION_PREF_QUICKAPP_CHANGED);
                intent.putExtra(EXTRA_QUICKAPP_SLOT3, prefs.getString(PREF_KEY_QUICKAPP_SLOT3, null));
            } else if (key.equals(PREF_KEY_QUICKAPP_SLOT4)) {
                intent.setAction(ACTION_PREF_QUICKAPP_CHANGED);
                intent.putExtra(EXTRA_QUICKAPP_SLOT4, prefs.getString(PREF_KEY_QUICKAPP_SLOT4, null));
            } else if (key.equals(PREF_KEY_EXPANDED_DESKTOP)) {
                intent.setAction(ACTION_PREF_EXPANDED_DESKTOP_MODE_CHANGED);
                intent.putExtra(EXTRA_ED_MODE, Integer.valueOf(
                        prefs.getString(PREF_KEY_EXPANDED_DESKTOP, "0")));
            } else if (key.equals(PREF_KEY_NAVBAR_HEIGHT)) {
                intent.setAction(ACTION_PREF_NAVBAR_CHANGED);
                intent.putExtra(EXTRA_NAVBAR_HEIGHT, prefs.getInt(PREF_KEY_NAVBAR_HEIGHT, 100));
            } else if (key.equals(PREF_KEY_NAVBAR_HEIGHT_LANDSCAPE)) {
                intent.setAction(ACTION_PREF_NAVBAR_CHANGED);
                intent.putExtra(EXTRA_NAVBAR_HEIGHT_LANDSCAPE, 
                        prefs.getInt(PREF_KEY_NAVBAR_HEIGHT_LANDSCAPE, 100));
            } else if (key.equals(PREF_KEY_NAVBAR_WIDTH)) {
                intent.setAction(ACTION_PREF_NAVBAR_CHANGED);
                intent.putExtra(EXTRA_NAVBAR_WIDTH, prefs.getInt(PREF_KEY_NAVBAR_WIDTH, 100));
            } else if (key.equals(PREF_KEY_NAVBAR_MENUKEY)) {
                intent.setAction(ACTION_PREF_NAVBAR_CHANGED);
                intent.putExtra(EXTRA_NAVBAR_MENUKEY, prefs.getBoolean(PREF_KEY_NAVBAR_MENUKEY, false));
            } else if (key.equals(PREF_KEY_NAVBAR_LAUNCHER_ENABLE)) {
                intent.setAction(ACTION_PREF_NAVBAR_CHANGED);
                intent.putExtra(EXTRA_NAVBAR_LAUNCHER_ENABLE,
                        prefs.getBoolean(PREF_KEY_NAVBAR_LAUNCHER_ENABLE, false));
            } else if (key.equals(PREF_KEY_NAVBAR_KEY_COLOR)) {
                intent.setAction(ACTION_PREF_NAVBAR_CHANGED);
                intent.putExtra(EXTRA_NAVBAR_KEY_COLOR,
                        prefs.getInt(PREF_KEY_NAVBAR_KEY_COLOR, 
                                getResources().getColor(R.color.navbar_key_color)));
            } else if (key.equals(PREF_KEY_NAVBAR_KEY_GLOW_COLOR)) {
                intent.setAction(ACTION_PREF_NAVBAR_CHANGED);
                intent.putExtra(EXTRA_NAVBAR_KEY_GLOW_COLOR,
                        prefs.getInt(PREF_KEY_NAVBAR_KEY_GLOW_COLOR, 
                                getResources().getColor(R.color.navbar_key_glow_color)));
            } else if (key.equals(PREF_KEY_NAVBAR_BG_COLOR)) {
                intent.setAction(ACTION_PREF_NAVBAR_CHANGED);
                intent.putExtra(EXTRA_NAVBAR_BG_COLOR,
                        prefs.getInt(PREF_KEY_NAVBAR_BG_COLOR, 
                                getResources().getColor(R.color.navbar_bg_color)));
            } else if (PREF_KEY_APP_LAUNCHER_SLOT.contains(key)) {
                intent.setAction(ACTION_PREF_APP_LAUNCHER_CHANGED);
                intent.putExtra(EXTRA_APP_LAUNCHER_SLOT,
                        PREF_KEY_APP_LAUNCHER_SLOT.indexOf(key));
                intent.putExtra(EXTRA_APP_LAUNCHER_APP, prefs.getString(key, null));
            } else if (key.equals(PREF_KEY_STATUSBAR_BRIGHTNESS)) {
                intent.setAction(ACTION_PREF_STATUSBAR_BRIGHTNESS_CHANGED);
                intent.putExtra(EXTRA_SB_BRIGHTNESS, prefs.getBoolean(PREF_KEY_STATUSBAR_BRIGHTNESS, false));
            } else if (key.equals(PREF_KEY_NETWORK_MODE_TILE_MODE)) {
                intent.setAction(ACTION_PREF_QUICKSETTINGS_CHANGED);
                intent.putExtra(EXTRA_NMT_MODE, Integer.valueOf(
                        prefs.getString(PREF_KEY_NETWORK_MODE_TILE_MODE, "0")));
            } else if (key.equals(PREF_KEY_DISPLAY_ALLOW_ALL_ROTATIONS)) {
                intent.setAction(ACTION_PREF_DISPLAY_ALLOW_ALL_ROTATIONS_CHANGED);
                intent.putExtra(EXTRA_ALLOW_ALL_ROTATIONS, 
                        prefs.getBoolean(PREF_KEY_DISPLAY_ALLOW_ALL_ROTATIONS, false));
            } else if (key.equals(PREF_KEY_QS_NETWORK_MODE_SIM_SLOT)) {
                intent.setAction(ACTION_PREF_QS_NETWORK_MODE_SIM_SLOT_CHANGED);
                intent.putExtra(EXTRA_SIM_SLOT, Integer.valueOf(
                        prefs.getString(PREF_KEY_QS_NETWORK_MODE_SIM_SLOT, "0")));
            } else if (key.equals(PREF_KEY_DATA_TRAFFIC_ENABLE)) {
                intent.setAction(ACTION_PREF_DATA_TRAFFIC_CHANGED);
                intent.putExtra(EXTRA_DT_ENABLE, prefs.getBoolean(PREF_KEY_DATA_TRAFFIC_ENABLE, false));
            } else if (key.equals(PREF_KEY_DATA_TRAFFIC_POSITION)) {
                intent.setAction(ACTION_PREF_DATA_TRAFFIC_CHANGED);
                intent.putExtra(EXTRA_DT_POSITION, Integer.valueOf(
                        prefs.getString(PREF_KEY_DATA_TRAFFIC_POSITION, "0")));
            } else if (key.equals(PREF_KEY_DATA_TRAFFIC_SIZE)) {
                intent.setAction(ACTION_PREF_DATA_TRAFFIC_CHANGED);
                intent.putExtra(EXTRA_DT_SIZE, Integer.valueOf(
                        prefs.getString(PREF_KEY_DATA_TRAFFIC_SIZE, "14")));
            }
            if (intent.getAction() != null) {
                getActivity().sendBroadcast(intent);
            }

            if (key.equals(PREF_KEY_FIX_CALLER_ID_PHONE) ||
                    key.equals(PREF_KEY_FIX_CALLER_ID_MMS)) {
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setTitle(R.string.important);
                int msgId = (key.equals(PREF_KEY_FIX_CALLER_ID_PHONE)) ?
                        R.string.fix_caller_id_phone_alert : R.string.fix_caller_id_mms_alert;
                builder.setMessage(msgId);
                builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        Toast.makeText(getActivity(), getString(R.string.reboot_required), Toast.LENGTH_SHORT).show();
                    }
                });
                mDialog = builder.create();
                mDialog.show();
            }

            if (key.equals(PREF_KEY_BRIGHTNESS_MIN) &&
                    prefs.getInt(PREF_KEY_BRIGHTNESS_MIN, 20) < 20) {
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setTitle(R.string.important);
                builder.setMessage(R.string.screen_brightness_min_warning);
                builder.setPositiveButton(android.R.string.ok, null);
                mDialog = builder.create();
                mDialog.show();
            }

            if (rebootKeys.contains(key))
                Toast.makeText(getActivity(), getString(R.string.reboot_required), Toast.LENGTH_SHORT).show();
        }

        @Override
        public boolean onPreferenceTreeClick(PreferenceScreen prefScreen, Preference pref) {
            Intent intent = null;

            if (pref == mPrefAboutGb) {
                intent = new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.url_gravitybox)));
            } else if (pref == mPrefAboutXposed) {
                intent = new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.url_xposed)));
            } else if (pref == mPrefAboutDonate) {
                intent = new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.url_donate)));
            } else if (pref == mPrefEngMode) {
                intent = new Intent(Intent.ACTION_MAIN);
                intent.setClassName(APP_ENGINEERING_MODE, APP_ENGINEERING_MODE_CLASS);
            } else if (pref == mPrefDualSimRinger) {
                intent = new Intent(Intent.ACTION_MAIN);
                intent.setClassName(APP_DUAL_SIM_RINGER, APP_DUAL_SIM_RINGER_CLASS);
            } else if (pref == mPrefLockscreenBgImage) {
                setCustomLockscreenImage();
                return true;
            } else if (pref == mPrefNotifImagePortrait) {
                setCustomNotifBgPortrait();
                return true;
            } else if (pref == mPrefNotifImageLandscape) {
                setCustomNotifBgLandscape();
                return true;
            } else if (pref == mPrefGbThemeDark) {
                File file = new File(getActivity().getFilesDir() + "/" + FILE_THEME_DARK_FLAG);
                if (mPrefGbThemeDark.isChecked()) {
                    if (!file.exists()) {
                        try {
                            file.createNewFile();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                } else {
                    if (file.exists()) {
                        file.delete();
                    }
                }
                getActivity().recreate();
            } else if (pref == mPrefQsTileOrder) {
                intent = new Intent(getActivity(), TileOrderActivity.class);
            } else if (pref == mPrefPieColorReset) {
                final Resources res = getResources();
                final int bgColor = res.getColor(R.color.pie_background_color);
                final int fgColor = res.getColor(R.color.pie_foreground_color);
                final int outlineColor = res.getColor(R.color.pie_outline_color);
                final int selectedColor = res.getColor(R.color.pie_selected_color);
                final int textColor = res.getColor(R.color.pie_text_color);
                mPrefPieColorBg.setValue(bgColor);
                mPrefPieColorFg.setValue(fgColor);
                mPrefPieColorOutline.setValue(outlineColor);
                mPrefPieColorSelected.setValue(selectedColor);
                mPrefPieColorText.setValue(textColor);
                Intent pieIntent = new Intent(ACTION_PREF_PIE_CHANGED);
                pieIntent.putExtra(EXTRA_PIE_COLOR_BG, bgColor);
                pieIntent.putExtra(EXTRA_PIE_COLOR_FG, fgColor);
                pieIntent.putExtra(EXTRA_PIE_COLOR_OUTLINE, outlineColor);
                pieIntent.putExtra(EXTRA_PIE_COLOR_SELECTED, selectedColor);
                pieIntent.putExtra(EXTRA_PIE_COLOR_TEXT, textColor);
                getActivity().sendBroadcast(pieIntent);
            } else if (pref == mPrefCallerUnknownPhoto) {
                setCustomCallerImage();
                return true;
            }

            if (intent != null) {
                try {
                    startActivity(intent);
                } catch (ActivityNotFoundException e) {
                    e.printStackTrace();
                }
                return true;
            }

            return super.onPreferenceTreeClick(prefScreen, pref);
        }

        private boolean isAppInstalled(String appUri) {
            PackageManager pm = getActivity().getPackageManager();
            try {
                pm.getPackageInfo(appUri, PackageManager.GET_ACTIVITIES);
                return true;
            } catch (Exception e) {
                return false;
            }
        }

        @SuppressWarnings("deprecation")
        private void setCustomLockscreenImage() {
            Intent intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            intent.setType("image/*");
            intent.putExtra("crop", "true");
            intent.putExtra("scale", true);
            intent.putExtra("scaleUpIfNeeded", false);
            intent.putExtra("outputFormat", Bitmap.CompressFormat.PNG.toString());
            Display display = getActivity().getWindowManager().getDefaultDisplay();
            int width = display.getWidth();
            int height = display.getHeight();
            Rect rect = new Rect();
            Window window = getActivity().getWindow();
            window.getDecorView().getWindowVisibleDisplayFrame(rect);
            int statusBarHeight = rect.top;
            int contentViewTop = window.findViewById(Window.ID_ANDROID_CONTENT).getTop();
            int titleBarHeight = contentViewTop - statusBarHeight;
            // Lock screen for tablets visible section are different in landscape/portrait,
            // image need to be cropped correctly, like wallpaper setup for scrolling in background in home screen
            // other wise it does not scale correctly
            if (Utils.isTabletUI(getActivity())) {
                width = getActivity().getWallpaperDesiredMinimumWidth();
                height = getActivity().getWallpaperDesiredMinimumHeight();
                float spotlightX = (float) display.getWidth() / width;
                float spotlightY = (float) display.getHeight() / height;
                intent.putExtra("aspectX", width);
                intent.putExtra("aspectY", height);
                intent.putExtra("outputX", width);
                intent.putExtra("outputY", height);
                intent.putExtra("spotlightX", spotlightX);
                intent.putExtra("spotlightY", spotlightY);
            } else {
                boolean isPortrait = getResources().getConfiguration().orientation ==
                    Configuration.ORIENTATION_PORTRAIT;
                intent.putExtra("aspectX", isPortrait ? width : height - titleBarHeight);
                intent.putExtra("aspectY", isPortrait ? height - titleBarHeight : width);
            }
            try {
                wallpaperTemporary.createNewFile();
                wallpaperTemporary.setWritable(true, false);
                intent.putExtra(MediaStore.EXTRA_OUTPUT,Uri.fromFile(wallpaperTemporary));
                intent.putExtra("return-data", false);
                getActivity().startActivityFromFragment(this, intent, REQ_LOCKSCREEN_BACKGROUND);
            } catch (Exception e) {
                Toast.makeText(getActivity(), getString(
                        R.string.lockscreen_background_result_not_successful),
                        Toast.LENGTH_SHORT).show();
                e.printStackTrace();
            }
        }

        @SuppressWarnings("deprecation")
        private void setCustomNotifBgPortrait() {
            Display display = getActivity().getWindowManager().getDefaultDisplay();
            int width = display.getWidth();
            int height = display.getHeight();
            Rect rect = new Rect();
            Window window = getActivity().getWindow();
            window.getDecorView().getWindowVisibleDisplayFrame(rect);
            int statusBarHeight = rect.top;
            int contentViewTop = window.findViewById(Window.ID_ANDROID_CONTENT).getTop();
            int titleBarHeight = contentViewTop - statusBarHeight;
            Intent intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            intent.setType("image/*");
            intent.putExtra("crop", "true");
            boolean isPortrait = getResources()
                    .getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT;
            intent.putExtra("aspectX", isPortrait ? width : height - titleBarHeight);
            intent.putExtra("aspectY", isPortrait ? height - titleBarHeight : width);
            intent.putExtra("outputX", isPortrait ? width : height);
            intent.putExtra("outputY", isPortrait ? height : width);
            intent.putExtra("scale", true);
            intent.putExtra("scaleUpIfNeeded", true);
            intent.putExtra("outputFormat", Bitmap.CompressFormat.PNG.toString());
            try {
                wallpaperTemporary.createNewFile();
                wallpaperTemporary.setWritable(true, false);
                intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(wallpaperTemporary));
                startActivityForResult(intent, REQ_NOTIF_BG_IMAGE_PORTRAIT);
            } catch (Exception e) {
                Toast.makeText(getActivity(), getString(
                        R.string.lockscreen_background_result_not_successful),
                        Toast.LENGTH_SHORT).show();
                e.printStackTrace();
            }
        }

        @SuppressWarnings("deprecation")
        private void setCustomNotifBgLandscape() {
            Display display = getActivity().getWindowManager().getDefaultDisplay();
            int width = display.getWidth();
            int height = display.getHeight();
            Rect rect = new Rect();
            Window window = getActivity().getWindow();
            window.getDecorView().getWindowVisibleDisplayFrame(rect);
            int statusBarHeight = rect.top;
            int contentViewTop = window.findViewById(Window.ID_ANDROID_CONTENT).getTop();
            int titleBarHeight = contentViewTop - statusBarHeight;
            Intent intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            intent.setType("image/*");
            intent.putExtra("crop", "true");
            boolean isPortrait = getResources()
                  .getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT;
            intent.putExtra("aspectX", isPortrait ? height - titleBarHeight : width);
            intent.putExtra("aspectY", isPortrait ? width : height - titleBarHeight);
            intent.putExtra("outputX", isPortrait ? height : width);
            intent.putExtra("outputY", isPortrait ? width : height);
            intent.putExtra("scale", true);
            intent.putExtra("scaleUpIfNeeded", true);
            intent.putExtra("outputFormat", Bitmap.CompressFormat.PNG.toString());
            try {
                wallpaperTemporary.createNewFile();
                wallpaperTemporary.setWritable(true, false);
                intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(wallpaperTemporary));
                startActivityForResult(intent, REQ_NOTIF_BG_IMAGE_LANDSCAPE);
            } catch (Exception e) {
                Toast.makeText(getActivity(), getString(
                        R.string.lockscreen_background_result_not_successful),
                        Toast.LENGTH_SHORT).show();
                e.printStackTrace();
            }
        }

        private void setCustomCallerImage() {
            int width = getResources().getDimensionPixelSize(R.dimen.caller_id_photo_width);
            int height = getResources().getDimensionPixelSize(R.dimen.caller_id_photo_height);
            Intent intent = new Intent(Intent.ACTION_PICK,
                    android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            intent.setType("image/*");
            intent.putExtra("crop", "true");
            boolean isPortrait = getResources()
                    .getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT;
            intent.putExtra("aspectX", isPortrait ? width : height);
            intent.putExtra("aspectY", isPortrait ? height : width);
            intent.putExtra("outputX", isPortrait ? width : height);
            intent.putExtra("outputY", isPortrait ? height : width);
            intent.putExtra("scale", true);
            intent.putExtra("scaleUpIfNeeded", true);
            intent.putExtra("outputFormat", Bitmap.CompressFormat.PNG.toString());
            try {
                wallpaperTemporary.createNewFile();
                wallpaperTemporary.setWritable(true, false);
                intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(wallpaperTemporary));
                startActivityForResult(intent, REQ_CALLER_PHOTO);
            } catch (Exception e) {
                Toast.makeText(getActivity(), getString(
                        R.string.caller_unkown_photo_result_not_successful),
                        Toast.LENGTH_SHORT).show();
                e.printStackTrace();
            }
        }

        @Override
        public void onActivityResult(int requestCode, int resultCode, Intent data) {
            if (requestCode == REQ_LOCKSCREEN_BACKGROUND) {
                if (resultCode == Activity.RESULT_OK) {
                    if (wallpaperTemporary.exists()) {
                        wallpaperTemporary.renameTo(wallpaperImage);
                    }
                    wallpaperImage.setReadable(true, false);
                    Toast.makeText(getActivity(), getString(
                            R.string.lockscreen_background_result_successful), 
                            Toast.LENGTH_SHORT).show();
                } else {
                    if (wallpaperTemporary.exists()) {
                        wallpaperTemporary.delete();
                    }
                    Toast.makeText(getActivity(), getString(
                            R.string.lockscreen_background_result_not_successful),
                            Toast.LENGTH_SHORT).show();
                }
            } else if (requestCode == REQ_NOTIF_BG_IMAGE_PORTRAIT) {
                if (resultCode == Activity.RESULT_OK) {
                    if (wallpaperTemporary.exists()) {
                        wallpaperTemporary.renameTo(notifBgImagePortrait);
                    }
                    notifBgImagePortrait.setReadable(true, false);
                    Toast.makeText(getActivity(), getString(
                            R.string.lockscreen_background_result_successful), 
                            Toast.LENGTH_SHORT).show();
                } else {
                    if (wallpaperTemporary.exists()) {
                        wallpaperTemporary.delete();
                    }
                    Toast.makeText(getActivity(), getString(
                            R.string.lockscreen_background_result_not_successful),
                            Toast.LENGTH_SHORT).show();
                }
                Intent intent = new Intent(ACTION_NOTIF_BACKGROUND_CHANGED);
                getActivity().sendBroadcast(intent);
            } else if (requestCode == REQ_NOTIF_BG_IMAGE_LANDSCAPE) {
                if (resultCode == Activity.RESULT_OK) {
                    if (wallpaperTemporary.exists()) {
                        wallpaperTemporary.renameTo(notifBgImageLandscape);
                    }
                    notifBgImageLandscape.setReadable(true, false);
                    Toast.makeText(getActivity(), getString(
                            R.string.lockscreen_background_result_successful), 
                            Toast.LENGTH_SHORT).show();
                } else {
                    if (wallpaperTemporary.exists()) {
                        wallpaperTemporary.delete();
                    }
                    Toast.makeText(getActivity(), getString(
                            R.string.lockscreen_background_result_not_successful),
                            Toast.LENGTH_SHORT).show();
                }
                Intent intent = new Intent(ACTION_NOTIF_BACKGROUND_CHANGED);
                getActivity().sendBroadcast(intent);
            } else if (requestCode == REQ_CALLER_PHOTO) {
                if (resultCode == Activity.RESULT_OK) {
                    if (wallpaperTemporary.exists()) {
                        wallpaperTemporary.renameTo(callerPhotoFile);
                    }
                    callerPhotoFile.setReadable(true, false);
                    Toast.makeText(getActivity(), getString(
                            R.string.caller_unknown_photo_result_successful), 
                            Toast.LENGTH_SHORT).show();
                } else {
                    if (wallpaperTemporary.exists()) {
                        wallpaperTemporary.delete();
                    }
                    Toast.makeText(getActivity(), getString(
                            R.string.caller_unkown_photo_result_not_successful),
                            Toast.LENGTH_SHORT).show();
                }
            }
        }
    }
}