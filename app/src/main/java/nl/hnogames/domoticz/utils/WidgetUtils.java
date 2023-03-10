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

package nl.hnogames.domoticz.utils;

import static android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_ID;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import nl.hnogames.domoticz.widgets.SecurityWidgetProvider;
import nl.hnogames.domoticz.widgets.WidgetProviderLarge;
import nl.hnogames.domoticz.widgets.WidgetProviderSmall;

public class WidgetUtils {
    public static void RefreshWidgets(Context context) {
        if (context == null)
            return;
        try {
            //refresh all widgets
            AppWidgetManager widgetManager = AppWidgetManager.getInstance(context);
            ComponentName widgetComponent = new ComponentName(context, WidgetProviderLarge.class);
            int[] appWidgetIds = widgetManager.getAppWidgetIds(widgetComponent);
            for (int appWidgetId : appWidgetIds) {
                Intent updateIntent = new Intent(context, WidgetProviderLarge.UpdateWidgetService.class);
                updateIntent.putExtra(EXTRA_APPWIDGET_ID, appWidgetId);
                updateIntent.setAction("FROM WIDGET PROVIDER");
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                    context.startForegroundService(updateIntent);
                else
                    context.startService(updateIntent);
            }

            ComponentName widgetSecurityComponent = new ComponentName(context, SecurityWidgetProvider.class);
            int[] appSecurityWidgetIds = widgetManager.getAppWidgetIds(widgetSecurityComponent);
            for (int appSecurityWidgetId : appSecurityWidgetIds) {
                Intent updateIntent = new Intent(context, SecurityWidgetProvider.UpdateSecurityWidgetService.class);
                updateIntent.putExtra(EXTRA_APPWIDGET_ID, appSecurityWidgetId);
                updateIntent.setAction("FROM WIDGET PROVIDER");
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                    context.startForegroundService(updateIntent);
                else
                    context.startService(updateIntent);
            }

            ComponentName smallwidgetComponent = new ComponentName(context, WidgetProviderSmall.class);
            int[] appSmallWidgetIds = widgetManager.getAppWidgetIds(smallwidgetComponent);
            for (int appSmallWidgetId : appSmallWidgetIds) {
                Intent updateIntent = new Intent(context, WidgetProviderSmall.UpdateWidgetService.class);
                updateIntent.putExtra(EXTRA_APPWIDGET_ID, appSmallWidgetId);
                updateIntent.setAction("FROM WIDGET PROVIDER");
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                    context.startForegroundService(updateIntent);
                else
                    context.startService(updateIntent);
            }
        } catch (Exception ex) {
        }
    }
}