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

import android.os.Bundle;
import android.view.MenuItem;

import androidx.appcompat.widget.Toolbar;

import com.ftinc.scoop.Scoop;

import nl.hnogames.domoticz.app.AppCompatAssistActivity;
import nl.hnogames.domoticz.fragments.Graph;
import nl.hnogames.domoticz.utils.SharedPrefUtil;
import nl.hnogames.domoticz.utils.UsefulBits;

public class GraphActivity extends AppCompatAssistActivity {

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

        try {
            Bundle bundle = getIntent().getExtras();
            if (bundle == null)
                this.finish();//get graph info via bundle

            this.setTitle(getString(R.string.wizard_graph));
            if (bundle != null) {
                String title = bundle.getString("TITLE");
                if (!UsefulBits.isEmpty(title)) {
                    setTitle(title);
                }
            }

            toolbar = findViewById(R.id.toolbar);
            setSupportActionBar(toolbar);

            Graph graph = new Graph();
            if (getSupportActionBar() != null)
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportFragmentManager().beginTransaction().replace(R.id.main, graph).commit();
        } catch (Exception ex) {
            this.finish();
        }
    }

    public void setTitle(String title) {
        if (getSupportActionBar() != null)
            getSupportActionBar().setTitle(title);
    }

    public void noGraphFound() {
        this.finish();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                this.finish();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }
}