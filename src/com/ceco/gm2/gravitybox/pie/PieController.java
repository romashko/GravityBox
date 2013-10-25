/*
 * Copyright (C) 2013 The CyanogenMod Project (Jens Doll)
 * Copyright (C) 2013 Peter Gregus for GravityBox project (C3C076@xda)
 * This code is loosely based on portions of the ParanoidAndroid Project source, Copyright (C) 2012.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.ceco.gm2.gravitybox.pie;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import android.app.ActivityOptions;
import android.app.SearchManager;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.hardware.input.InputManager;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.os.Vibrator;
import android.provider.Settings;
import android.telephony.PhoneStateListener;
import android.telephony.ServiceState;
import android.telephony.TelephonyManager;
import android.view.HapticFeedbackConstants;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.ImageView;

import com.ceco.gm2.gravitybox.ModPieControls;
import com.ceco.gm2.gravitybox.R;
import com.ceco.gm2.gravitybox.pie.PieItem;
import com.ceco.gm2.gravitybox.pie.PieLayout;
import com.ceco.gm2.gravitybox.pie.PieLayout.PieDrawable;
import com.ceco.gm2.gravitybox.pie.PieLayout.PieSlice;
import com.ceco.gm2.gravitybox.pie.PieSliceContainer;
import com.ceco.gm2.gravitybox.pie.PieSysInfo;

import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.XposedHelpers.ClassNotFoundError;

/**
 * Controller class for the default pie control.
 * <p>
 * This class is responsible for setting up the pie control, activating it, and defining and
 * executing the actions that can be triggered by the pie control.
 */
public class PieController implements PieLayout.OnSnapListener, PieItem.PieOnClickListener {
    public static final String PACKAGE_NAME = "com.android.systemui";
    public static final String TAG = "GB:PieController";
    private static final String CLASS_BASE_STATUSBAR = "com.android.systemui.statusbar.BaseStatusBar";
    public static final boolean DEBUG = false;

    private enum ButtonType {
        BACK,
        HOME,
        RECENT,
        MENU,
        SEARCH
    };

    public static final float EMPTY_ANGLE = 10;
    public static final float START_ANGLE = 180 + EMPTY_ANGLE;

    private static final int MSG_INJECT_KEY = 1066;

    private Context mContext;
    private Context mGbContext;
    private Resources mGbResources;
    private PieLayout mPieContainer;
    /**
     * This is only needed for #toggleRecentApps()
     */
    private Object mStatusBar;
    private Class<?> mBaseStatusBarClass;
    private Vibrator mVibrator;
    private int mBatteryLevel;
    private int mBatteryStatus;
    private boolean mHasTelephony;
    private ServiceState mServiceState;

    // all pie slices that are managed by the controller
    private PieSliceContainer mNavigationSlice;
    private PieSysInfo mSysInfo;
    private PieItem mMenuButton;

    private int mNavigationIconHints = 0;
    private int mDisabledFlags = 0;
    private boolean mShowMenu = false;
    private boolean mShowSearch = false;
    private Drawable mBackIcon;
    private Drawable mBackAltIcon;

    private static void log(String message) {
        XposedBridge.log(TAG + ": " + message);
    }

    /**
     * Defines the positions in which pie controls may appear. This enumeration is used to store
     * an index, a flag and the android gravity for each position.
     */
    public enum Position {
        LEFT(0, 0, android.view.Gravity.LEFT),
        BOTTOM(1, 1, android.view.Gravity.BOTTOM),
        RIGHT(2, 1, android.view.Gravity.RIGHT),
        TOP(3, 0, android.view.Gravity.TOP);

        Position(int index, int factor, int android_gravity) {
            INDEX = index;
            FLAG = (0x01<<index);
            ANDROID_GRAVITY = android_gravity;
            FACTOR = factor;
        }

        public final int INDEX;
        public final int FLAG;
        public final int ANDROID_GRAVITY;
        /**
         * This is 1 when the position is not at the axis (like {@link Position.RIGHT} is
         * at {@code Layout.getWidth()} not at {@code 0}).
         */
        public final int FACTOR;
    }

    private Position mPosition;

