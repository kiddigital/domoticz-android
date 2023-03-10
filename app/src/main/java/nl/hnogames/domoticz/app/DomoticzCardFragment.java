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

package nl.hnogames.domoticz.app;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;

import java.util.List;

import nl.hnogames.domoticz.MainActivity;
import nl.hnogames.domoticz.R;
import nl.hnogames.domoticz.helpers.StaticHelper;
import nl.hnogames.domoticz.interfaces.DomoticzFragmentListener;
import nl.hnogames.domoticz.utils.SharedPrefUtil;
import nl.hnogames.domoticz.utils.UsefulBits;
import nl.hnogames.domoticzapi.DomoticzValues;
import nl.hnogames.domoticzapi.Utils.PhoneConnectionUtil;
import nl.hnogames.domoticzapi.Utils.ServerUtil;

public class DomoticzCardFragment extends Fragment {
    public View frameLayout;
    public PhoneConnectionUtil mPhoneConnectionUtil;
    public SwipeRefreshLayout mSwipeRefreshLayout;
    public MaterialButton btnCheckSettings;
    private DomoticzFragmentListener listener;
    private String fragmentName;
    private SharedPrefUtil mSharedPrefs;
    private TextView debugText;
    private boolean debug;
    private ViewGroup root;

    public DomoticzCardFragment() {
    }

    public void refreshFragment() {
    }

    public void setActionbar(String title) {
        if (getActivity() instanceof MainActivity)
            ((MainActivity) getActivity()).setActionbar(title);
    }

    public void setSortFab(boolean visible) {
        if (getActivity() instanceof MainActivity) {
            if (((MainActivity) getActivity()).fabSort != null)
                ((MainActivity) getActivity()).fabSort.setVisibility(visible ? View.VISIBLE : View.GONE);
        }
    }

    public void setTheme() {
        if (mSharedPrefs == null)
            mSharedPrefs = new SharedPrefUtil(getActivity());
    }

    public ServerUtil getServerUtil() {
        return StaticHelper.getServerUtil(getContext());
    }

    @Override
    public View onCreateView(LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {
        root = (ViewGroup) inflater.inflate(R.layout.fragment_cameras, null);
        if (getActivity() instanceof MainActivity)
            frameLayout = ((MainActivity) getActivity()).frameLayout;
        mSwipeRefreshLayout = root.findViewById(R.id.swipe_refresh_layout);
        btnCheckSettings = root.findViewById(R.id.btnCheckSettings);
        if (btnCheckSettings != null) {
            btnCheckSettings.setOnClickListener(v -> {
                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).OpenSettings();
                }
            });
        }
        setTheme();
        return root;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        debug = mSharedPrefs.isDebugEnabled();

        if (debug) showDebugLayout();

