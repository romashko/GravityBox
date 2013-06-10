/*
 * Copyright (C) 2012 The Android Open Source Project
 *
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

package com.ceco.gm2.gravitybox.systemui;

import static com.android.internal.util.cm.QSConstants.TILES_DEFAULT;
import static com.android.internal.util.cm.QSConstants.TILE_AIRPLANE;
import static com.android.internal.util.cm.QSConstants.TILE_AUTOROTATE;
import static com.android.internal.util.cm.QSConstants.TILE_BATTERY;
import static com.android.internal.util.cm.QSConstants.TILE_BLUETOOTH;
import static com.android.internal.util.cm.QSConstants.TILE_BRIGHTNESS;
import static com.android.internal.util.cm.QSConstants.TILE_DELIMITER;
import static com.android.internal.util.cm.QSConstants.TILE_GPS;
import static com.android.internal.util.cm.QSConstants.TILE_LTE;
import static com.android.internal.util.cm.QSConstants.TILE_MOBILEDATA;
import static com.android.internal.util.cm.QSConstants.TILE_NETWORKMODE;
import static com.android.internal.util.cm.QSConstants.TILE_RINGER;
import static com.android.internal.util.cm.QSConstants.TILE_SCREENTIMEOUT;
import static com.android.internal.util.cm.QSConstants.TILE_SETTINGS;
import static com.android.internal.util.cm.QSConstants.TILE_SLEEP;
import static com.android.internal.util.cm.QSConstants.TILE_SYNC;
import static com.android.internal.util.cm.QSConstants.TILE_TORCH;
import static com.android.internal.util.cm.QSConstants.TILE_USER;
import static com.android.internal.util.cm.QSConstants.TILE_VOLUME;
import static com.android.internal.util.cm.QSConstants.TILE_WIFI;
import static com.android.internal.util.cm.QSConstants.TILE_WIFIAP;
import static com.android.internal.util.cm.QSConstants.TILE_WIMAX;
import static com.android.internal.util.cm.QSUtils.deviceSupportsBluetooth;
import static com.android.internal.util.cm.QSUtils.deviceSupportsImeSwitcher;
import static com.android.internal.util.cm.QSUtils.deviceSupportsLte;
import static com.android.internal.util.cm.QSUtils.deviceSupportsMobileData;
import static com.android.internal.util.cm.QSUtils.deviceSupportsUsbTether;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;

import com.ceco.gm2.gravitybox.systemui.AirplaneModeTile;
import com.ceco.gm2.gravitybox.systemui.AlarmTile;
import com.ceco.gm2.gravitybox.systemui.AutoRotateTile;
import com.ceco.gm2.gravitybox.systemui.BatteryTile;
import com.ceco.gm2.gravitybox.systemui.BluetoothTile;
import com.ceco.gm2.gravitybox.systemui.BrightnessTile;
import com.ceco.gm2.gravitybox.systemui.BugReportTile;
import com.ceco.gm2.gravitybox.systemui.GPSTile;
import com.ceco.gm2.gravitybox.systemui.InputMethodTile;
import com.ceco.gm2.gravitybox.systemui.MobileNetworkTile;
import com.ceco.gm2.gravitybox.systemui.PreferencesTile;
import com.ceco.gm2.gravitybox.systemui.QuickSettingsTile;
import com.ceco.gm2.gravitybox.systemui.RingerModeTile;
import com.ceco.gm2.gravitybox.systemui.ScreenTimeoutTile;
import com.ceco.gm2.gravitybox.systemui.SleepScreenTile;
import com.ceco.gm2.gravitybox.systemui.SyncTile;
import com.ceco.gm2.gravitybox.systemui.TorchTile;
import com.ceco.gm2.gravitybox.systemui.UsbTetherTile;
import com.ceco.gm2.gravitybox.systemui.UserTile;
import com.ceco.gm2.gravitybox.systemui.VolumeTile;
import com.ceco.gm2.gravitybox.systemui.WiFiDisplayTile;
import com.ceco.gm2.gravitybox.systemui.WiFiTile;
import com.ceco.gm2.gravitybox.systemui.WifiAPTile;
import com.android.systemui.statusbar.phone.PanelBar;
import com.android.systemui.statusbar.phone.PhoneStatusBar;
import com.android.systemui.statusbar.policy.BatteryController;
import com.android.systemui.statusbar.policy.BluetoothController;
import com.android.systemui.statusbar.policy.NetworkControllerGemini;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class QuickSettingsController {
    private static String TAG = "QuickSettingsController";

    // Stores the broadcast receivers and content observers
    // quick tiles register for.
    public HashMap<String, ArrayList<QuickSettingsTile>> mReceiverMap
        = new HashMap<String, ArrayList<QuickSettingsTile>>();
    public HashMap<Uri, ArrayList<QuickSettingsTile>> mObserverMap
        = new HashMap<Uri, ArrayList<QuickSettingsTile>>();

    // Uris that need to be monitored for updating tile status
    private HashSet<Uri> mTileStatusUris = new HashSet<Uri>();

    private final Context mContext;
    private ArrayList<QuickSettingsTile> mQuickSettingsTiles;
    public PanelBar mBar;
    private final QuickSettingsContainerView mContainerView;
    private final Handler mHandler;
    private BroadcastReceiver mReceiver;
    private ContentObserver mObserver;
    public PhoneStatusBar mStatusBarService;
    private NetworkControllerGemini mNetworkController;
    private BluetoothController mBluetoothController;

    private InputMethodTile mIMETile;

    private static final int MSG_UPDATE_TILES = 1000;

    public QuickSettingsController(Context context, QuickSettingsContainerView container, PhoneStatusBar statusBarService,
            NetworkControllerGemini networkController, BluetoothController btController) {
        mContext = context;
        mContainerView = container;
        mHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);

                switch (msg.what) {
                    case MSG_UPDATE_TILES:
                        setupQuickSettings();
                        break;
                }
            }
        };
        mStatusBarService = statusBarService;
        mQuickSettingsTiles = new ArrayList<QuickSettingsTile>();
        mNetworkController = networkController;
        mBluetoothController = btController;
    }

    void loadTiles() {
        // Reset reference tiles
        mIMETile = null;

        // Filter items not compatible with device
        boolean bluetoothSupported = deviceSupportsBluetooth();
        boolean mobileDataSupported = deviceSupportsMobileData(mContext);
        boolean lteSupported = deviceSupportsLte(mContext);

        if (!bluetoothSupported) {
            TILES_DEFAULT.remove(TILE_BLUETOOTH);
        }

        if (!mobileDataSupported) {
            TILES_DEFAULT.remove(TILE_WIFIAP);
            TILES_DEFAULT.remove(TILE_MOBILEDATA);
            TILES_DEFAULT.remove(TILE_NETWORKMODE);
        }

        if (!lteSupported) {
            TILES_DEFAULT.remove(TILE_LTE);
        }

        // Read the stored list of tiles
        ContentResolver resolver = mContext.getContentResolver();
        LayoutInflater inflater = LayoutInflater.from(mContext);
        String tiles = Settings.System.getString(resolver, Settings.System.QUICK_SETTINGS_TILES);
        if (tiles == null) {
            Log.i(TAG, "Default tiles being loaded");
            tiles = TextUtils.join(TILE_DELIMITER, TILES_DEFAULT);
        }

        Log.i(TAG, "Tiles list: " + tiles);

        // Split out the tile names and add to the list
        for (String tile : tiles.split("\\|")) {
            QuickSettingsTile qs = null;
            if (tile.equals(TILE_USER)) {
                qs = new UserTile(mContext, this);
            } else if (tile.equals(TILE_BATTERY)) {
                qs = new BatteryTile(mContext, this, new BatteryController(mContext));
            } else if (tile.equals(TILE_SETTINGS)) {
                qs = new PreferencesTile(mContext, this);
            } else if (tile.equals(TILE_WIFI)) {
                qs = new WiFiTile(mContext, this, mNetworkController);
            } else if (tile.equals(TILE_GPS)) {
                qs = new GPSTile(mContext, this);
            } else if (tile.equals(TILE_BLUETOOTH) && bluetoothSupported) {
                qs = new BluetoothTile(mContext, this, mBluetoothController);
            } else if (tile.equals(TILE_BRIGHTNESS)) {
                qs = new BrightnessTile(mContext, this, mHandler);
            } else if (tile.equals(TILE_RINGER)) {
                qs = new RingerModeTile(mContext, this);
            } else if (tile.equals(TILE_SYNC)) {
                qs = new SyncTile(mContext, this);
            } else if (tile.equals(TILE_WIFIAP) && mobileDataSupported) {
                qs = new WifiAPTile(mContext, this);
            } else if (tile.equals(TILE_SCREENTIMEOUT)) {
                qs = new ScreenTimeoutTile(mContext, this);
            } else if (tile.equals(TILE_MOBILEDATA) && mobileDataSupported) {
                qs = new MobileNetworkTile(mContext, this, mNetworkController);
            } else if (tile.equals(TILE_AUTOROTATE)) {
                qs = new AutoRotateTile(mContext, this, mHandler);
            } else if (tile.equals(TILE_AIRPLANE)) {
                qs = new AirplaneModeTile(mContext, this, mNetworkController);
            } else if (tile.equals(TILE_TORCH)) {
                qs = new TorchTile(mContext, this, mHandler);
            } else if (tile.equals(TILE_SLEEP)) {
                qs = new SleepScreenTile(mContext, this);
            } else if (tile.equals(TILE_WIMAX)) {
                // Not available yet
            } else if (tile.equals(TILE_VOLUME)) {
                qs = new VolumeTile(mContext, this, mHandler);
            }

            if (qs != null) {
                qs.setupQuickSettingsTile(inflater, mContainerView);
                mQuickSettingsTiles.add(qs);
            }
        }

        // Load the dynamic tiles
        // These toggles must be the last ones added to the view, as they will show
        // only when they are needed
        if (Settings.System.getInt(resolver, Settings.System.QS_DYNAMIC_ALARM, 1) == 1) {
            QuickSettingsTile qs = new AlarmTile(mContext, this, mHandler);
            qs.setupQuickSettingsTile(inflater, mContainerView);
            mQuickSettingsTiles.add(qs);
        }
        if (Settings.System.getInt(resolver, Settings.System.QS_DYNAMIC_BUGREPORT, 1) == 1) {
            QuickSettingsTile qs = new BugReportTile(mContext, this, mHandler);
            qs.setupQuickSettingsTile(inflater, mContainerView);
            mQuickSettingsTiles.add(qs);
        }
        if (Settings.System.getInt(resolver, Settings.System.QS_DYNAMIC_WIFI, 1) == 1) {
            QuickSettingsTile qs = new WiFiDisplayTile(mContext, this);
            qs.setupQuickSettingsTile(inflater, mContainerView);
            mQuickSettingsTiles.add(qs);
        }
        if (deviceSupportsImeSwitcher(mContext) && Settings.System.getInt(resolver, Settings.System.QS_DYNAMIC_IME, 1) == 1) {
            mIMETile = new InputMethodTile(mContext, this);
            mIMETile.setupQuickSettingsTile(inflater, mContainerView);
            mQuickSettingsTiles.add(mIMETile);
        }
        if (deviceSupportsUsbTether(mContext) && Settings.System.getInt(resolver, Settings.System.QS_DYNAMIC_USBTETHER, 1) == 1) {
            QuickSettingsTile qs = new UsbTetherTile(mContext, this);
            qs.setupQuickSettingsTile(inflater, mContainerView);
            mQuickSettingsTiles.add(qs);
        }
    }

    public void shutdown() {
        if (mObserver != null) {
            mContext.getContentResolver().unregisterContentObserver(mObserver);
        }
        if (mReceiver != null) {
            mContext.unregisterReceiver(mReceiver);
        }
        for (QuickSettingsTile qs : mQuickSettingsTiles) {
            qs.onDestroy();
        }
        mQuickSettingsTiles.clear();
        mContainerView.removeAllViews();
    }

    protected void setupQuickSettings() {
        shutdown();
        mReceiver = new QSBroadcastReceiver();
        mReceiverMap.clear();
        mObserver = new QuickSettingsObserver(mHandler);
        mObserverMap.clear();
        mTileStatusUris.clear();
        loadTiles();
        setupBroadcastReceiver();
        setupContentObserver();
    }

    void setupContentObserver() {
        ContentResolver resolver = mContext.getContentResolver();
        for (Uri uri : mObserverMap.keySet()) {
            resolver.registerContentObserver(uri, false, mObserver);
        }
        for (Uri uri : mTileStatusUris) {
            resolver.registerContentObserver(uri, false, mObserver);
        }
    }

    private class QuickSettingsObserver extends ContentObserver {
        public QuickSettingsObserver(Handler handler) {
            super(handler);
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            if (mTileStatusUris.contains(uri)) {
                mHandler.removeMessages(MSG_UPDATE_TILES);
                mHandler.sendEmptyMessage(MSG_UPDATE_TILES);
            } else {
                ContentResolver resolver = mContext.getContentResolver();
                for (QuickSettingsTile tile : mObserverMap.get(uri)) {
                    tile.onChangeUri(resolver, uri);
                }
            }
        }
    }

    void setupBroadcastReceiver() {
        IntentFilter filter = new IntentFilter();
        for (String action : mReceiverMap.keySet()) {
            filter.addAction(action);
        }
        mContext.registerReceiver(mReceiver, filter);
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private void registerInMap(Object item, QuickSettingsTile tile, HashMap map) {
        if (map.keySet().contains(item)) {
            ArrayList list = (ArrayList) map.get(item);
            if (!list.contains(tile)) {
                list.add(tile);
            }
        } else {
            ArrayList<QuickSettingsTile> list = new ArrayList<QuickSettingsTile>();
            list.add(tile);
            map.put(item, list);
        }
    }

    public void registerAction(Object action, QuickSettingsTile tile) {
        registerInMap(action, tile, mReceiverMap);
    }

    public void registerObservedContent(Uri uri, QuickSettingsTile tile) {
        registerInMap(uri, tile, mObserverMap);
    }

    private class QSBroadcastReceiver extends BroadcastReceiver {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action != null) {
                for (QuickSettingsTile t : mReceiverMap.get(action)) {
                    t.onReceive(context, intent);
                }
            }
        }
    };

    void setBar(PanelBar bar) {
        mBar = bar;
    }

    public void setService(PhoneStatusBar phoneStatusBar) {
        mStatusBarService = phoneStatusBar;
    }

    public void setImeWindowStatus(boolean visible) {
        if (mIMETile != null) {
            mIMETile.toggleVisibility(visible);
        }
    }

    public void updateResources() {
        mContainerView.updateResources();
        for (QuickSettingsTile t : mQuickSettingsTiles) {
            t.updateResources();
        }
    }
}
