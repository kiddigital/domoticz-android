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

package nl.hnogames.domoticz.service;

import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import com.ftinc.scoop.Scoop;

import java.util.ArrayList;

import nl.hnogames.domoticz.containers.NFCInfo;
import nl.hnogames.domoticz.helpers.StaticHelper;
import nl.hnogames.domoticz.utils.SharedPrefUtil;
import nl.hnogames.domoticz.utils.UsefulBits;
import nl.hnogames.domoticzapi.Containers.DevicesInfo;
import nl.hnogames.domoticzapi.DomoticzValues;
import nl.hnogames.domoticzapi.Interfaces.DevicesReceiver;
import nl.hnogames.domoticzapi.Interfaces.setCommandReceiver;

public class NFCServiceActivity extends AppCompatActivity {
    private final String TAG = NFCServiceActivity.class.getSimpleName();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        SharedPrefUtil mSharedPrefs = new SharedPrefUtil(this);
        Scoop.getInstance().apply(this);

        super.onCreate(savedInstanceState);
        if (mSharedPrefs.isNFCEnabled()) {
            ArrayList<NFCInfo> nfcList = mSharedPrefs.getNFCList();
            //if (getIntent().getAction().equals(NfcAdapter.ACTION_TECH_DISCOVERED)) {
            NFCInfo foundNFC = null;
            final String tagID = UsefulBits.ByteArrayToHexString(getIntent().getByteArrayExtra(NfcAdapter.EXTRA_ID));
            Log.i(TAG, "NFC ID Found: " + tagID);

            if (nfcList != null && nfcList.size() > 0) {
                for (NFCInfo n : nfcList) {
                    if (n.getId().equals(tagID))
                        foundNFC = n;
                }
            }
            if (foundNFC != null && foundNFC.isEnabled()) {
                handleSwitch(foundNFC.getSwitchIdx(), foundNFC.getSwitchPassword(), foundNFC.getValue(), foundNFC.isSceneOrGroup());
            } else {
                finish();
            }
            //}
        } else {
            Log.i(TAG, "NFC is not enabled.");
            finish();
        }
    }

    private void handleSwitch(final int idx, final String password, final String value, final boolean isSceneOrGroup) {
        StaticHelper.getDomoticz(this).getDevice(new DevicesReceiver() {
            @Override
            public void onReceiveDevices(ArrayList<DevicesInfo> mDevicesInfo) {
            }

            @Override
            public void onReceiveDevice(DevicesInfo mDevicesInfo) {
                if (mDevicesInfo == null)
                    return;

                int jsonAction;
                int jsonUrl = DomoticzValues.Json.Url.Set.SWITCHES;
                int jsonValue = 0;

                if (!isSceneOrGroup) {
                    if (mDevicesInfo.getSwitchTypeVal() == DomoticzValues.Device.Type.Value.BLINDS ||
                            mDevicesInfo.getSwitchTypeVal() == DomoticzValues.Device.Type.Value.BLINDPERCENTAGE ||
                            mDevicesInfo.getSwitchTypeVal() == DomoticzValues.Device.Type.Value.DOORLOCKINVERTED) {
                        if (!mDevicesInfo.getStatusBoolean()) {
                            jsonAction = DomoticzValues.Device.Switch.Action.OFF;
                            if (!UsefulBits.isEmpty(value)) {
                                jsonAction = DomoticzValues.Device.Dimmer.Action.DIM_LEVEL;
                                jsonValue = 0;
                            }
                        } else {
                            jsonAction = DomoticzValues.Device.Switch.Action.ON;
                            if (!UsefulBits.isEmpty(value)) {
                                jsonAction = DomoticzValues.Device.Dimmer.Action.DIM_LEVEL;
                                jsonValue = getSelectorValue(mDevicesInfo, value);
                            }
                        }
                    } else {
                        if (!mDevicesInfo.getStatusBoolean()) {
                            jsonAction = DomoticzValues.Device.Switch.Action.ON;
                            if (!UsefulBits.isEmpty(value)) {
                                jsonAction = DomoticzValues.Device.Dimmer.Action.DIM_LEVEL;
                                jsonValue = getSelectorValue(mDevicesInfo, value);
                            }
                        } else {
                            jsonAction = DomoticzValues.Device.Switch.Action.OFF;
                            if (!UsefulBits.isEmpty(value)) {
                                jsonAction = DomoticzValues.Device.Dimmer.Action.DIM_LEVEL;
                                jsonValue = 0;
                            }
                        }
                    }
                    switch (mDevicesInfo.getSwitchTypeVal()) {
                        case DomoticzValues.Device.Type.Value.PUSH_ON_BUTTON:
                            jsonAction = DomoticzValues.Device.Switch.Action.ON;
                            break;
                        case DomoticzValues.Device.Type.Value.PUSH_OFF_BUTTON:
                            jsonAction = DomoticzValues.Device.Switch.Action.OFF;
                            break;
                    }
                } else {
                    jsonUrl = DomoticzValues.Json.Url.Set.SCENES;
                    if (!mDevicesInfo.getStatusBoolean()) {
                        jsonAction = DomoticzValues.Scene.Action.ON;
                    } else
                        jsonAction = DomoticzValues.Scene.Action.OFF;

                    if (mDevicesInfo.getType().equals(DomoticzValues.Scene.Type.SCENE))
                        jsonAction = DomoticzValues.Scene.Action.ON;
                }

                StaticHelper.getDomoticz(getApplicationContext()).setAction(idx, jsonUrl, jsonAction, jsonValue, password, new setCommandReceiver() {
                    @Override

                    public void onReceiveResult(String result) {
                        if (!UsefulBits.isEmpty(result))
                            Log.d(TAG, result);
                        finish();
                    }

                    @Override

                    public void onError(Exception error) {
                        if (error != null && !UsefulBits.isEmpty(error.getMessage()))
                            Log.d(TAG, error.getMessage());
                        finish();
                    }
                });
            }

            @Override
            public void onError(Exception error) {
                if (error != null && !UsefulBits.isEmpty(error.getMessage()))
                    Log.d(TAG, error.getMessage());
                finish();
            }

        }, idx, isSceneOrGroup);
    }

    private int getSelectorValue(DevicesInfo mDevicesInfo, String value) {
        if (mDevicesInfo == null || mDevicesInfo.getLevelNames() == null)
            return 0;

        int jsonValue = 0;
        if (!UsefulBits.isEmpty(value)) {
            ArrayList<String> levelNames = mDevicesInfo.getLevelNames();
            int counter = 0;
            for (String l : levelNames) {
                if (l.equals(value))
                    break;
                else
                    counter += 10;
            }
            jsonValue = counter;
        }
        return jsonValue;
    }
}