        checkConnection();
    }

    /**
     * Connects to the attached fragment to cast the DomoticzFragmentListener to.
     * Throws ClassCastException if the fragment does not implement the DomoticzFragmentListener
     *
     * @param fragment fragment to cast the DomoticzFragmentListener to
     */
    public void onAttachFragment(Fragment fragment) {

        fragmentName = fragment.toString();

        try {
            listener = (DomoticzFragmentListener) fragment;
        } catch (ClassCastException e) {
            throw new ClassCastException(
                    fragment + " must implement DomoticzFragmentListener");
        }
    }

    /**
     * Checks for a active connection
     */
    public void checkConnection() {
        if (listener == null) {
            List<Fragment> fragments = getFragmentManager().getFragments();
            onAttachFragment(fragments.get(0) != null ? fragments.get(0) : fragments.get(1));
        }

        mPhoneConnectionUtil = new PhoneConnectionUtil(getContext());
        if (mPhoneConnectionUtil.isNetworkAvailable()) {
            addDebugText("Connection OK");
            listener.onConnectionOk();
        } else {
            listener.onConnectionFailed();
            setErrorMessage(getString(R.string.error_notConnected));
        }
    }

    /**
     * Handles the success messages
     *
     * @param result Result text to handle
     */
    public void successHandling(String result, boolean displayToast) {
        if (result.equalsIgnoreCase(DomoticzValues.Result.ERROR))
            Toast.makeText(getActivity(), R.string.action_failed, Toast.LENGTH_SHORT).show();
        else if (result.equalsIgnoreCase(DomoticzValues.Result.OK)) {
            if (displayToast)
                Toast.makeText(getActivity(), R.string.action_success, Toast.LENGTH_SHORT).show();
        } else {
            if (displayToast)
                Toast.makeText(getActivity(), R.string.action_unknown, Toast.LENGTH_SHORT).show();
        }
        if (debug) addDebugText("- Result: " + result);
    }

    /**
     * Handles the error messages
     *
     * @param error Exception
     */
    public void errorHandling(Exception error, View frameLayout) {
        String errorMessage = StaticHelper.getDomoticz(getActivity()).getErrorMessage(error);
        if (mPhoneConnectionUtil == null)
            mPhoneConnectionUtil = new PhoneConnectionUtil(getContext());

        if (mPhoneConnectionUtil.isNetworkAvailable()) {
            if (errorMessage.contains("No value for result")) {
                setMessage(getString(R.string.no_data_on_domoticz));
            } else {
                setErrorMessage(errorMessage);
            }
        } else {
            if (frameLayout != null) {
                UsefulBits.showSnackbar(getContext(), frameLayout, R.string.error_notConnected, Snackbar.LENGTH_SHORT);
                if (getActivity() instanceof MainActivity)
                    ((MainActivity) getActivity()).Talk(R.string.error_notConnected);
            }
        }
    }

    public ActionBar getActionBar() {
        return ((AppCompatActivity) getActivity()).getSupportActionBar();
    }

    private void setErrorMessage(String message) {
        if (debug) addDebugText(message);
        else {
            Logger(fragmentName, message);
            setErrorLayoutMessage(message);
        }
    }

    public void addDebugText(String text) {
        Logger(fragmentName, text);

        if (debug) {
            if (debugText != null) {
                String temp = debugText.getText().toString();
                if (temp.isEmpty() || temp.equals("")) debugText.setText(text);
                else {
                    temp = temp + "\n";
                    temp = temp + text;
                    debugText.setText(temp);
                }
            } else throw new RuntimeException(
                    "Layout should have a TextView defined with the ID \"debugText\"");
        }
    }

    private void setErrorLayoutMessage(String message) {
        if (mPhoneConnectionUtil.isNetworkAvailable()) {
            RelativeLayout errorLayout = root.findViewById(R.id.errorLayout);
            if (errorLayout != null) {
                errorLayout.setVisibility(View.VISIBLE);
                TextView errorTextMessage = root.findViewById(R.id.errorTextMessage);
                errorTextMessage.setText(message);
            } else throw new RuntimeException(
                    "Layout should have a RelativeLayout defined with the ID of errorLayout");
        }
    }

    private void setMessage(String message) {
        RelativeLayout errorLayout = root.findViewById(R.id.errorLayout);
        if (errorLayout != null) {
            errorLayout.setVisibility(View.VISIBLE);

            MaterialButton settingsButton = root.findViewById(R.id.btnCheckSettings);
            settingsButton.setVisibility(View.GONE);

            ImageView errorImage = root.findViewById(R.id.errorImage);
            errorImage.setVisibility(View.VISIBLE);

            TextView errorTextWrong = root.findViewById(R.id.errorTextWrong);
            errorTextWrong.setVisibility(View.GONE);

            TextView errorTextMessage = root.findViewById(R.id.errorTextMessage);
            errorTextMessage.setText(message);
        } else throw new RuntimeException(
                "Layout should have a RelativeLayout defined with the ID of errorLayout");
    }

    private void hideRecyclerView() {
        RecyclerView recyclerView = root.findViewById(R.id.my_recycler_view);
        if (recyclerView != null) {
            recyclerView.setVisibility(View.GONE);
        }
    }

    private void showDebugLayout() {
        try {
            if (root != null) {
                LinearLayout debugLayout = root.findViewById(R.id.debugLayout);
                if (debugLayout != null) {
                    debugLayout.setVisibility(View.VISIBLE);
                    debugText = root.findViewById(R.id.debugText);
                }
            }
        } catch (Exception ex) {
        }
    }

    public void Logger(String tag, String text) {
        if (!UsefulBits.isEmpty(tag) && !UsefulBits.isEmpty(text))
            Log.d(tag, text);
    }

    public void Filter(String text) {
    }
}