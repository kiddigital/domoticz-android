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

package nl.hnogames.domoticzapi.Parsers;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import nl.hnogames.domoticzapi.Containers.LoginInfo;
import nl.hnogames.domoticzapi.Interfaces.JSONParserInterface;
import nl.hnogames.domoticzapi.Interfaces.LoginReceiver;

public class LoginParser implements JSONParserInterface {

    private static final String TAG = LoginParser.class.getSimpleName();
    private LoginReceiver loginReceiver;

    public LoginParser(LoginReceiver receiver) {
        this.loginReceiver = receiver;
    }

    @Override
    public void parseResult(String result) {
        try {
            if (result == null)
                loginReceiver.OnReceive(new LoginInfo());
            else {
                JSONObject jsonObject = new JSONObject(result);
                LoginInfo info = new LoginInfo(jsonObject);
                if (info == null)
                    onError(new NullPointerException(
                            "Not logged in Domoticz."));
                else
                    loginReceiver.OnReceive(info);
            }
        } catch (Exception e) {
            Log.e(TAG, "LoginParser JSON exception");
            e.printStackTrace();
            loginReceiver.onError(e);
        }
    }

    @Override
    public void onError(Exception error) {
        Log.e(TAG, "LoginParser of JSONParserInterface exception");
        loginReceiver.onError(error);
    }
}