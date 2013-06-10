package com.ceco.gm2.gravitybox.systemui;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnLongClickListener;

import com.android.systemui.R;
import com.ceco.gm2.gravitybox.systemui.QuickSettingsContainerView;
import com.ceco.gm2.gravitybox.systemui.QuickSettingsController;
import com.mediatek.systemui.ext.IconIdWrapper;
import com.mediatek.systemui.ext.NetworkType;
import com.android.systemui.statusbar.policy.NetworkController;
import com.android.systemui.statusbar.policy.NetworkControllerGemini;
import com.android.systemui.statusbar.policy.NetworkControllerGemini.SignalCluster;

public class WiFiTile extends QuickSettingsTile implements SignalCluster {
    private NetworkControllerGemini mController;
    private boolean mWifiConnected;
    private boolean mWifiNotConnected;
    private int mWifiSignalIconId;
    private String mDescription;

    public WiFiTile(Context context, QuickSettingsController qsc, NetworkControllerGemini controller) {
        super(context, qsc);

        mController = controller;

        mOnClick = new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                WifiManager wfm = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);
                wfm.setWifiEnabled(!wfm.isWifiEnabled());
            }
        };
        mOnLongClick = new OnLongClickListener() {

            @Override
            public boolean onLongClick(View v) {
                startSettingsActivity(android.provider.Settings.ACTION_WIFI_SETTINGS);
                return true;
            }
        };
    }

    @Override
    void onPostCreate() {
        mController.addSignalCluster(this);
        updateTile();
        super.onPostCreate();
    }

    @Override
    public void onDestroy() {
        //mController.removeNetworkSignalChangedCallback(this);
        super.onDestroy();
    }

    @Override
    public void updateResources() {
        updateTile();
        super.updateResources();
    }

    private synchronized void updateTile() {
        if (mWifiConnected) {
            mDrawable = mWifiSignalIconId;
            mLabel = mDescription.substring(1, mDescription.length()-1);
        } else if (mWifiNotConnected) {
            mDrawable = R.drawable.ic_qs_wifi_0;
            mLabel = mContext.getString(R.string.quick_settings_wifi_label);
        } else {
            mDrawable = R.drawable.ic_qs_wifi_no_network;
            mLabel = mContext.getString(R.string.quick_settings_wifi_off_label);
        }
    }

    @Override
    public void apply() {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void setDataConnected(int arg0, boolean arg1) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void setDataNetType3G(int arg0, NetworkType arg1) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void setIsAirplaneMode(boolean arg0) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void setMobileDataIndicators(int arg0, boolean arg1,
            IconIdWrapper[] arg2, IconIdWrapper arg3, IconIdWrapper arg4,
            String arg5, String arg6) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void setRoamingFlagandResource(boolean arg0, boolean arg1, int arg2,
            int arg3) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void setShowSimIndicator(int arg0, boolean arg1, int arg2) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void setWifiIndicators(boolean enabled, int wifiSignalIconId, int arg2, String description) {
        // TODO Auto-generated method stub
        mWifiConnected = enabled && (wifiSignalIconId > 0) && (description != null);
        mWifiNotConnected = (wifiSignalIconId > 0) && (description == null);
        mWifiSignalIconId = wifiSignalIconId;
        mDescription = description;
        updateResources();
    }

}
