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

package nl.hnogames.domoticz.welcome;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.wifi.ScanResult;
import android.os.Build;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.Spinner;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.AppCompatEditText;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

import com.afollestad.materialdialogs.MaterialDialog;
import com.fastaccess.permission.base.PermissionFragmentHelper;
import com.fastaccess.permission.base.callback.OnPermissionCallback;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.isupatches.wisefy.WiseFy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import nl.hnogames.domoticz.R;
import nl.hnogames.domoticz.helpers.StaticHelper;
import nl.hnogames.domoticz.ui.MultiSelectionSpinner;
import nl.hnogames.domoticz.utils.PermissionsUtil;
import nl.hnogames.domoticz.utils.SharedPrefUtil;
import nl.hnogames.domoticz.utils.UsefulBits;
import nl.hnogames.domoticzapi.DomoticzValues;

public class WelcomePage3 extends Fragment implements OnPermissionCallback {

    private static final String INSTANCE = "INSTANCE";
    private static final int WELCOME_WIZARD = 1;
    private static final int SETTINGS = 2;
    private SharedPrefUtil mSharedPrefs;

    private AppCompatEditText server_name_input, remote_server_input, remote_port_input,
            remote_username_input, remote_password_input,
            remote_directory_input, local_server_input, local_password_input,
            local_username_input, local_port_input, local_directory_input;

    private Spinner remote_protocol_spinner, local_protocol_spinner;
    private SwitchMaterial localServer_switch;
    private int remoteProtocolSelectedPosition, localProtocolSelectedPosition;
    private View v;
    private boolean hasBeenVisibleToUser = false;
    private MultiSelectionSpinner local_wifi_spinner;
    private int callingInstance;
    private PermissionFragmentHelper permissionFragmentHelper;

    public static WelcomePage3 newInstance(int instance) {
        WelcomePage3 f = new WelcomePage3();
        Bundle bdl = new Bundle(1);
        bdl.putInt(INSTANCE, instance);
        f.setArguments(bdl);
        return f;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        try {
            callingInstance = getArguments().getInt(INSTANCE);
        } catch (Exception e) {
            callingInstance = WELCOME_WIZARD;
        }
        mSharedPrefs = new SharedPrefUtil(getActivity());
        permissionFragmentHelper = PermissionFragmentHelper.getInstance(this);
        v = inflater.inflate(R.layout.fragment_welcome3, container, false);

        getLayoutReferences();
        setPreferenceValues();
        return v;
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);