    public static class Tracker {
        public static float sDistance;
        private float initialX = 0;
        private float initialY = 0;
        private float gracePeriod = 0;

        private Tracker(Position position) {
            this.position = position;
        }

        public void start(MotionEvent event) {
            initialX = event.getX();
            initialY = event.getY();
            switch (position) {
                case LEFT:
                    gracePeriod = initialX + sDistance / 3.0f;
                    break;
                case RIGHT:
                    gracePeriod = initialX - sDistance / 3.0f;
                    break;
                default:
                    break;
            }
            active = true;
        }

        public boolean move(MotionEvent event) {
            final float x = event.getX();
            final float y = event.getY();
            if (!active) {
                return false;
            }

            // Unroll the complete logic here - we want to be fast and out of the
            // event chain as fast as possible.
            boolean loaded = false;
            switch (position) {
                case LEFT:
                    if (x < gracePeriod) {
                        initialY = y;
                    }
                    if (initialY - y < sDistance && y - initialY < sDistance) {
                        if (x - initialX <= sDistance) {
                            return false;
                        }
                        loaded = true;
                    }
                    break;
                case BOTTOM:
                    if (initialX - x < sDistance && x - initialX < sDistance) {
                        if (initialY - y <= sDistance) {
                            return false;
                        }
                        loaded = true;
                    }
                    break;
                case TOP:
                    if (initialX - x < sDistance && x - initialX < sDistance) {
                        if (y - initialY <= sDistance) {
                            return false;
                        }
                        loaded = true;
                    }
                    break;
                case RIGHT:
                    if (x > gracePeriod) {
                        initialY = y;
                    }
                    if (initialY - y < sDistance && y - initialY < sDistance) {
                        if (initialX - x <= sDistance) {
                            return false;
                        }
                        loaded = true;
                    }
                    break;
            }
            active = false;
            return loaded;
        }

        public boolean active = false;
        public final Position position;
    }

    public Tracker buildTracker(Position position) {
        return new Tracker(position);
    }

    private static class H extends Handler {
        public void handleMessage(Message m) {
            switch (m.what) {
                case MSG_INJECT_KEY:
                    final long eventTime = SystemClock.uptimeMillis();
                    final InputManager inputManager = (InputManager)
                            XposedHelpers.callStaticMethod(InputManager.class, "getInstance");

                    XposedHelpers.callMethod(inputManager, "injectInputEvent",
                            new KeyEvent(eventTime - 50, eventTime - 50, KeyEvent.ACTION_DOWN, m.arg1, 0), 0);
                    XposedHelpers.callMethod(inputManager, "injectInputEvent",
                            new KeyEvent(eventTime - 50, eventTime - 25, KeyEvent.ACTION_UP, m.arg1, 0), 0);

                    break;
            }
        }
    }
    private H mHandler = new H();

    private void injectKeyDelayed(int keycode) {
        mHandler.removeMessages(MSG_INJECT_KEY);
        mHandler.sendMessageDelayed(Message.obtain(mHandler, MSG_INJECT_KEY, keycode, 0), 50);
    }

