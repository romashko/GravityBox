package com.ceco.gm2.gravitybox.systemui;

import android.content.Context;
import android.content.Intent;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnLongClickListener;

import com.android.systemui.R;
import com.ceco.gm2.gravitybox.systemui.QuickSettingsController;
import com.ceco.gm2.gravitybox.systemui.QuickSettingsContainerView;
import com.mediatek.systemui.ext.IconIdWrapper;
import com.mediatek.systemui.ext.NetworkType;
import com.android.systemui.statusbar.policy.NetworkControllerGemini;

public class AirplaneModeTile extends QuickSettingsTile implements NetworkControllerGemini.SignalCluster {
    private boolean enabled = false;
    private NetworkControllerGemini mControllerGemini;

    public AirplaneModeTile(Context context, QuickSettingsController qsc, NetworkControllerGemini controller) {
        super(context, qsc);

        mControllerGemini = controller;

        mOnClick = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Change the system setting
                Settings.Global.putInt(mContext.getContentResolver(), Settings.Global.AIRPLANE_MODE_ON,
                                        !enabled ? 1 : 0);

                // Post the intent
                Intent intent = new Intent(Intent.ACTION_AIRPLANE_MODE_CHANGED);
                intent.putExtra("state", !enabled);
                mContext.sendBroadcast(intent);
            }
        };
        mOnLongClick = new OnLongClickListener() {

            @Override
            public boolean onLongClick(View v) {
                startSettingsActivity(android.provider.Settings.ACTION_WIRELESS_SETTINGS);
                return true;
            }
        };
    }

    @Override
    void onPostCreate() {
        mControllerGemini.addSignalCluster(this);
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
        mLabel = mContext.getString(R.string.quick_settings_airplane_mode_label);
        mDrawable = (enabled) ? R.drawable.ic_qs_airplane_on : R.drawable.ic_qs_airplane_off;
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
    public void setIsAirplaneMode(boolean enabled) {
        // TODO Auto-generated method stub
        this.enabled = enabled;
        updateResources();
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
    public void setWifiIndicators(boolean arg0, int arg1, int arg2, String arg3) {
        // TODO Auto-generated method stub
        
    }

}
