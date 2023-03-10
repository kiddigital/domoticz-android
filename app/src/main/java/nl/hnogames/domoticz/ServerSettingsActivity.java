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

package nl.hnogames.domoticz;

import android.content.DialogInterface;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.NavUtils;
import androidx.fragment.app.Fragment;

import com.ftinc.scoop.Scoop;

import nl.hnogames.domoticz.app.AppCompatAssistActivity;
import nl.hnogames.domoticz.utils.SharedPrefUtil;
import nl.hnogames.domoticz.utils.UsefulBits;
import nl.hnogames.domoticz.welcome.SetupServerSettings;
import nl.hnogames.domoticz.welcome.WelcomePage3;

public class ServerSettingsActivity extends AppCompatAssistActivity {

    @SuppressWarnings("unused")
    private static final int WELCOME_WIZARD = 1;
    @SuppressWarnings("unused")
    private static final int SETTINGS = 2;

    private String updateName = "";
    private boolean addNew = false;
    private boolean activeServer = false;
    private Toolbar toolbar;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SharedPrefUtil mSharedPrefs = new SharedPrefUtil(this);
        // Apply Scoop to the activity
        Scoop.getInstance().apply(this);

        if (!UsefulBits.isEmpty(mSharedPrefs.getDisplayLanguage()))
            UsefulBits.setDisplayLanguage(this, mSharedPrefs.getDisplayLanguage());

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_graph);
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (savedInstanceState == null) {
            Bundle extras = getIntent().getExtras();
            if (extras != null) //noinspection SpellCheckingInspection
            {
                addNew = extras.getBoolean("ADDSERVER");
                updateName = extras.getString("SERVERNAME");
                activeServer = extras.getBoolean("SERVERACTIVE");
            }
        }

        if (getSupportActionBar() != null)
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        if (!addNew && UsefulBits.isEmpty(updateName)) {
            Fragment serverSettings = WelcomePage3.newInstance(SETTINGS);
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.main, serverSettings)
                    .commit();
        } else {
            SetupServerSettings serverSettings = SetupServerSettings.newInstance(SETTINGS);

            if (!UsefulBits.isEmpty(updateName)) {
                Bundle data = new Bundle();
                data.putString("SERVERNAME", updateName);
                data.putBoolean("SERVERACTIVE", activeServer);
                serverSettings.setArguments(data);
            }

            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.main, serverSettings)
                    .commit();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                if (!addNew)
                    NavUtils.navigateUpFromSameTask(this);
                else {
                    setResult(RESULT_CANCELED);
                    super.finish();
                }
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void ServerCancel() {
        setResult(RESULT_CANCELED);
        super.finish();
    }

    public void ServerAdded(Boolean added) {
        if (!added) {
            Toast.makeText(this, R.string.server_must_be_unique, Toast.LENGTH_SHORT).show();
        } else {
            setResult(RESULT_OK);
            super.finish();
        }
    }

    @Override
    public void onBackPressed() {
        if (addNew) {
            new AlertDialog.Builder(this)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setTitle(getString(R.string.dont_save_new_server))
                    .setMessage(R.string.are_you_sure)
                    .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            ServerCancel();
                        }
                    })
                    .setNegativeButton(R.string.cancel, null)
                    .show();
        } else
            super.onBackPressed();
    }
}