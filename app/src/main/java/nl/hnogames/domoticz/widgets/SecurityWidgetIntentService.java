/*
 * Copyright (C) 2015 Domoticz - Mark Heinis
 *
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package nl.hnogames.domoticz.widgets;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;

import nl.hnogames.domoticz.R;
import nl.hnogames.domoticz.helpers.StaticHelper;
import nl.hnogames.domoticz.utils.NotificationUtil;
import nl.hnogames.domoticz.utils.SharedPrefUtil;
import nl.hnogames.domoticz.utils.WidgetUtils;
import nl.hnogames.domoticzapi.DomoticzValues;
import nl.hnogames.domoticzapi.Interfaces.setCommandReceiver;

public class SecurityWidgetIntentService extends Service {
    private int widgetID = 0;
    private String password = null;
    private int action;
    private SharedPrefUtil mSharedPrefs;
    private int idx;
    private Context mContext;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        mSharedPrefs = new SharedPrefUtil(this);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            this.startForeground(1337, NotificationUtil.getForegroundServiceNotification(this, "Widget"));
        }

        widgetID = intent.getIntExtra("WIDGETID", 999999);
        idx = intent.getIntExtra("IDX", 999999);
        action = intent.getIntExtra("WIDGETACTION", 999999);
        password = intent.getStringExtra("WIDGETPASSWORD");
        mContext = this;

        if (intent.getAction().equals(SecurityWidgetProvider.ACTION_WIDGET_ARMAWAY)) {
            Log.i("onReceive", SecurityWidgetProvider.ACTION_WIDGET_ARMAWAY);
            action = DomoticzValues.Security.Status.ARMAWAY;
        } else if (intent.getAction().equals(SecurityWidgetProvider.ACTION_WIDGET_ARMHOME)) {
            Log.i("onReceive", SecurityWidgetProvider.ACTION_WIDGET_ARMHOME);
            action = DomoticzValues.Security.Status.ARMHOME;
        } else if (intent.getAction().equals(SecurityWidgetProvider.ACTION_WIDGET_DISARM)) {
            Log.i("onReceive", SecurityWidgetProvider.ACTION_WIDGET_DISARM);
            action = DomoticzValues.Security.Status.DISARM;
        }

        processRequest(idx, action, password);
        stopSelf();
        return START_NOT_STICKY;
    }


    private void processRequest(final int idx, final int status, final String password) {
        StaticHelper.getDomoticz(getApplicationContext()).setSecurityPanelAction(status, password, new setCommandReceiver() {
            @Override
            public void onReceiveResult(String result) {
                if (action == DomoticzValues.Security.Status.ARMAWAY) {
                    Toast.makeText(mContext, mContext.getString(R.string.status) + ": " +
                                    mContext.getString(R.string.security_arm_away),
                            Toast.LENGTH_LONG).show();

                } else if (action == DomoticzValues.Security.Status.ARMHOME) {
                    Toast.makeText(mContext, mContext.getString(R.string.status) + ": " +
                                    mContext.getString(R.string.security_arm_home),
                            Toast.LENGTH_LONG).show();

                } else if (action == DomoticzValues.Security.Status.DISARM) {
                    Toast.makeText(mContext, mContext.getString(R.string.status) + ": " +
                                    mContext.getString(R.string.security_disarm),
                            Toast.LENGTH_LONG).show();
                }
                WidgetUtils.RefreshWidgets(mContext);
            }

            @Override
            public void onError(Exception error) {
                Log.e("SECURITYWIDGET", StaticHelper.getDomoticz(getApplicationContext()).getErrorMessage(error));
                Toast.makeText(mContext,
                        mContext.getString(R.string.security_generic_error),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