        if (!isVisibleToUser) {
            if (hasBeenVisibleToUser) writePreferenceValues();
        } else hasBeenVisibleToUser = true;
    }

    private void getLayoutReferences() {
        server_name_input = v.findViewById(R.id.server_name_input);
        remote_server_input = v.findViewById(R.id.remote_server_input);
        remote_port_input = v.findViewById(R.id.remote_port_input);
        remote_username_input = v.findViewById(R.id.remote_username_input);
        remote_password_input = v.findViewById(R.id.remote_password_input);
        remote_directory_input = v.findViewById(R.id.remote_directory_input);
        remote_protocol_spinner = v.findViewById(R.id.remote_protocol_spinner);
        local_server_input = v.findViewById(R.id.local_server_input);
        local_port_input = v.findViewById(R.id.local_port_input);
        local_username_input = v.findViewById(R.id.local_username_input);
        local_password_input = v.findViewById(R.id.local_password_input);
        local_directory_input = v.findViewById(R.id.local_directory_input);
        local_protocol_spinner = v.findViewById(R.id.local_protocol_spinner);
        local_wifi_spinner = v.findViewById(R.id.local_wifi);
        CheckBox cbShowPassword = v.findViewById(R.id.showpassword);
        CheckBox cbShowPasswordLocal = v.findViewById(R.id.showpasswordlocal);
        Button btnManualSSID = v.findViewById(R.id.set_ssid);

        btnManualSSID.setOnClickListener(v -> new MaterialDialog.Builder(getContext())
                .title(R.string.welcome_ssid_button_prompt)
                .content(R.string.welcome_msg_no_ssid_found)
                .inputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD)
                .input(null, null, (dialog, input) -> {
                    Set<String> ssidFromPrefs = StaticHelper.getServerUtil(getContext()).getActiveServer().getLocalServerSsid();
                    final ArrayList<String> ssidListFromPrefs = new ArrayList<>();
                    if (ssidFromPrefs != null) {
                        if (ssidFromPrefs.size() > 0) {
                            for (String wifi : ssidFromPrefs) {
                                ssidListFromPrefs.add(wifi);
                            }
                        }
                    }
                    ssidListFromPrefs.add(String.valueOf(input));
                    StaticHelper.getServerUtil(getContext()).getActiveServer().setLocalServerSsid(ssidListFromPrefs);

                    setSsid_spinner();
                }).show());

        if (callingInstance == SETTINGS) {
            v.findViewById(R.id.server_settings_title).setVisibility(View.GONE);
        }

        final LinearLayout localServerSettingsLayout = v.findViewById(R.id.local_server_settings);
        localServer_switch = v.findViewById(R.id.localServer_switch);
        localServer_switch.setChecked(mSharedPrefs.isAdvancedSettingsEnabled());
        localServer_switch.setOnCheckedChangeListener((compoundButton, checked) -> {
            if (checked) localServerSettingsLayout.setVisibility(View.VISIBLE);
            else localServerSettingsLayout.setVisibility(View.GONE);
        });
        localServerSettingsLayout.setVisibility(StaticHelper.getServerUtil(getContext()).getActiveServer().getIsLocalServerAddressDifferent() ? View.VISIBLE : View.GONE);
        cbShowPassword.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (!isChecked) {
                remote_password_input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                remote_password_input.setSelection(remote_password_input.getText().length());
            } else {
                remote_password_input.setInputType(InputType.TYPE_CLASS_TEXT);
                remote_password_input.setSelection(remote_password_input.getText().length());
            }
        });
        cbShowPasswordLocal.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (!isChecked) {
                local_password_input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                local_password_input.setSelection(local_password_input.getText().length());
            } else {
                local_password_input.setInputType(InputType.TYPE_CLASS_TEXT);
                local_password_input.setSelection(local_password_input.getText().length());
            }
        });
    }

    private void setPreferenceValues() {
        server_name_input.setText(StaticHelper.getServerUtil(getContext()).getActiveServer().getServerName());
        remote_username_input.setText(StaticHelper.getServerUtil(getContext()).getActiveServer().getRemoteServerUsername());
        remote_password_input.setText(StaticHelper.getServerUtil(getContext()).getActiveServer().getRemoteServerPassword());
        remote_server_input.setText(StaticHelper.getServerUtil(getContext()).getActiveServer().getRemoteServerUrl());
        remote_port_input.setText(StaticHelper.getServerUtil(getContext()).getActiveServer().getRemoteServerPort());
        remote_directory_input.setText(StaticHelper.getServerUtil(getContext()).getActiveServer().getRemoteServerDirectory());
        localServer_switch.setChecked(StaticHelper.getServerUtil(getContext()).getActiveServer().getIsLocalServerAddressDifferent());
        local_username_input.setText(StaticHelper.getServerUtil(getContext()).getActiveServer().getLocalServerUsername());
        local_password_input.setText(StaticHelper.getServerUtil(getContext()).getActiveServer().getLocalServerPassword());
        local_server_input.setText(StaticHelper.getServerUtil(getContext()).getActiveServer().getLocalServerUrl());
        local_port_input.setText(StaticHelper.getServerUtil(getContext()).getActiveServer().getLocalServerPort());
        local_directory_input.setText(StaticHelper.getServerUtil(getContext()).getActiveServer().getLocalServerDirectory());

        setProtocol_spinner();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!PermissionsUtil.canAccessLocation(getActivity())) {
                permissionFragmentHelper.request(PermissionsUtil.INITIAL_LOCATION_PERMS);
            } else
                setSsid_spinner();
        } else
            setSsid_spinner();
    }

    private void setSsid_spinner() {
        try {
            Set<String> ssidFromPrefs = StaticHelper.getServerUtil(getContext()).getActiveServer().getLocalServerSsid();
            final ArrayList<String> ssidListFromPrefs = new ArrayList<>();
            //noinspection SpellCheckingInspection
            final ArrayList<String> ssids = new ArrayList<>();
            if (ssidFromPrefs != null) {
                if (ssidFromPrefs.size() > 0) {
                    for (String wifi : ssidFromPrefs) {
                        ssids.add(wifi);
                        ssidListFromPrefs.add(wifi);
                    }

                    //quickly set the values
                    local_wifi_spinner.setTitle(R.string.welcome_ssid_spinner_prompt);
                    local_wifi_spinner.setItems(ssids);
                    local_wifi_spinner.setSelection(ssidListFromPrefs);
                }
            }

            if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
                permissionFragmentHelper.request(PermissionsUtil.INITIAL_LOCATION_PERMS);
            else {
                WiseFy wisefy = new WiseFy.Brains(getActivity()).getSmarts();
                List<ScanResult> nearbyAccessPoints = wisefy.getNearbyAccessPoints(true);
                if (nearbyAccessPoints == null || nearbyAccessPoints.size() < 1) {
                    if (nearbyAccessPoints.size() <= 0) {
                        // No wifi ssid nearby found!
                        local_wifi_spinner.setEnabled(false);                       // Disable spinner
                        ssids.add(getString(R.string.welcome_msg_no_ssid_found));
                        // Set selection to the 'no ssids found' message to inform user
                        local_wifi_spinner.setItems(ssids);
                        local_wifi_spinner.setSelection(0);
                    }
                } else {
                    for (ScanResult ssid : nearbyAccessPoints) {
                        if (!UsefulBits.isEmpty(ssid.SSID) && !ssids.contains(ssid.SSID))
                            ssids.add(ssid.SSID);
                    }
                    local_wifi_spinner.setTitle(R.string.welcome_ssid_spinner_prompt);
                    local_wifi_spinner.setItems(ssids);
                    local_wifi_spinner.setSelection(ssidListFromPrefs);
                }
            }
        } catch (Exception ex) {
        }
    }

    private void setProtocol_spinner() {
        String[] protocols = getResources().getStringArray(R.array.remote_server_protocols);
        ArrayAdapter<String> protocolAdapter
                = new ArrayAdapter<>(getActivity(), R.layout.spinner_list_item, protocols);
        remote_protocol_spinner.setAdapter(protocolAdapter);
        remote_protocol_spinner.setSelection(getPrefsDomoticzRemoteSecureIndex());
        remote_protocol_spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView,
                                       View view, int position, long id) {
                remoteProtocolSelectedPosition = position;
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });

        local_protocol_spinner.setAdapter(protocolAdapter);
        local_protocol_spinner.setSelection(getPrefsDomoticzLocalSecureIndex());
        local_protocol_spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView,
                                       View view, int position, long id) {
                localProtocolSelectedPosition = position;
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });
    }

    private void writePreferenceValues() {
        StaticHelper.getServerUtil(getContext()).getActiveServer().setRemoteServerUsername(
                remote_username_input.getText().toString().trim());
        StaticHelper.getServerUtil(getContext()).getActiveServer().setRemoteServerPassword(
                remote_password_input.getText().toString().trim());
        StaticHelper.getServerUtil(getContext()).getActiveServer().setRemoteServerUrl(
                remote_server_input.getText().toString().trim());
        StaticHelper.getServerUtil(getContext()).getActiveServer().setRemoteServerPort(
                remote_port_input.getText().toString().trim());
        StaticHelper.getServerUtil(getContext()).getActiveServer().setRemoteServerDirectory(
                remote_directory_input.getText().toString().trim());
        StaticHelper.getServerUtil(getContext()).getActiveServer().setRemoteServerSecure(
                getSpinnerDomoticzRemoteSecureBoolean());
        SwitchMaterial useSameAddress = v.findViewById(R.id.localServer_switch);
        if (!useSameAddress.isChecked()) {
            StaticHelper.getServerUtil(getContext()).getActiveServer().setLocalSameAddressAsRemote();
            StaticHelper.getServerUtil(getContext()).getActiveServer().setIsLocalServerAddressDifferent(false);
        } else {
            StaticHelper.getServerUtil(getContext()).getActiveServer().setLocalServerUsername(
                    local_username_input.getText().toString().trim());
            StaticHelper.getServerUtil(getContext()).getActiveServer().setLocalServerPassword(
                    local_password_input.getText().toString().trim());
            StaticHelper.getServerUtil(getContext()).getActiveServer().setLocalServerUrl(
                    local_server_input.getText().toString().trim());
            StaticHelper.getServerUtil(getContext()).getActiveServer().setLocalServerPort(
                    local_port_input.getText().toString().trim());
            StaticHelper.getServerUtil(getContext()).getActiveServer().setLocalServerDirectory(
                    local_directory_input.getText().toString().trim());
            StaticHelper.getServerUtil(getContext()).getActiveServer().setLocalServerSecure(
                    getSpinnerDomoticzLocalSecureBoolean());
            StaticHelper.getServerUtil(getContext()).getActiveServer().setIsLocalServerAddressDifferent(true);
        }

        StaticHelper.getServerUtil(getContext()).getActiveServer().setServerName(server_name_input.getText().toString());
        StaticHelper.getServerUtil(getContext()).getActiveServer().setLocalServerSsid(local_wifi_spinner.getSelectedStrings());
        StaticHelper.getServerUtil(getContext()).saveDomoticzServers(true);
    }

    private boolean getSpinnerDomoticzRemoteSecureBoolean() {
        String[] protocols = getResources().getStringArray(R.array.remote_server_protocols);
        return protocols[remoteProtocolSelectedPosition].equalsIgnoreCase(DomoticzValues.Protocol.SECURE);
    }

    private boolean getSpinnerDomoticzLocalSecureBoolean() {
        String[] protocols = getResources().getStringArray(R.array.remote_server_protocols);
        return protocols[localProtocolSelectedPosition].equalsIgnoreCase(DomoticzValues.Protocol.SECURE);
    }

    private int getPrefsDomoticzRemoteSecureIndex() {
        boolean isSecure = StaticHelper.getServerUtil(getContext()).getActiveServer().getRemoteServerSecure();
        String[] protocols = getResources().getStringArray(R.array.remote_server_protocols);
        int i = 0;
        String protocolString;

        if (isSecure) protocolString = DomoticzValues.Protocol.SECURE;
        else protocolString = DomoticzValues.Protocol.INSECURE;

        for (String protocol : protocols) {
            if (protocol.equalsIgnoreCase(protocolString)) return i;
            i++;
        }
        return i;
    }

    private int getPrefsDomoticzLocalSecureIndex() {
        boolean isSecure = StaticHelper.getServerUtil(getContext()).getActiveServer().getLocalServerSecure();
        String[] protocols = getResources().getStringArray(R.array.remote_server_protocols);
        int i = 0;
        String protocolString;

        if (isSecure) protocolString = DomoticzValues.Protocol.SECURE;
        else protocolString = DomoticzValues.Protocol.INSECURE;

        for (String protocol : protocols) {
            if (protocol.equalsIgnoreCase(protocolString)) return i;
            i++;
        }
        return i;
    }

    @Override
    public void onPause() {
        super.onPause();
        if (callingInstance == SETTINGS) {
            writePreferenceValues();   // Only when used by settings
        }
    }

    @Override
    public void onPermissionDeclined(@NonNull String[] permissionName) {
        Log.i("onPermissionDeclined", "Permission(s) " + Arrays.toString(permissionName) + " Declined");
        String[] neededPermission = PermissionFragmentHelper.declinedPermissions(this, PermissionsUtil.INITIAL_LOCATION_PERMS);
        AlertDialog alert = PermissionsUtil.getAlertDialog(getActivity(), permissionFragmentHelper, getActivity().getString(R.string.permission_title),
                getActivity().getString(R.string.permission_desc_location), neededPermission, (dialog, which) -> {
                });
        if (!alert.isShowing()) {
            alert.show();
        }
    }

    @Override
    public void onPermissionPreGranted(@NonNull String permissionsName) {
        Log.i("onPermissionPreGranted", "Permission( " + permissionsName + " ) preGranted");
    }

    @Override
    public void onPermissionNeedExplanation(@NonNull String permissionName) {
        Log.i("NeedExplanation", "Permission( " + permissionName + " ) needs Explanation");
    }

    @Override
    public void onPermissionReallyDeclined(@NonNull String permissionName) {
        Log.i("ReallyDeclined", "Permission " + permissionName + " can only be granted from settingsScreen");
    }

    @Override
    public void onNoPermissionNeeded() {
        Log.i("onNoPermissionNeeded", "Permission(s) not needed");
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        permissionFragmentHelper.onActivityForResult(requestCode);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        permissionFragmentHelper.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    public void onPermissionGranted(@NonNull String[] permissionName) {
        Log.i("onPermissionGranted", "Permission(s) " + Arrays.toString(permissionName) + " Granted");
        if (PermissionsUtil.canAccessLocation(getActivity()))
            setSsid_spinner();
    }
}