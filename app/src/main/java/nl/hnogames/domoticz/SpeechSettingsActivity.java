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

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.text.InputType;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.content.ContextCompat;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.fastaccess.permission.base.PermissionHelper;
import com.ftinc.scoop.Scoop;
import com.github.zagum.speechrecognitionview.RecognitionProgressView;
import com.github.zagum.speechrecognitionview.adapters.RecognitionListenerAdapter;
import com.google.android.material.snackbar.Snackbar;
import com.nhaarman.listviewanimations.appearance.simple.SwingBottomInAnimationAdapter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;

import nl.hnogames.domoticz.adapters.SpeechAdapter;
import nl.hnogames.domoticz.app.AppCompatPermissionsActivity;
import nl.hnogames.domoticz.containers.SpeechInfo;
import nl.hnogames.domoticz.helpers.StaticHelper;
import nl.hnogames.domoticz.interfaces.SpeechClickListener;
import nl.hnogames.domoticz.ui.SwitchDialog;
import nl.hnogames.domoticz.utils.DeviceUtils;
import nl.hnogames.domoticz.utils.PermissionsUtil;
import nl.hnogames.domoticz.utils.SharedPrefUtil;
import nl.hnogames.domoticz.utils.UsefulBits;
import nl.hnogames.domoticzapi.Containers.DevicesInfo;
import nl.hnogames.domoticzapi.DomoticzValues;
import nl.hnogames.domoticzapi.Interfaces.DevicesReceiver;


public class SpeechSettingsActivity extends AppCompatPermissionsActivity implements SpeechClickListener {

    boolean result = false;
    private SharedPrefUtil mSharedPrefs;
    private CoordinatorLayout coordinatorLayout;
    private ArrayList<SpeechInfo> SpeechList;
    private SpeechAdapter adapter;

    private SpeechRecognizer speechRecognizer;
    private RecognitionProgressView recognitionProgressView;
    private RecognitionListenerAdapter recognitionListener;
    private boolean listeningSpeechRecognition = false;
    private Toolbar toolbar;

    private PermissionHelper permissionHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        mSharedPrefs = new SharedPrefUtil(this);
        // Apply Scoop to the activity
        Scoop.getInstance().apply(this);

        if (!UsefulBits.isEmpty(mSharedPrefs.getDisplayLanguage()))
            UsefulBits.setDisplayLanguage(this, mSharedPrefs.getDisplayLanguage());
        permissionHelper = PermissionHelper.getInstance(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_speech_settings);
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        coordinatorLayout = findViewById(R.id.coordinatorLayout);

        if (getSupportActionBar() != null)
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        this.setTitle(R.string.category_Speech);

        SpeechList = mSharedPrefs.getSpeechList();
        adapter = new SpeechAdapter(this, SpeechList, this);

        createListView();

