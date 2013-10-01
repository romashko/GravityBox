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

package com.ceco.gm2.gravitybox.quicksettings;

import com.ceco.gm2.gravitybox.GravityBoxResultReceiver;
import com.ceco.gm2.gravitybox.R;
import com.ceco.gm2.gravitybox.GravityBoxResultReceiver.Receiver;
import com.ceco.gm2.gravitybox.GravityBoxService;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SyncStatusObserver;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

public class SyncTile extends AQuickSettingsTile {

    private TextView mTextView;
    private Handler mHandler;
    private Object mSyncObserverHandle = null;
    private GravityBoxResultReceiver mReceiver;
    private boolean mSyncState;

    public SyncTile(Context context, Context gbContext, Object statusBar, Object panelBar) {
        super(context, gbContext, statusBar, panelBar);

        mOnClick = new View.OnClickListener() {
            
            @Override
            public void onClick(View v) {
                toggleState();
            }
        };

        mHandler = new Handler();
    }

    @Override
    protected void onTileCreate() {
        LayoutInflater inflater = LayoutInflater.from(mGbContext);
        inflater.inflate(R.layout.quick_settings_tile_sync, mTile);
        mTextView = (TextView) mTile.findViewById(R.id.sync_tileview);

        mReceiver = new GravityBoxResultReceiver(mHandler);
        mReceiver.setReceiver(new Receiver() {

            @Override
            public void onReceiveResult(int resultCode, Bundle resultData) {
                if (resultCode == GravityBoxService.RESULT_SYNC_STATUS) {
                    mSyncState = resultData.getBoolean(GravityBoxService.KEY_SYNC_STATUS);
                    updateResources();
                }
            }
            
        });

        mSyncObserverHandle = ContentResolver.addStatusChangeListener(
                ContentResolver.SYNC_OBSERVER_TYPE_SETTINGS, mSyncObserver);
    }

    @Override
    protected void onTilePostCreate() {
        getSyncState();
    }

    @Override
    protected void updateTile() {
        if (mSyncState) {
            mDrawableId = R.drawable.ic_qs_sync_on;
            mLabel = mGbResources.getString(R.string.quick_settings_sync_on);
        } else {
            mDrawableId = R.drawable.ic_qs_sync_off;
            mLabel = mGbResources.getString(R.string.quick_settings_sync_off);
        }

        mTextView.setText(mLabel);
        mTextView.setCompoundDrawablesWithIntrinsicBounds(0, mDrawableId, 0, 0);
    }

    private void getSyncState() {
        Intent si = new Intent(mGbContext, GravityBoxService.class);
        si.setAction(GravityBoxService.ACTION_GET_SYNC_STATUS);
        si.putExtra("receiver", mReceiver);
        mGbContext.startService(si);
    }

    private void toggleState() {
        Intent si = new Intent(mGbContext, GravityBoxService.class);
        si.setAction(GravityBoxService.ACTION_TOGGLE_SYNC);
        mGbContext.startService(si);
    }

    private SyncStatusObserver mSyncObserver = new SyncStatusObserver() {
        public void onStatusChanged(int which) {
            // update state/view if something happened
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    getSyncState();
                }
            });
        }
    };
}