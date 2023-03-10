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

import static android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_ID;
import static android.appwidget.AppWidgetManager.INVALID_APPWIDGET_ID;

import android.app.PendingIntent;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;

import androidx.annotation.Nullable;

import java.util.ArrayList;

import nl.hnogames.domoticz.R;
import nl.hnogames.domoticz.helpers.StaticHelper;
import nl.hnogames.domoticz.utils.NotificationUtil;
import nl.hnogames.domoticz.utils.SharedPrefUtil;
import nl.hnogames.domoticz.utils.UsefulBits;
import nl.hnogames.domoticzapi.Containers.DevicesInfo;
import nl.hnogames.domoticzapi.DomoticzIcons;
import nl.hnogames.domoticzapi.Interfaces.DevicesReceiver;

public class SecurityWidgetProvider extends AppWidgetProvider {

    public static String ACTION_WIDGET_ARMAWAY = "nl.hnogames.domoticz.Service.WIDGET_SECURITY.ARMAWAY";
    public static String ACTION_WIDGET_ARMHOME = "nl.hnogames.domoticz.Service.WIDGET_SECURITY.ARMHOME";
    public static String ACTION_WIDGET_DISARM = "nl.hnogames.domoticz.Service.WIDGET_SECURITY.DISCARD";
    private static String packageName;

    public static PendingIntent buildButtonPendingIntent(Context context, int widget_id, int idx, String action, String password) {
        Intent intent = new Intent(context, SecurityWidgetIntentService.class);
        intent.setAction(action);
        intent.putExtra("IDX", idx);
        intent.putExtra("WIDGETID", widget_id);
        intent.putExtra("WIDGETPASSWORD", password);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return PendingIntent.getForegroundService(context, widget_id, intent, PendingIntent.FLAG_IMMUTABLE);
        } else {
            return PendingIntent.getService(context, widget_id, intent, PendingIntent.FLAG_IMMUTABLE);
        }
    }

    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        super.onDeleted(context, appWidgetIds);
        for (int widgetId : appWidgetIds) {
            SharedPrefUtil mSharedPrefs = new SharedPrefUtil(context);
            mSharedPrefs.deleteSecurityWidget(widgetId);
        }
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager,
                         int[] appWidgetIds) {
        super.onUpdate(context, appWidgetManager, appWidgetIds);
        packageName = context.getPackageName();

        // Get all ids
        ComponentName thisWidget = new ComponentName(context,
                SecurityWidgetProvider.class);
        int[] allWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget);
        if (allWidgetIds != null) {
            for (int mAppWidgetId : allWidgetIds) {
                Intent intent = new Intent(context, UpdateSecurityWidgetService.class);
                intent.putExtra(EXTRA_APPWIDGET_ID, mAppWidgetId);
                intent.setAction("FROM WIDGET PROVIDER");
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        context.startForegroundService(intent);
                    } else
                        context.startService(intent);
                } catch (Exception ex) {
                }
            }
        }
    }

    public static class UpdateSecurityWidgetService extends Service {
        private static final int INVALID_IDX = 999999;
        private static SharedPrefUtil mSharedPrefs;
        private RemoteViews views;

        @Override
        public int onStartCommand(Intent intent, int flags, int startId) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                this.startForeground(1337, NotificationUtil.getForegroundServiceNotification(this, "Widget"));
            }
            AppWidgetManager appWidgetManager = AppWidgetManager
                    .getInstance(UpdateSecurityWidgetService.this);
            try {
                int incomingAppWidgetId = intent.getIntExtra(EXTRA_APPWIDGET_ID,
                        INVALID_APPWIDGET_ID);
                if (incomingAppWidgetId != INVALID_APPWIDGET_ID) {
                    try {
                        updateAppWidget(appWidgetManager, incomingAppWidgetId);
                    } catch (NullPointerException e) {
                        if (!UsefulBits.isEmpty(e.getMessage()))
                            Log.e(SecurityWidgetProvider.class.getSimpleName() + "@onStartCommand", e.getMessage());
                    }
                }

            } catch (Exception ex) {
                Log.e("UpdateWidget", ex.toString());
            }

            stopSelf();
            return START_NOT_STICKY;
        }

        @Nullable
        @Override
        public IBinder onBind(Intent intent) {
            return null;
        }

        public void updateAppWidget(final AppWidgetManager appWidgetManager,
                                    final int appWidgetId) {
            if (mSharedPrefs == null)
                mSharedPrefs = new SharedPrefUtil(this.getApplicationContext());
            final int idx = mSharedPrefs.getSecurityWidgetIDX(appWidgetId);
            if (appWidgetId == INVALID_APPWIDGET_ID || idx == INVALID_IDX) {
                Log.i("WIDGET", "I am invalid");
                return;
            }

            try {
                final String password = mSharedPrefs.getSecurityWidgetPin(appWidgetId);
                views = new RemoteViews(packageName, mSharedPrefs.getSecurityWidgetLayout(appWidgetId));
                StaticHelper.getDomoticz(getApplicationContext()).getDevice(new DevicesReceiver() {
                    @Override
                    public void onReceiveDevices(ArrayList<DevicesInfo> mDevicesInfo) {
                    }

                    @Override
                    public void onReceiveDevice(DevicesInfo s) {
                        try {
                            if (s != null) {
                                views = new RemoteViews(packageName, mSharedPrefs.getSecurityWidgetLayout(appWidgetId));
                                views.setTextViewText(R.id.title, s.getName());
                                views.setTextViewText(R.id.status, getApplicationContext().getString(R.string.status) + ": " +
                                        s.getData());

                                views.setOnClickPendingIntent(R.id.armhome, buildButtonPendingIntent(
                                        UpdateSecurityWidgetService.this,
                                        appWidgetId,
                                        s.getIdx(), ACTION_WIDGET_ARMHOME, password));
                                views.setViewVisibility(R.id.armhome, View.VISIBLE);

                                views.setOnClickPendingIntent(R.id.armaway, buildButtonPendingIntent(
                                        UpdateSecurityWidgetService.this,
                                        appWidgetId,
                                        s.getIdx(), ACTION_WIDGET_ARMAWAY, password));
                                views.setViewVisibility(R.id.armaway, View.VISIBLE);

                                views.setOnClickPendingIntent(R.id.disarm, buildButtonPendingIntent(
                                        UpdateSecurityWidgetService.this,
                                        appWidgetId,
                                        s.getIdx(), ACTION_WIDGET_DISARM, password));
                                views.setViewVisibility(R.id.disarm, View.VISIBLE);

                                views.setImageViewResource(R.id.rowIcon, DomoticzIcons.getDrawableIcon(s.getTypeImg(), s.getType(), s.getSwitchType(), true, s.getUseCustomImage(), s.getImage()));
                                appWidgetManager.updateAppWidget(appWidgetId, views);
                            }
                        } catch (Exception ignored) {
                        }
                    }

                    @Override
                    public void onError(Exception error) {
                    }
                }, idx, false);
            } catch (Exception ignored) {
            }
        }
    }
}