        UsefulBits.showSnackbar(this, coordinatorLayout, R.string.Speech_register, Snackbar.LENGTH_SHORT);
    }

    private void createListView() {
        ListView listView = findViewById(R.id.listView);
        SwingBottomInAnimationAdapter animationAdapter = new SwingBottomInAnimationAdapter(adapter);
        animationAdapter.setAbsListView(listView);
        listView.setAdapter(animationAdapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int item, long id) {
                showEditDialog(SpeechList.get(item));
            }
        });
        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, final int position, long id) {
                SpeechInfo speech = SpeechList.get(position);
                if (speech.getSwitchIdx() > 0) {
                    speech.setSwitchIdx(0);
                    speech.setSwitchName(null);
                    speech.setSwitchPassword(null);
                    speech.setValue(null);
                    updateSpeech(speech);
                    UsefulBits.showSnackbar(SpeechSettingsActivity.this, coordinatorLayout, R.string.switch_connection_removed, Snackbar.LENGTH_LONG);
                    adapter.notifyDataSetChanged();
                } else
                    getSwitchesAndShowSwitchesDialog(speech);
                return true;
            }
        });
    }

    private void showEditDialog(final SpeechInfo mSpeechInfo) {
        new MaterialDialog.Builder(this)
                .title(R.string.Speech_edit)
                .content(R.string.Speech_name)
                .inputType(InputType.TYPE_CLASS_TEXT)
                .negativeText(R.string.cancel)
                .input(this.getString(R.string.category_Speech), mSpeechInfo.getName(), new MaterialDialog.InputCallback() {
                    @Override
                    public void onInput(@NonNull MaterialDialog dialog, CharSequence input) {
                        if (!UsefulBits.isEmpty(String.valueOf(input))) {
                            mSpeechInfo.setName(String.valueOf(input));
                            updateSpeech(mSpeechInfo);
                        }
                    }
                }).show();
    }

    private void getSwitchesAndShowSwitchesDialog(final SpeechInfo qrInfo) {
        StaticHelper.getDomoticz(SpeechSettingsActivity.this).getDevices(new DevicesReceiver() {
            @Override

            public void onReceiveDevices(ArrayList<DevicesInfo> switches) {
                showSwitchesDialog(qrInfo, switches);
            }

            @Override

            public void onReceiveDevice(DevicesInfo mDevicesInfo) {
            }

            @Override

            public void onError(Exception error) {
                UsefulBits.showSnackbarWithAction(SpeechSettingsActivity.this, coordinatorLayout, SpeechSettingsActivity.this.getString(R.string.unable_to_get_switches), Snackbar.LENGTH_SHORT,
                        null, new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                getSwitchesAndShowSwitchesDialog(qrInfo);
                            }
                        }, SpeechSettingsActivity.this.getString(R.string.retry));
            }
        }, 0, "all");
    }

    private void showSwitchesDialog(
            final SpeechInfo SpeechInfo,
            final ArrayList<DevicesInfo> switches) {

        final ArrayList<DevicesInfo> supportedSwitches = new ArrayList<>();
        for (DevicesInfo d : switches) {
            if (DeviceUtils.isAutomatedToggableDevice(d))
                supportedSwitches.add(d);
        }

        SwitchDialog infoDialog = new SwitchDialog(
                SpeechSettingsActivity.this, supportedSwitches,
                R.layout.dialog_switch_logs,
                StaticHelper.getDomoticz(SpeechSettingsActivity.this));

        infoDialog.onDismissListener(new SwitchDialog.DismissListener() {
            @Override
            public void onDismiss(int selectedSwitchIDX, String selectedSwitchPassword, String selectedSwitchName, boolean isSceneOrGroup) {
                SpeechInfo.setSwitchIdx(selectedSwitchIDX);
                SpeechInfo.setSwitchPassword(selectedSwitchPassword);
                SpeechInfo.setSwitchName(selectedSwitchName);
                SpeechInfo.setSceneOrGroup(isSceneOrGroup);

                if (!isSceneOrGroup) {
                    for (DevicesInfo s : supportedSwitches) {
                        if (s.getIdx() == selectedSwitchIDX && s.getSwitchTypeVal() == DomoticzValues.Device.Type.Value.SELECTOR)
                            showSelectorDialog(SpeechInfo, s);
                        else
                            updateSpeech(SpeechInfo);
                    }
                } else {
                    updateSpeech(SpeechInfo);
                }
            }
        });
        infoDialog.show();
    }

    private void showSelectorDialog(final SpeechInfo SpeechInfo, DevicesInfo selector) {
        final ArrayList<String> levelNames = selector.getLevelNames();
        new MaterialDialog.Builder(this)
                .title(R.string.selector_value)
                .items(levelNames)
                .itemsCallback(new MaterialDialog.ListCallback() {
                    @Override
                    public void onSelection(MaterialDialog dialog, View view, int which, CharSequence text) {
                        SpeechInfo.setValue(String.valueOf(text));
                        updateSpeech(SpeechInfo);
                    }
                })
                .show();
    }

    public void updateSpeech(SpeechInfo SpeechInfo) {
        if (SpeechList == null)
            SpeechList = new ArrayList<>();

        boolean found = false;
        int i = 0;
        for (SpeechInfo l : SpeechList) {
            if (l.getId().equals(SpeechInfo.getId())) {
                SpeechList.set(i, SpeechInfo);
                found = true;
            }
            i++;
        }
        if (!found)//add new
            SpeechList.add(SpeechInfo);

        mSharedPrefs.saveSpeechList(SpeechList);
        adapter.data = SpeechList;
        adapter.notifyDataSetChanged();
    }

    private boolean showNoDeviceAttachedDialog(final SpeechInfo SpeechInfo) {
        new MaterialDialog.Builder(this)
                .title(R.string.noSwitchSelected_title)
                .content(getString(R.string.noSwitchSelected_explanation_Speech)
                        + UsefulBits.newLine()
                        + UsefulBits.newLine()
                        + getString(R.string.noSwitchSelected_connectOneNow))
                .positiveText(R.string.yes)
                .negativeText(R.string.no)
                .onPositive(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        getSwitchesAndShowSwitchesDialog(SpeechInfo);
                        result = true;
                    }
                })
                .show();
        return result;
    }

    @Override
    public boolean onEnableClick(SpeechInfo Speech, boolean checked) {
        if (Speech.getSwitchIdx() <= 0 && checked)
            return showNoDeviceAttachedDialog(Speech);
        else {
            Speech.setEnabled(checked);
            updateSpeech(Speech);
            return checked;
        }
    }

    @Override
    public void onRemoveClick(SpeechInfo Speech) {
        showRemoveUndoSnackbar(Speech);
    }

    private void showRemoveUndoSnackbar(final SpeechInfo SpeechInfo) {
        // remove location from list view
        removeSpeechFromListView(SpeechInfo);

        // Show snackbar with undo option
        String text = String.format(getString(R.string.something_deleted),
                getString(R.string.Speech));

        UsefulBits.showSnackbarWithAction(this, coordinatorLayout, text, Snackbar.LENGTH_SHORT, new Snackbar.Callback() {
            @Override
            public void onDismissed(Snackbar snackbar, int event) {
                super.onDismissed(snackbar, event);

                switch (event) {
                    case Snackbar.Callback.DISMISS_EVENT_TIMEOUT:
                    case Snackbar.Callback.DISMISS_EVENT_CONSECUTIVE:
                    case Snackbar.Callback.DISMISS_EVENT_MANUAL:
                    case Snackbar.Callback.DISMISS_EVENT_ACTION:
                    case Snackbar.Callback.DISMISS_EVENT_SWIPE:
                        removeSpeechFromListView(SpeechInfo);
                        break;
                }
            }
        }, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateSpeech(SpeechInfo);//undo
            }
        }, this.getString(R.string.undo));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        try {
            if (mSharedPrefs.isSpeechEnabled())
                getMenuInflater().inflate(R.menu.menu_speech, menu);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            case R.id.action_speech:
                if (speechRecognizer == null)
                    speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
                if (recognitionProgressView == null)
                    recognitionProgressView = findViewById(R.id.recognition_view);
                if (recognitionListener == null) {
                    recognitionListener = new RecognitionListenerAdapter() {
                        @Override
                        public void onResults(Bundle results) {
                            showSpeechResults(results);
                            stopRecognition();
                        }
                    };
                }

                int[] colors = {
                        ContextCompat.getColor(this, R.color.material_amber_600),
                        ContextCompat.getColor(this, R.color.material_blue_600),
                        ContextCompat.getColor(this, R.color.material_deep_purple_600),
                        ContextCompat.getColor(this, R.color.material_green_600),
                        ContextCompat.getColor(this, R.color.material_orange_600)
                };
                recognitionProgressView.setColors(colors);
                recognitionProgressView.setSpeechRecognizer(speechRecognizer);
                recognitionProgressView.setRecognitionListener(recognitionListener);
                recognitionProgressView.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        startRecognition();
                    }
                }, 50);

                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void playRecognitionAnimation() {
        findViewById(R.id.main).setVisibility(View.GONE);
        recognitionProgressView.setVisibility(View.VISIBLE);
        recognitionProgressView.play();
    }

    private void stopRecognitionAnimation() {
        findViewById(R.id.main).setVisibility(View.VISIBLE);
        recognitionProgressView.setVisibility(View.GONE);
        recognitionProgressView.stop();
    }

    private void showSpeechResults(Bundle results) {
        ArrayList<String> matches = results
                .getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
        if (matches != null)
            processResult(matches.get(0).toLowerCase());
    }

    private void startRecognition() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (PermissionsUtil.canAccessAudioState(this)) {
                Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
                intent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, getPackageName());
                intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
                intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());

                speechRecognizer.startListening(intent);
                listeningSpeechRecognition = true;
                playRecognitionAnimation();
            } else {
                permissionHelper.request(PermissionsUtil.INITIAL_AUDIO_PERMS);
            }
        } else {
            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, getPackageName());
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
            speechRecognizer.startListening(intent);

            listeningSpeechRecognition = true;
            playRecognitionAnimation();
        }
    }

    private void stopRecognition() {
        if (speechRecognizer != null) {
            speechRecognizer.stopListening();
            speechRecognizer.cancel();
            speechRecognizer.destroy();
        }
        stopRecognitionAnimation();
        listeningSpeechRecognition = false;
    }

    private void removeSpeechFromListView(SpeechInfo SpeechInfo) {
        SpeechList.remove(SpeechInfo);
        mSharedPrefs.saveSpeechList(SpeechList);

        adapter.data = SpeechList;
        adapter.notifyDataSetChanged();
    }

    /* Called when the second activity's finishes */
    protected void processResult(final String speechText) {
        boolean newTagFound = true;
        if (SpeechList != null && SpeechList.size() > 0) {
            for (SpeechInfo n : SpeechList) {
                if (n.getId().equals(speechText))
                    newTagFound = false;
            }
        }

        if (newTagFound) {
            UsefulBits.showSnackbar(SpeechSettingsActivity.this, coordinatorLayout, getString(R.string.Speech_saved) + ": " + speechText, Snackbar.LENGTH_SHORT);
            SpeechInfo SpeechInfo = new SpeechInfo();
            SpeechInfo.setId(speechText);
            SpeechInfo.setName(speechText);
            updateSpeech(SpeechInfo);
        } else {
            UsefulBits.showSnackbar(this, coordinatorLayout, R.string.Speech_exists, Snackbar.LENGTH_SHORT);
        }
    }

    @Override
    public void onBackPressed() {
        if (listeningSpeechRecognition) {
            stopRecognition();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public void onPermissionGranted(@NonNull String[] permissionName) {
        Log.i("onPermissionGranted", "Permission(s) " + Arrays.toString(permissionName) + " Granted");
        if (PermissionsUtil.canAccessAudioState(this)) {
            startRecognition();
        }
    }
}