    private BroadcastReceiver mBatteryReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            mBatteryLevel = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);
            mBatteryStatus = intent.getIntExtra(BatteryManager.EXTRA_STATUS,
                    BatteryManager.BATTERY_STATUS_UNKNOWN);
        }
    };

    private PhoneStateListener mPhoneStateListener = new PhoneStateListener() {
        @Override
        public void onServiceStateChanged(ServiceState serviceState) {
            mServiceState = serviceState;
        }
    };

    public PieController(Context context, Context gbContext) {
        mContext = context;
        mGbContext = gbContext;
        mGbResources = gbContext.getResources();

        mVibrator = (Vibrator) mContext.getSystemService(Context.VIBRATOR_SERVICE);

        final PackageManager pm = mContext.getPackageManager();
        mHasTelephony = pm.hasSystemFeature(PackageManager.FEATURE_TELEPHONY);

        final Resources res = mContext.getResources();
        Tracker.sDistance = mGbResources.getDimensionPixelSize(R.dimen.pie_trigger_distance);

        mBackIcon = res.getDrawable(context.getResources().getIdentifier(
                "ic_sysbar_back", "drawable", PACKAGE_NAME));
        mBackAltIcon = res.getDrawable(context.getResources().getIdentifier(
                "ic_sysbar_back_ime", "drawable", PACKAGE_NAME));

        try {
            mBaseStatusBarClass = XposedHelpers.findClass(CLASS_BASE_STATUSBAR, mContext.getClassLoader());
        } catch (ClassNotFoundError e) {
            XposedBridge.log(e);
        }
    }

    public void attachTo(Object statusBar) {
        mStatusBar = statusBar;
    }

    public void attachTo(PieLayout container) {
        mPieContainer = container;
        mPieContainer.clearSlices();

        if (DEBUG) {
            log("Attaching to container: " + container);
        }

        mPieContainer.setOnSnapListener(this);

        // construct navbar slice
        int inner = mGbResources.getDimensionPixelSize(R.dimen.pie_navbar_radius);
        int outer = inner + mGbResources.getDimensionPixelSize(R.dimen.pie_navbar_height);

        mNavigationSlice = new PieSliceContainer(mPieContainer, PieSlice.IMPORTANT
                | PieDrawable.DISPLAY_ALL);
        mNavigationSlice.setGeometry(START_ANGLE, 180 - 2 * EMPTY_ANGLE, inner, outer);
        setupNavigationItems();
        mPieContainer.addSlice(mNavigationSlice);

        // construct sysinfo slice
        inner = mGbResources.getDimensionPixelSize(R.dimen.pie_sysinfo_radius);
        outer = inner + mGbResources.getDimensionPixelSize(R.dimen.pie_sysinfo_height);
        mSysInfo = new PieSysInfo(mContext, mGbContext, mPieContainer, this, PieDrawable.DISPLAY_NOT_AT_TOP);
        mSysInfo.setGeometry(START_ANGLE, 180 - 2 * EMPTY_ANGLE, inner, outer);
        mPieContainer.addSlice(mSysInfo);

        // start listening for changes
        mContext.registerReceiver(mBatteryReceiver,
                new IntentFilter(Intent.ACTION_BATTERY_CHANGED));

        if (mHasTelephony) {
            TelephonyManager telephonyManager =
                    (TelephonyManager) mContext.getSystemService(Context.TELEPHONY_SERVICE);
            telephonyManager.listen(mPhoneStateListener, PhoneStateListener.LISTEN_SERVICE_STATE);
        }
    }

    private void setupNavigationItems() {
        if (mNavigationSlice == null) return;

        Resources res = mContext.getResources();
        int minimumImageSize = (int)mGbResources.getDimension(R.dimen.pie_item_size);

        mNavigationSlice.clear();
        mNavigationSlice.addItem(constructItem(2, ButtonType.BACK,
                res.getDrawable(res.getIdentifier("ic_sysbar_back", "drawable", PACKAGE_NAME)),
                minimumImageSize));
        mNavigationSlice.addItem(constructItem(2, ButtonType.HOME,
                res.getDrawable(res.getIdentifier("ic_sysbar_home", "drawable", PACKAGE_NAME)),
                minimumImageSize));
        mNavigationSlice.addItem(constructItem(2, ButtonType.RECENT,
                res.getDrawable(res.getIdentifier("ic_sysbar_recent", "drawable", PACKAGE_NAME)),
                minimumImageSize));
        if (mShowSearch) {
            mNavigationSlice.addItem(constructItem(1, ButtonType.SEARCH,
                    mGbResources.getDrawable(R.drawable.ic_sysbar_search_side), minimumImageSize));
        }

        mMenuButton = constructItem(1, ButtonType.MENU,
                res.getDrawable(res.getIdentifier("ic_sysbar_menu", "drawable", PACKAGE_NAME)),
                minimumImageSize);
        mNavigationSlice.addItem(mMenuButton);

        setNavigationIconHints(mNavigationIconHints, true);
        setMenuVisibility(mShowMenu);
    }

    private PieItem constructItem(int width, ButtonType type, Drawable image, int minimumImageSize) {
        ImageView view = new ImageView(mContext);
        view.setImageDrawable(image);
        view.setMinimumWidth(minimumImageSize);
        view.setMinimumHeight(minimumImageSize);
        LayoutParams lp = new LayoutParams(minimumImageSize, minimumImageSize);
        view.setLayoutParams(lp);
        PieItem item = new PieItem(mContext, mGbContext, mPieContainer, 0, width, type, view);
        item.setOnClickListener(this);
        return item;
    }

    public void activateFromTrigger(View view, MotionEvent event, Position position) {
        if (mPieContainer != null && !isShowing()) {
            doHapticTriggerFeedback();

            mPosition = position;
            Point center = new Point((int) event.getRawX(), (int) event.getRawY());
            mPieContainer.activate(center, position);
            mPieContainer.invalidate();
        }
    }

    public void setNavigationIconHints(int hints) {
        // this call may come from outside
        // check if we already have a navigation slice to manipulate
        if (mNavigationSlice != null) {
            setNavigationIconHints(hints, false);
        } else {
            mNavigationIconHints = hints;
        }
    }

    protected void setNavigationIconHints(int hints, boolean force) {
        if (!force && hints == mNavigationIconHints) return;

        if (DEBUG) log("Pie navigation hints: " + hints);

        mNavigationIconHints = hints;

        PieItem item = findItem(ButtonType.HOME);
        if (item != null) {
            boolean isNop = (hints & (1 << 1)) != 0;
            item.setAlpha(isNop ? 0.5f : 1.0f);
        }
        item = findItem(ButtonType.RECENT);
        if (item != null) {
            boolean isNop = (hints & (1 << 2)) != 0;
            item.setAlpha(isNop ? 0.5f : 1.0f);
        }
        item = findItem(ButtonType.BACK);
        if (item != null) {
            boolean isNop = (hints & (1 << 0)) != 0;
            boolean isAlt = (hints & (1 << 3)) != 0;
            item.setAlpha(isNop ? 0.5f : 1.0f);
            item.setImageDrawable(isAlt ? mBackAltIcon : mBackIcon);
        }
        setDisabledFlags(mDisabledFlags, true);
    }

    private PieItem findItem(ButtonType type) {
        for (PieItem item : mNavigationSlice.getItems()) {
            ButtonType itemType = (ButtonType) item.tag;
            if (type == itemType) {
                return item;
            }
        }

        return null;
    }

    public void setDisabledFlags(int disabledFlags) {
        // this call may come from outside
        // check if we already have a navigation slice to manipulate
        if (mNavigationSlice != null) {
            setDisabledFlags(disabledFlags, false);
        } else {
            mDisabledFlags = disabledFlags;
        }
    }

    protected void setDisabledFlags(int disabledFlags, boolean force) {
        if (!force && mDisabledFlags == disabledFlags) return;

        mDisabledFlags = disabledFlags;

        final boolean disableHome = ((disabledFlags & 0x00200000) != 0);
        final boolean disableRecent = ((disabledFlags & 0x01000000) != 0);
        final boolean disableBack = ((disabledFlags & 0x00400000) != 0)
                && ((mNavigationIconHints & (1 << 3)) == 0);
        final boolean disableSearch = ((disabledFlags & 0x02000000) != 0);

        PieItem item = findItem(ButtonType.BACK);
        if (item != null) item.show(!disableBack);
        item = findItem(ButtonType.HOME);
        if (item != null) item.show(!disableHome);
        item = findItem(ButtonType.RECENT);
        if (item != null) item.show(!disableRecent);
        item = findItem(ButtonType.SEARCH);
        if (item != null) item.show(!disableRecent && !disableSearch);
        setMenuVisibility(mShowMenu);
    }

    public void setMenuVisibility(boolean showMenu) {
        // this call may come from outside
        if (mMenuButton != null) {
            final boolean disableRecent = ((mDisabledFlags & 0x01000000) != 0);
            mMenuButton.show(showMenu && !disableRecent);
        }

        mShowMenu = showMenu;
    }

    public void setSearchVisibility(boolean showSearch) {
        mShowSearch = showSearch;
        setupNavigationItems();
    }

    @Override
    public void onSnap(Position position) {
        if (position == mPosition) {
            return;
        }

        doHapticTriggerFeedback();

        if (DEBUG) {
            log("onSnap from " + position.name());
        }

        ModPieControls.onPieSnapped(mPosition.FLAG, position.FLAG);
    }

    @Override
    public void onClick(PieItem item) {
        ButtonType type = (ButtonType) item.tag;

        // provide the same haptic feedback as if a virtual key is pressed
        mPieContainer.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);

        switch (type) {
            case BACK:
                injectKeyDelayed(KeyEvent.KEYCODE_BACK);
                break;
            case HOME:
                injectKeyDelayed(KeyEvent.KEYCODE_HOME);
                break;
            case MENU:
                injectKeyDelayed(KeyEvent.KEYCODE_MENU);
                break;
            case RECENT:
                if (mStatusBar != null && mBaseStatusBarClass != null) {
                    try {
                        Method m = mBaseStatusBarClass.getDeclaredMethod("toggleRecentApps");
                        m.setAccessible(true);
                        try {
                            m.invoke(mStatusBar);
                        } catch (IllegalArgumentException e) {
                            XposedBridge.log(e);
                        } catch (IllegalAccessException e) {
                            XposedBridge.log(e);
                        } catch (InvocationTargetException e) {
                            XposedBridge.log(e);
                        }
                    } catch (NoSuchMethodException e) {
                        XposedBridge.log(e);
                    }
                }
                break;
            case SEARCH:
                launchAssistAction();
                break;
        }
    }

    private void doHapticTriggerFeedback() {
        if (mVibrator == null || !mVibrator.hasVibrator()) {
            return;
        }

        int hapticSetting = Settings.System.getInt(mContext.getContentResolver(),
                Settings.System.HAPTIC_FEEDBACK_ENABLED, 1);
        if (hapticSetting != 0) {
            mVibrator.vibrate(5);
        }
    }

    private void launchAssistAction() {
        SearchManager sm = (SearchManager) mContext.getSystemService(Context.SEARCH_SERVICE);
        Intent intent = (Build.VERSION.SDK_INT > 17) ?
                (Intent) XposedHelpers.callMethod(sm, "getAssistIntent", mContext, true) :
                (Intent) XposedHelpers.callMethod(sm, "getAssistIntent", mContext);
        Resources res = mContext.getResources();

        if (intent != null) {
            try {
                ActivityOptions opts = ActivityOptions.makeCustomAnimation(mContext,
                        res.getIdentifier("search_launch_enter", "anim", PACKAGE_NAME),
                        res.getIdentifier("search_launch_exit", "anim", PACKAGE_NAME));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                mContext.startActivity(intent, opts.toBundle());
            } catch (ActivityNotFoundException ignored) {
                // fall through
            }
        }
    }

    public boolean isShowing() {
        return mPieContainer != null && mPieContainer.isShowing();
    }

    public String getOperatorState() {
        if (!mHasTelephony) {
            return null;
        }
        if (mServiceState == null || mServiceState.getState() == ServiceState.STATE_OUT_OF_SERVICE) {
            return mGbResources.getString(R.string.pie_phone_status_no_service);
        }
        if (mServiceState.getState() == ServiceState.STATE_POWER_OFF) {
            return mGbResources.getString(R.string.pie_phone_status_airplane_mode);
        }
        if ((Boolean)XposedHelpers.callMethod(mServiceState, "isEmergencyOnly")) {
            return mGbResources.getString(R.string.pie_phone_status_emergency_only);
        }
        return mServiceState.getOperatorAlphaLong();
    }

    public String getBatteryLevel() {
        if (mBatteryStatus == BatteryManager.BATTERY_STATUS_FULL) {
            return mGbResources.getString(R.string.pie_battery_status_full);
        }
        if (mBatteryStatus == BatteryManager.BATTERY_STATUS_CHARGING) {
            return mGbResources.getString(R.string.pie_battery_status_charging, mBatteryLevel);
        }
        return mGbResources.getString(R.string.pie_battery_status_discharging, mBatteryLevel);
    }
}
