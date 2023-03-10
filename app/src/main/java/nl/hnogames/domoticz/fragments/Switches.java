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

package nl.hnogames.domoticz.fragments;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.ItemTouchHelper;

import com.afollestad.materialdialogs.MaterialDialog;
import com.fastaccess.permission.base.PermissionFragmentHelper;
import com.fastaccess.permission.base.callback.OnPermissionCallback;
import com.google.android.material.snackbar.Snackbar;
import com.skydoves.colorpickerview.ColorPickerDialog;
import com.skydoves.colorpickerview.listeners.ColorEnvelopeListener;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import nl.hnogames.domoticz.MainActivity;
import nl.hnogames.domoticz.R;
import nl.hnogames.domoticz.adapters.SwitchesAdapter;
import nl.hnogames.domoticz.app.AppController;
import nl.hnogames.domoticz.app.DomoticzRecyclerFragment;
import nl.hnogames.domoticz.helpers.MarginItemDecoration;
import nl.hnogames.domoticz.helpers.SimpleItemTouchHelperCallback;
import nl.hnogames.domoticz.helpers.StaticHelper;
import nl.hnogames.domoticz.interfaces.DomoticzFragmentListener;
import nl.hnogames.domoticz.interfaces.switchesClickListener;
import nl.hnogames.domoticz.ui.NotificationInfoDialog;
import nl.hnogames.domoticz.ui.PasswordDialog;
import nl.hnogames.domoticz.ui.RGBWWColorPickerDialog;
import nl.hnogames.domoticz.ui.SecurityPanelDialog;
import nl.hnogames.domoticz.ui.SwitchInfoDialog;
import nl.hnogames.domoticz.ui.SwitchLogInfoDialog;
import nl.hnogames.domoticz.ui.SwitchTimerInfoDialog;
import nl.hnogames.domoticz.ui.WWColorPickerDialog;
import nl.hnogames.domoticz.utils.CameraUtil;
import nl.hnogames.domoticz.utils.PermissionsUtil;
import nl.hnogames.domoticz.utils.SerializableManager;
import nl.hnogames.domoticz.utils.UsefulBits;
import nl.hnogames.domoticz.utils.WidgetUtils;
import nl.hnogames.domoticzapi.Containers.DevicesInfo;
import nl.hnogames.domoticzapi.Containers.NotificationInfo;
import nl.hnogames.domoticzapi.Containers.SwitchLogInfo;
import nl.hnogames.domoticzapi.Containers.SwitchTimerInfo;
import nl.hnogames.domoticzapi.Domoticz;
import nl.hnogames.domoticzapi.DomoticzValues;
import nl.hnogames.domoticzapi.Interfaces.DevicesReceiver;
import nl.hnogames.domoticzapi.Interfaces.NotificationReceiver;
import nl.hnogames.domoticzapi.Interfaces.SwitchLogReceiver;
import nl.hnogames.domoticzapi.Interfaces.SwitchTimerReceiver;
import nl.hnogames.domoticzapi.Interfaces.setCommandReceiver;
import nl.hnogames.domoticzapi.Utils.PhoneConnectionUtil;

public class Switches extends DomoticzRecyclerFragment implements DomoticzFragmentListener,
        switchesClickListener, OnPermissionCallback {

    @SuppressWarnings("unused")
    private static final String TAG = Switches.class.getSimpleName();
    private Context mContext;
    private SwitchesAdapter adapter;
    private ArrayList<DevicesInfo> extendedStatusSwitches;
    private Parcelable state = null;
    private boolean busy = false;
    private String filter = "";
    private boolean itemDecorationAdded = false;
    private LinearLayout lExtraPanel = null;
    private Animation animShow, animHide;
    private ItemTouchHelper mItemTouchHelper;
    private PermissionFragmentHelper permissionFragmentHelper;

    @Override
    public void onConnectionFailed() {
        new GetCachedDataTask().execute();
    }

    @Override
    public void refreshFragment() {
        if (mSwipeRefreshLayout != null)
            mSwipeRefreshLayout.setRefreshing(true);
        getSwitchesData();
    }

    @Override
    public void onDestroyView() {
        if (adapter != null)
            adapter.onDestroy();
        super.onDestroyView();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        onAttachFragment(this);
        mContext = context;
        initAnimation();
        setActionbar(getString(R.string.title_switches));
        permissionFragmentHelper = PermissionFragmentHelper.getInstance(this);
        setSortFab(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);
        lySortDevices.setVisibility(View.VISIBLE);
        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        onAttachFragment(this);
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void Filter(String text) {
        filter = text;
        try {
            if (adapter != null) {
                if (UsefulBits.isEmpty(text) &&
                        (UsefulBits.isEmpty(super.getSort()) || super.getSort().equals(mContext.getString(R.string.filterOn_all))) &&
                        mSharedPrefs.enableCustomSorting() && !mSharedPrefs.isCustomSortingLocked()) {
                    if (mItemTouchHelper == null) {
                        mItemTouchHelper = new ItemTouchHelper(new SimpleItemTouchHelperCallback(adapter, false));
                    }
                    mItemTouchHelper.attachToRecyclerView(gridView);
                } else {
                    if (mItemTouchHelper != null)
                        mItemTouchHelper.attachToRecyclerView(null);
                }
                adapter.getFilter().filter(text);
                adapter.notifyDataSetChanged();
            }
            super.Filter(text);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public void onConnectionOk() {
        super.showSpinner(true);
        getSwitchesData();
    }

    private void initAnimation() {
        animShow = AnimationUtils.loadAnimation(mContext, R.anim.enter_from_right);
        animHide = AnimationUtils.loadAnimation(mContext, R.anim.exit_to_right);
    }

    private void getSwitchesData() {
        try {
            busy = true;
            if (extendedStatusSwitches != null && extendedStatusSwitches.size() > 0) {
                state = gridView.getLayoutManager().onSaveInstanceState();
            }
            //switch toggled, refresh listview
            if (mSwipeRefreshLayout != null)
                mSwipeRefreshLayout.setRefreshing(true);
            WidgetUtils.RefreshWidgets(mContext);
            new GetCachedDataTask().execute();
        } catch (Exception ex) {
        }
    }

    // add dynamic list view
    private void createListView(ArrayList<DevicesInfo> switches) {
        if (getView() != null) {
            try {
                ArrayList<DevicesInfo> supportedSwitches = new ArrayList<>();
                final List<Integer> appSupportedSwitchesValues = StaticHelper.getDomoticz(mContext).getSupportedSwitchesValues();
                final List<String> appSupportedSwitchesNames = StaticHelper.getDomoticz(mContext).getSupportedSwitchesNames();

                for (DevicesInfo mDevicesInfo : switches) {
                    String name = mDevicesInfo.getName();
                    int switchTypeVal = mDevicesInfo.getSwitchTypeVal();
                    String switchType = mDevicesInfo.getSwitchType();

                    if (!name.startsWith(Domoticz.HIDDEN_CHARACTER) &&
                            appSupportedSwitchesValues.contains(switchTypeVal) &&
                            appSupportedSwitchesNames.contains(switchType)) {
                        if (UsefulBits.isEmpty(super.getSort()) || super.getSort().equals(mContext.getString(R.string.filterOn_all))) {
                            supportedSwitches.add(mDevicesInfo);
                        } else {
                            if (mContext != null) {
                                // UsefulBits.showSnackbar(mContext, frameLayout, mContext.getString(R.string.filter_on) + ": " + super.getSort(), Snackbar.LENGTH_SHORT);
                                if (getActivity() instanceof MainActivity)
                                    ((MainActivity) getActivity()).Talk(mContext.getString(R.string.filter_on) + ": " + super.getSort());
                                if ((super.getSort().equals(mContext.getString(R.string.filterOn_on)) && mDevicesInfo.getStatusBoolean()) &&
                                        StaticHelper.getDomoticz(mContext).isOnOffSwitch(mDevicesInfo)) {
                                    supportedSwitches.add(mDevicesInfo);
                                }
                                if ((super.getSort().equals(mContext.getString(R.string.filterOn_off)) && !mDevicesInfo.getStatusBoolean()) &&
                                        StaticHelper.getDomoticz(mContext).isOnOffSwitch(mDevicesInfo)) {
                                    supportedSwitches.add(mDevicesInfo);
                                }
                                if (super.getSort().equals(mContext.getString(R.string.filterOn_static)) &&
                                        !StaticHelper.getDomoticz(mContext).isOnOffSwitch(mDevicesInfo)) {
                                    supportedSwitches.add(mDevicesInfo);
                                }
                            }
                        }
                    }
                }

                if (adapter == null) {
                    final switchesClickListener listener = this;
                    adapter = new SwitchesAdapter(mContext, getServerUtil(), AddAdsDevice(supportedSwitches), listener);
                    gridView.setAdapter(adapter);
                } else {
                    adapter.setData(AddAdsDevice(supportedSwitches));
                    adapter.notifyDataSetChanged();
                }
                if (!isTablet && !itemDecorationAdded) {
                    gridView.addItemDecoration(new MarginItemDecoration(20));
                    itemDecorationAdded = true;
                }

                if (mItemTouchHelper == null) {
                    mItemTouchHelper = new ItemTouchHelper(new SimpleItemTouchHelperCallback(adapter, isTablet));
                }
                if ((UsefulBits.isEmpty(super.getSort()) || super.getSort().equals(mContext.getString(R.string.filterOn_all))) &&
                        mSharedPrefs.enableCustomSorting() && !mSharedPrefs.isCustomSortingLocked()) {
                    mItemTouchHelper.attachToRecyclerView(gridView);
                } else {
                    if (mItemTouchHelper != null)
                        mItemTouchHelper.attachToRecyclerView(null);
                }

                mSwipeRefreshLayout.setRefreshing(false);
                mSwipeRefreshLayout.setOnRefreshListener(() -> getSwitchesData());

                if (state != null) {
                    gridView.getLayoutManager().onRestoreInstanceState(state);
                }

                this.Filter(filter);
                busy = false;
            } catch (Exception ex) {
                errorHandling(ex);
            }
        }
        super.showSpinner(false);
    }

    private ArrayList<DevicesInfo> AddAdsDevice(ArrayList<DevicesInfo> supportedSwitches) {
        try {
            if (supportedSwitches == null || supportedSwitches.size() <= 0)
                return supportedSwitches;

            if (!AppController.IsPremiumEnabled || !mSharedPrefs.isAPKValidated()) {
                ArrayList<DevicesInfo> filteredList = new ArrayList<>();
                for (DevicesInfo d : supportedSwitches) {
                    if (d.getIdx() != MainActivity.ADS_IDX)
                        filteredList.add(d);
                }
                DevicesInfo adView = new DevicesInfo();
                adView.setIdx(MainActivity.ADS_IDX);
                adView.setName("Ads");
                adView.setType("advertisement");
                adView.setDescription("Advertisement");
                adView.setFavoriteBoolean(true);
                adView.setIsProtected(false);
                adView.setStatusBoolean(false);
                filteredList.add(1, adView);
                return filteredList;
            }
        } catch (Exception ex) {
        }
        return supportedSwitches;
    }

    private void showInfoDialog(final DevicesInfo mSwitch) {
        SwitchInfoDialog infoDialog = new SwitchInfoDialog(
                mContext,
                StaticHelper.getDomoticz(mContext),
                mSwitch,
                R.layout.dialog_switch_info);
        infoDialog.setIdx(String.valueOf(mSwitch.getIdx()));
        infoDialog.setLastUpdate(mSwitch.getLastUpdate());
        try {
            infoDialog.setColorLight(mSwitch.getSubType().startsWith(DomoticzValues.Device.SubType.Name.RGB) || mSwitch.getSubType().startsWith(DomoticzValues.Device.SubType.Name.WW));
        } catch (Exception ex) {
        }
        infoDialog.setSignalLevel(String.valueOf(mSwitch.getSignalLevel()));
        infoDialog.setBatteryLevel(String.valueOf(mSwitch.getBatteryLevel()));
        infoDialog.setIsFavorite(mSwitch.getFavoriteBoolean());
        infoDialog.show();
        infoDialog.onDismissListener((isChanged, isFavorite) -> {
            if (isChanged) changeFavorite(mSwitch, isFavorite);
        });
    }

    private void showLogDialog(ArrayList<SwitchLogInfo> switchLogs) {
        if (switchLogs.size() <= 0) {
            Toast.makeText(mContext, "No logs found.", Toast.LENGTH_LONG).show();
        } else {
            SwitchLogInfoDialog infoDialog = new SwitchLogInfoDialog(
                    mContext,
                    switchLogs,
                    R.layout.dialog_switch_logs);
            infoDialog.show();
        }
    }

    private void showTimerDialog(ArrayList<SwitchTimerInfo> switchLogs) {
        if (switchLogs.size() <= 0) {
            Toast.makeText(mContext, "No timer found.", Toast.LENGTH_LONG).show();
        } else {
            SwitchTimerInfoDialog infoDialog = new SwitchTimerInfoDialog(
                    mContext,
                    switchLogs,
                    R.layout.dialog_switch_logs);
            infoDialog.show();
        }
    }

    private void showNotificationDialog(ArrayList<NotificationInfo> notificationInfo) {
        if (notificationInfo.size() <= 0) {
            Toast.makeText(mContext, "No notifications found.", Toast.LENGTH_LONG).show();
        } else {
            NotificationInfoDialog infoDialog = new NotificationInfoDialog(
                    mContext,
                    notificationInfo);
            infoDialog.show();
        }
    }

    private void changeFavorite(final DevicesInfo mSwitch, final boolean isFavorite) {
        if (busy)
            return;

        addDebugText("changeFavorite");
        addDebugText("Set idx " + mSwitch.getIdx() + " favorite to " + isFavorite);

        if (isFavorite) {
            UsefulBits.showSnackbar(mContext, frameLayout, mSwitch.getName() + " " + mContext.getString(R.string.favorite_added), Snackbar.LENGTH_SHORT);
            if (getActivity() instanceof MainActivity)
                ((MainActivity) getActivity()).Talk(R.string.favorite_added);
        } else {
            UsefulBits.showSnackbar(mContext, frameLayout, mSwitch.getName() + " " + mContext.getString(R.string.favorite_removed), Snackbar.LENGTH_SHORT);
            if (getActivity() instanceof MainActivity)
                ((MainActivity) getActivity()).Talk(R.string.favorite_removed);
        }

        int jsonAction;
        int jsonUrl = DomoticzValues.Json.Url.Set.FAVORITE;

        if (isFavorite) jsonAction = DomoticzValues.Device.Favorite.ON;
        else jsonAction = DomoticzValues.Device.Favorite.OFF;

        StaticHelper.getDomoticz(mContext).setAction(mSwitch.getIdx(), jsonUrl, jsonAction, 0, null, new setCommandReceiver() {
            @Override

            public void onReceiveResult(String result) {
                if (result.contains("WRONG CODE")) {
                    UsefulBits.showSnackbar(mContext, frameLayout, R.string.security_wrong_code, Snackbar.LENGTH_SHORT);
                    if (getActivity() instanceof MainActivity)
                        ((MainActivity) getActivity()).Talk(R.string.security_wrong_code);
                } else {
                    successHandling(result, false);
                    mSwitch.setFavoriteBoolean(isFavorite);
                }
            }

            @Override
            public void onError(Exception error) {
                UsefulBits.showSnackbar(mContext, frameLayout, R.string.error_favorite, Snackbar.LENGTH_SHORT);
                if (getActivity() instanceof MainActivity)
                    ((MainActivity) getActivity()).Talk(R.string.error_favorite);
            }
        });
    }


    @Override
    public void onLogButtonClick(int idx) {
        StaticHelper.getDomoticz(mContext).getSwitchLogs(idx, new SwitchLogReceiver() {
            @Override

            public void onReceiveSwitches(ArrayList<SwitchLogInfo> switchesLogs) {
                showLogDialog(switchesLogs);
            }

            @Override

            public void onError(Exception error) {
                UsefulBits.showSnackbar(mContext, frameLayout, R.string.error_logs, Snackbar.LENGTH_SHORT);
                if (getActivity() instanceof MainActivity)
                    ((MainActivity) getActivity()).Talk(R.string.error_logs);
            }
        });
    }

    @Override

    public void onLikeButtonClick(int idx, boolean checked) {
        changeFavorite(getSwitch(idx), checked);
    }

    @Override
    public void onColorButtonClick(final int idx) {
        if (getSwitch(idx).getSubType().contains(DomoticzValues.Device.SubType.Name.WW) && getSwitch(idx).getSubType().contains(DomoticzValues.Device.SubType.Name.RGB)) {
            RGBWWColorPickerDialog colorDialog = new RGBWWColorPickerDialog(mContext,
                    getSwitch(idx).getIdx());
            colorDialog.show();
            colorDialog.onDismissListener(new RGBWWColorPickerDialog.DismissListener() {
                @Override
                public void onDismiss(final int value, final boolean isRGB) {
                    if (getSwitch(idx).isProtected()) {
                        PasswordDialog passwordDialog = new PasswordDialog(
                                mContext, StaticHelper.getDomoticz(mContext));
                        passwordDialog.show();
                        passwordDialog.onDismissListener(new PasswordDialog.DismissListener() {
                            @Override
                            public void onDismiss(String password) {
                                if (!isRGB)
                                    setKelvinColor(value, idx, password, true);
                                else
                                    setRGBColor(value, idx, password, true);
                            }

                            @Override
                            public void onCancel() {
                            }
                        });
                    } else {
                        if (!isRGB)
                            setKelvinColor(value, idx, null, true);
                        else
                            setRGBColor(value, idx, null, true);
                    }
                }

                @Override
                public void onChangeRGBColor(int color) {
                    if (!getSwitch(idx).isProtected())
                        setRGBColor(color, idx, null, false);
                }

                @Override
                public void onChangeKelvinColor(int kelvin) {
                    if (!getSwitch(idx).isProtected())
                        setKelvinColor(kelvin, idx, null, false);
                }
            });
        } else if (getSwitch(idx).getSubType().startsWith(DomoticzValues.Device.SubType.Name.WW)) {
            WWColorPickerDialog colorDialog = new WWColorPickerDialog(mContext,
                    getSwitch(idx).getIdx());
            colorDialog.show();
            colorDialog.onDismissListener(new WWColorPickerDialog.DismissListener() {
                @Override
                public void onDismiss(final int kelvin) {
                    if (getSwitch(idx).isProtected()) {
                        PasswordDialog passwordDialog = new PasswordDialog(
                                mContext, StaticHelper.getDomoticz(mContext));
                        passwordDialog.show();
                        passwordDialog.onDismissListener(new PasswordDialog.DismissListener() {
                            @Override
                            public void onDismiss(String password) {
                                setKelvinColor(kelvin, idx, password, true);
                            }

                            @Override
                            public void onCancel() {
                            }
                        });
                    } else
                        setKelvinColor(kelvin, idx, null, true);
                }

                @Override
                public void onChangeColor(int kelvin) {
                    if (!getSwitch(idx).isProtected())
                        setKelvinColor(kelvin, idx, null, false);
                }
            });
        } else {
            ColorPickerDialog.Builder builder = new ColorPickerDialog.Builder(getContext());
            builder.setTitle(getString(R.string.choose_color));
            builder.setPositiveButton(getString(R.string.ok), (ColorEnvelopeListener) (envelope, fromUser) -> {
                if (getSwitch(idx).isProtected()) {
                    PasswordDialog passwordDialog = new PasswordDialog(
                            mContext, StaticHelper.getDomoticz(mContext));
                    passwordDialog.show();
                    passwordDialog.onDismissListener(new PasswordDialog.DismissListener() {
                        @Override
                        public void onDismiss(String password) {
                            setRGBColor(envelope.getColor(), idx, password, true);
                        }

                        @Override
                        public void onCancel() {
                        }
                    });
                } else
                    setRGBColor(envelope.getColor(), idx, null, true);
            });
            builder.setNegativeButton(getString(R.string.cancel), (dialogInterface, i) -> dialogInterface.dismiss());
            builder.show();
        }
    }

    private void setKelvinColor(int kelvin, final int idx, final String password, final boolean selected) {
        StaticHelper.getDomoticz(mContext).setAction(idx,
                DomoticzValues.Json.Url.Set.KELVIN,
                DomoticzValues.Device.Dimmer.Action.KELVIN,
                kelvin,
                password,
                new setCommandReceiver() {
                    @Override

                    public void onReceiveResult(String result) {
                        if (result.contains("WRONG CODE")) {
                            UsefulBits.showSnackbar(mContext, frameLayout, R.string.security_wrong_code, Snackbar.LENGTH_SHORT);
                            if (getActivity() instanceof MainActivity)
                                ((MainActivity) getActivity()).Talk(R.string.security_wrong_code);
                        } else {
                            if (getSwitch(idx) == null)
                                return;
                            if (selected) {
                                UsefulBits.showSnackbar(mContext, frameLayout, mContext.getString(R.string.color_set) + ": " + getSwitch(idx).getName(), Snackbar.LENGTH_SHORT);
                                if (getActivity() instanceof MainActivity)
                                    ((MainActivity) getActivity()).Talk(R.string.color_set);
                            }
                        }
                    }

                    @Override

                    public void onError(Exception error) {
                        if (selected) {
                            if (!UsefulBits.isEmpty(password)) {
                                UsefulBits.showSnackbar(mContext, frameLayout, R.string.security_wrong_code, Snackbar.LENGTH_SHORT);
                                if (getActivity() instanceof MainActivity)
                                    ((MainActivity) getActivity()).Talk(R.string.security_wrong_code);
                            } else {
                                UsefulBits.showSnackbar(mContext, frameLayout, R.string.error_color, Snackbar.LENGTH_SHORT);
                                if (getActivity() instanceof MainActivity)
                                    ((MainActivity) getActivity()).Talk(R.string.error_color);
                            }
                        }
                    }
                });
    }

    private void setRGBColor(int selectedColor, final int idx, final String password, final boolean selected) {
        double[] hsv = UsefulBits.rgb2hsv(Color.red(selectedColor), Color.green(selectedColor), Color.blue(selectedColor));
        if (hsv == null || hsv.length <= 0)
            return;

        if (selected) {
            Log.v(TAG, "Selected HVS Color: h:" + hsv[0] + " v:" + hsv[1] + " s:" + hsv[2] + " color: " + selectedColor);
            addDebugText("Selected HVS Color: h:" + hsv[0] + " v:" + hsv[1] + " s:" + hsv[2] + " color: " + selectedColor);
        }

        boolean isWhite = false;
        long hue = Math.round(hsv[0]);
        if (selectedColor == -1) {
            isWhite = true;
        }
        StaticHelper.getDomoticz(mContext).setRGBColorAction(idx,
                DomoticzValues.Json.Url.Set.RGBCOLOR,
                hue,
                getSwitch(idx).getLevel(),
                isWhite,
                password,
                new setCommandReceiver() {
                    @Override

                    public void onReceiveResult(String result) {
                        if (getSwitch(idx) == null)
                            return;
                        if (selected) {
                            UsefulBits.showSnackbar(mContext, frameLayout, mContext.getString(R.string.color_set) + ": " + getSwitch(idx).getName(), Snackbar.LENGTH_SHORT);
                            if (getActivity() instanceof MainActivity)
                                ((MainActivity) getActivity()).Talk(R.string.color_set);
                        }
                    }

                    @Override

                    public void onError(Exception error) {
                        if (selected) {
                            if (!UsefulBits.isEmpty(password)) {
                                UsefulBits.showSnackbar(mContext, frameLayout, R.string.security_wrong_code, Snackbar.LENGTH_SHORT);
                                if (getActivity() instanceof MainActivity)
                                    ((MainActivity) getActivity()).Talk(R.string.security_wrong_code);
                            } else {
                                UsefulBits.showSnackbar(mContext, frameLayout, R.string.error_color, Snackbar.LENGTH_SHORT);
                                if (getActivity() instanceof MainActivity)
                                    ((MainActivity) getActivity()).Talk(R.string.error_color);
                            }
                        }
                    }
                });
    }

    @Override
    public void onTimerButtonClick(int idx) {
        StaticHelper.getDomoticz(mContext).getSwitchTimers(idx, new SwitchTimerReceiver() {
            @Override

            public void onReceiveSwitchTimers(ArrayList<SwitchTimerInfo> switchTimers) {
                if (switchTimers != null)
                    showTimerDialog(switchTimers);
            }

            @Override

            public void onError(Exception error) {
                UsefulBits.showSnackbar(mContext, frameLayout, R.string.error_timer, Snackbar.LENGTH_SHORT);
                if (getActivity() instanceof MainActivity)
                    ((MainActivity) getActivity()).Talk(R.string.error_timer);
            }
        }, false);
    }

    @Override
    public void onNotificationButtonClick(int idx) {
        StaticHelper.getDomoticz(mContext).getNotifications(idx, new NotificationReceiver() {
            @Override
            public void onReceiveNotifications(ArrayList<NotificationInfo> mNotificationInfos) {
                if (mNotificationInfos != null)
                    showNotificationDialog(mNotificationInfos);
            }

            @Override
            public void onError(Exception error) {
                UsefulBits.showSnackbar(mContext, frameLayout, R.string.error_notifications, Snackbar.LENGTH_SHORT);
                if (getActivity() instanceof MainActivity)
                    ((MainActivity) getActivity()).Talk(R.string.error_notifications);
            }
        });
    }

    @Override
    public void onThermostatClick(int idx) {
    }

    @Override
    public void onSetTemperatureClick(int idx) {
    }

    @Override
    public void onSecurityPanelButtonClick(int idx) {
        SecurityPanelDialog securityDialog = new SecurityPanelDialog(
                mContext, StaticHelper.getDomoticz(mContext),
                getSwitch(idx));
        securityDialog.show();

        securityDialog.onDismissListener(() -> {
            getSwitchesData();//refresh
        });
    }

    @Override
    public void onStateButtonClick(final int idx, int itemsRes, final int[] stateIds) {
        new MaterialDialog.Builder(mContext)
                .title(R.string.choose_status)
                .items(itemsRes)
                .itemsCallback((dialog, view, which, text) -> {
                    if (getSwitch(idx).isProtected()) {
                        PasswordDialog passwordDialog = new PasswordDialog(
                                mContext, StaticHelper.getDomoticz(mContext));
                        passwordDialog.show();
                        passwordDialog.onDismissListener(new PasswordDialog.DismissListener() {
                            @Override
                            public void onDismiss(String password) {
                                setState(idx, stateIds[which], password);
                            }

                            @Override
                            public void onCancel() {
                            }
                        });
                    } else
                        setState(idx, stateIds[which], null);
                })
                .show();
    }

    @Override
    public void onSelectorDimmerClick(final int idx, final String[] levelNames) {
        new MaterialDialog.Builder(mContext)
                .title(R.string.choose_status)
                .items(levelNames)
                .itemsCallback((dialog, view, which, text) -> {
                    for (int i = 0; i < levelNames.length; i++) {
                        if (levelNames[i].equals(text)) {
                            onDimmerChange(idx, i * 10, true);
                        }
                    }
                })
                .show();
    }

    @Override
    public void onSelectorChange(int idx, int level) {
        onDimmerChange(idx, level, true);
    }

    @Override
    public void onItemClicked(View v, int position) {
        LinearLayout extra_panel = v.findViewById(R.id.extra_panel);
        if (extra_panel != null) {
            if (extra_panel.getVisibility() == View.VISIBLE) {
                extra_panel.startAnimation(animHide);
                extra_panel.setVisibility(View.GONE);
            } else {
                extra_panel.setVisibility(View.VISIBLE);
                extra_panel.startAnimation(animShow);
            }

            if (extra_panel != lExtraPanel) {
                if (lExtraPanel != null) {
                    if (lExtraPanel.getVisibility() == View.VISIBLE) {
                        lExtraPanel.startAnimation(animHide);
                        lExtraPanel.setVisibility(View.GONE);
                    }
                }
            }

            lExtraPanel = extra_panel;
        }
    }

    @Override
    public boolean onItemLongClicked(int idx) {
        showInfoDialog(getSwitch(idx));
        return true;
    }

    @Override
    public void onCameraFullScreenClick(int idx, String name) {
        CameraUtil.ProcessImage(mContext, idx, name);
    }

    private void setState(final int idx, int state, final String password) {
        StaticHelper.getDomoticz(mContext).setModalAction(idx,
                state,
                1,
                password,
                new setCommandReceiver() {
                    @Override

                    public void onReceiveResult(String result) {
                        if (getSwitch(idx) == null)
                            return;
                        UsefulBits.showSnackbar(mContext, frameLayout, mContext.getString(R.string.state_set) + ": " + getSwitch(idx).getName(), Snackbar.LENGTH_SHORT);
                        if (getActivity() instanceof MainActivity)
                            ((MainActivity) getActivity()).Talk(R.string.state_set);
                        getSwitchesData();
                    }

                    @Override

                    public void onError(Exception error) {
                        if (!UsefulBits.isEmpty(password)) {
                            UsefulBits.showSnackbar(mContext, frameLayout, R.string.security_wrong_code, Snackbar.LENGTH_SHORT);
                            if (getActivity() instanceof MainActivity)
                                ((MainActivity) getActivity()).Talk(R.string.security_wrong_code);
                        } else {
                            UsefulBits.showSnackbar(mContext, frameLayout, R.string.security_no_rights, Snackbar.LENGTH_SHORT);
                            if (getActivity() instanceof MainActivity)
                                ((MainActivity) getActivity()).Talk(R.string.security_no_rights);
                        }
                    }
                });
    }

    private DevicesInfo getSwitch(int idx) {
        DevicesInfo clickedSwitch = null;
        for (DevicesInfo mDevicesInfo : extendedStatusSwitches) {
            if (mDevicesInfo.getIdx() == idx) {
                clickedSwitch = mDevicesInfo;
            }
        }
        return clickedSwitch;
    }

    @Override

    public void onSwitchClick(int idx, final boolean checked) {
        if (busy)
            return;
        addDebugText("onSwitchClick");
        addDebugText("Set idx " + idx + " to " + checked);
        final DevicesInfo clickedSwitch = getSwitch(idx);
        if (clickedSwitch.isProtected()) {
            PasswordDialog passwordDialog = new PasswordDialog(
                    mContext, StaticHelper.getDomoticz(mContext));
            passwordDialog.show();
            passwordDialog.onDismissListener(new PasswordDialog.DismissListener() {
                @Override

                public void onDismiss(String password) {
                    toggleSwitch(clickedSwitch, checked, password);
                }

                @Override
                public void onCancel() {
                }
            });
        } else {
            toggleSwitch(clickedSwitch, checked, null);
        }
    }

    private void toggleSwitch(DevicesInfo clickedSwitch, boolean checked, final String password) {
        if (checked) {
            if (getActivity() instanceof MainActivity)
                ((MainActivity) getActivity()).Talk(R.string.switch_on);
            UsefulBits.showSnackbar(mContext, frameLayout, mContext.getString(R.string.switch_on) + ": " + clickedSwitch.getName(), Snackbar.LENGTH_SHORT);
        } else {
            if (getActivity() instanceof MainActivity)
                ((MainActivity) getActivity()).Talk(R.string.switch_off);
            UsefulBits.showSnackbar(mContext, frameLayout, mContext.getString(R.string.switch_off) + ": " + clickedSwitch.getName(), Snackbar.LENGTH_SHORT);
        }
        int idx = clickedSwitch.getIdx();
        if (clickedSwitch.getIdx() > 0) {
            int jsonAction;
            int jsonUrl = DomoticzValues.Json.Url.Set.SWITCHES;
            if (clickedSwitch.getType().equals(DomoticzValues.Scene.Type.GROUP) || clickedSwitch.getType().equals(DomoticzValues.Scene.Type.SCENE)) {
                jsonUrl = DomoticzValues.Json.Url.Set.SCENES;
                if (checked) jsonAction = DomoticzValues.Scene.Action.ON;
                else jsonAction = DomoticzValues.Scene.Action.OFF;
            } else if (clickedSwitch.getSwitchTypeVal() == DomoticzValues.Device.Type.Value.BLINDS ||
                    clickedSwitch.getSwitchTypeVal() == DomoticzValues.Device.Type.Value.BLINDPERCENTAGE ||
                    clickedSwitch.getSwitchTypeVal() == DomoticzValues.Device.Type.Value.DOORLOCKINVERTED) {
                if (checked) jsonAction = DomoticzValues.Device.Switch.Action.OFF;
                else jsonAction = DomoticzValues.Device.Switch.Action.ON;
            } else {
                if (checked) jsonAction = DomoticzValues.Device.Switch.Action.ON;
                else jsonAction = DomoticzValues.Device.Switch.Action.OFF;
            }

            StaticHelper.getDomoticz(mContext).setAction(idx, jsonUrl, jsonAction, 0, password, new setCommandReceiver() {
                @Override

                public void onReceiveResult(String result) {
                    if (result.contains("WRONG CODE")) {
                        UsefulBits.showSnackbar(mContext, frameLayout, R.string.security_wrong_code, Snackbar.LENGTH_SHORT);
                        if (getActivity() instanceof MainActivity)
                            ((MainActivity) getActivity()).Talk(R.string.security_wrong_code);
                    } else {
                        successHandling(result, false);
                        getSwitchesData();
                    }
                }

                @Override

                public void onError(Exception error) {
                    if (!UsefulBits.isEmpty(password)) {
                        UsefulBits.showSnackbar(mContext, frameLayout, R.string.security_wrong_code, Snackbar.LENGTH_SHORT);
                        if (getActivity() instanceof MainActivity)
                            ((MainActivity) getActivity()).Talk(R.string.security_wrong_code);
                    } else {
                        UsefulBits.showSnackbar(mContext, frameLayout, R.string.security_no_rights, Snackbar.LENGTH_SHORT);
                        if (getActivity() instanceof MainActivity)
                            ((MainActivity) getActivity()).Talk(R.string.security_no_rights);
                    }
                }
            });
        }
    }

    @Override
    public void onButtonClick(int idx, final boolean checked) {
        if (busy)
            return;

        addDebugText("onButtonClick");
        addDebugText("Set idx " + idx + " to " + (checked ? "ON" : "OFF"));
        final DevicesInfo clickedSwitch = getSwitch(idx);
        if (clickedSwitch.isProtected()) {
            PasswordDialog passwordDialog = new PasswordDialog(
                    mContext, StaticHelper.getDomoticz(mContext));
            passwordDialog.show();
            passwordDialog.onDismissListener(new PasswordDialog.DismissListener() {
                @Override

                public void onDismiss(String password) {
                    toggleButton(clickedSwitch, checked, password);
                }

                @Override
                public void onCancel() {
                }
            });
        } else
            toggleButton(clickedSwitch, checked, null);
    }

    private void toggleButton(DevicesInfo clickedSwitch, boolean checked, final String password) {
        if (checked) {
            if (getActivity() instanceof MainActivity)
                ((MainActivity) getActivity()).Talk(R.string.switch_on);
            UsefulBits.showSnackbar(mContext, frameLayout, mContext.getString(R.string.switch_on) + ": " + clickedSwitch.getName(), Snackbar.LENGTH_SHORT);
        } else {
            if (getActivity() instanceof MainActivity)
                ((MainActivity) getActivity()).Talk(R.string.switch_off);
            UsefulBits.showSnackbar(mContext, frameLayout, mContext.getString(R.string.switch_off) + ": " + clickedSwitch.getName(), Snackbar.LENGTH_SHORT);
        }

        int idx = clickedSwitch.getIdx();
        int jsonAction;
        int jsonUrl = DomoticzValues.Json.Url.Set.SWITCHES;

        if (checked) jsonAction = DomoticzValues.Device.Switch.Action.ON;
        else jsonAction = DomoticzValues.Device.Switch.Action.OFF;

        StaticHelper.getDomoticz(mContext).setAction(idx, jsonUrl, jsonAction, 0, password, new setCommandReceiver() {
            @Override

            public void onReceiveResult(String result) {
                if (result.contains("WRONG CODE")) {
                    UsefulBits.showSnackbar(mContext, frameLayout, R.string.security_wrong_code, Snackbar.LENGTH_SHORT);
                    if (getActivity() instanceof MainActivity)
                        ((MainActivity) getActivity()).Talk(R.string.security_wrong_code);
                } else {
                    successHandling(result, false);
                    getSwitchesData();
                }
            }

            @Override

            public void onError(Exception error) {
                if (!UsefulBits.isEmpty(password)) {
                    UsefulBits.showSnackbar(mContext, frameLayout, R.string.security_wrong_code, Snackbar.LENGTH_SHORT);
                    if (getActivity() instanceof MainActivity)
                        ((MainActivity) getActivity()).Talk(R.string.security_wrong_code);
                } else {
                    UsefulBits.showSnackbar(mContext, frameLayout, R.string.security_no_rights, Snackbar.LENGTH_SHORT);
                    if (getActivity() instanceof MainActivity)
                        ((MainActivity) getActivity()).Talk(R.string.security_no_rights);
                }
            }
        });
    }

    @Override

    public void onBlindClick(final int idx, final int jsonAction) {
        if (busy)
            return;

        addDebugText("onBlindClick");
        addDebugText("Set idx " + idx + " to " + jsonAction);
        final DevicesInfo clickedSwitch = getSwitch(idx);
        if (clickedSwitch.isProtected()) {
            PasswordDialog passwordDialog = new PasswordDialog(
                    mContext, StaticHelper.getDomoticz(mContext));
            passwordDialog.show();
            passwordDialog.onDismissListener(new PasswordDialog.DismissListener() {
                @Override

                public void onDismiss(String password) {
                    setBlindState(clickedSwitch, jsonAction, password);
                }

                @Override
                public void onCancel() {
                }
            });
        } else {
            setBlindState(clickedSwitch, jsonAction, null);
        }
    }

    private void setBlindState(final DevicesInfo clickedSwitch, final int jsonAction, final String password) {
        if ((jsonAction == DomoticzValues.Device.Blind.Action.UP || jsonAction == DomoticzValues.Device.Blind.Action.OFF)) {
            UsefulBits.showSnackbar(mContext, frameLayout, mContext.getString(R.string.blind_up) + ": " + clickedSwitch.getName(), Snackbar.LENGTH_SHORT);
            if (getActivity() instanceof MainActivity)
                ((MainActivity) getActivity()).Talk(R.string.blind_up);
            clickedSwitch.setStatus(DomoticzValues.Device.Blind.State.OPEN);
        } else if ((jsonAction == DomoticzValues.Device.Blind.Action.DOWN || jsonAction == DomoticzValues.Device.Blind.Action.ON)) {
            clickedSwitch.setStatus(DomoticzValues.Device.Blind.State.CLOSED);
            UsefulBits.showSnackbar(mContext, frameLayout, mContext.getString(R.string.blind_down) + ": " + clickedSwitch.getName(), Snackbar.LENGTH_SHORT);
            if (getActivity() instanceof MainActivity)
                ((MainActivity) getActivity()).Talk(R.string.blind_down);
        } else {
            clickedSwitch.setStatus(DomoticzValues.Device.Blind.State.STOPPED);
            UsefulBits.showSnackbar(mContext, frameLayout, mContext.getString(R.string.blind_stop) + ": " + clickedSwitch.getName(), Snackbar.LENGTH_SHORT);
            if (getActivity() instanceof MainActivity)
                ((MainActivity) getActivity()).Talk(R.string.blind_stop);
        }

        int jsonUrl = DomoticzValues.Json.Url.Set.SWITCHES;
        StaticHelper.getDomoticz(mContext).setAction(clickedSwitch.getIdx(), jsonUrl, jsonAction, 0, password, new setCommandReceiver() {
            @Override

            public void onReceiveResult(String result) {
                if (result.contains("WRONG CODE")) {
                    UsefulBits.showSnackbar(mContext, frameLayout, R.string.security_wrong_code, Snackbar.LENGTH_SHORT);
                    if (getActivity() instanceof MainActivity)
                        ((MainActivity) getActivity()).Talk(R.string.security_wrong_code);
                } else {
                    successHandling(result, false);
                    getSwitchesData();
                }
            }

            @Override
            public void onError(Exception error) {
                if (!UsefulBits.isEmpty(password)) {
                    UsefulBits.showSnackbar(mContext, frameLayout, R.string.security_wrong_code, Snackbar.LENGTH_SHORT);
                    if (getActivity() instanceof MainActivity)
                        ((MainActivity) getActivity()).Talk(R.string.security_wrong_code);
                } else {
                    UsefulBits.showSnackbar(mContext, frameLayout, R.string.security_no_rights, Snackbar.LENGTH_SHORT);
                    if (getActivity() instanceof MainActivity)
                        ((MainActivity) getActivity()).Talk(R.string.security_no_rights);
                }
            }
        });
    }

    @Override
    public void onDimmerChange(int idx, final int value, final boolean selector) {
        if (busy)
            return;

        addDebugText("onDimmerChange for " + idx + " to " + value);
        final DevicesInfo clickedSwitch = getSwitch(idx);
        if (clickedSwitch.isProtected()) {
            PasswordDialog passwordDialog = new PasswordDialog(
                    mContext, StaticHelper.getDomoticz(mContext));
            passwordDialog.show();
            passwordDialog.onDismissListener(new PasswordDialog.DismissListener() {
                @Override

                public void onDismiss(String password) {
                    setDimmerState(clickedSwitch, value, selector, password);
                }

                @Override
                public void onCancel() {
                }
            });
        } else {
            setDimmerState(clickedSwitch, value, selector, null);
        }
    }

    private void setDimmerState(DevicesInfo clickedSwitch, int value, final boolean selector, final String password) {
        if (clickedSwitch != null) {
            String text = String.format(mContext.getString(R.string.set_level_switch),
                    clickedSwitch.getName(),
                    !selector ? (value) : ((value) / 10) + 1);
            UsefulBits.showSnackbar(mContext, frameLayout, text, Snackbar.LENGTH_SHORT);
            if (getActivity() instanceof MainActivity)
                ((MainActivity) getActivity()).Talk(text);
            int jsonUrl = DomoticzValues.Json.Url.Set.SWITCHES;
            int jsonAction = DomoticzValues.Device.Dimmer.Action.DIM_LEVEL;

            StaticHelper.getDomoticz(mContext).setAction(clickedSwitch.getIdx(), jsonUrl, jsonAction, !selector ? (value) : (value) + 10, password, new setCommandReceiver() {
                @Override

                public void onReceiveResult(String result) {
                    if (result.contains("WRONG CODE")) {
                        UsefulBits.showSnackbar(mContext, frameLayout, R.string.security_wrong_code, Snackbar.LENGTH_SHORT);
                        if (getActivity() instanceof MainActivity)
                            ((MainActivity) getActivity()).Talk(R.string.security_wrong_code);
                    } else {
                        successHandling(result, false);
                        if (selector)
                            getSwitchesData();
                    }
                }

                @Override

                public void onError(Exception error) {
                    if (!UsefulBits.isEmpty(password)) {
                        UsefulBits.showSnackbar(mContext, frameLayout, R.string.security_wrong_code, Snackbar.LENGTH_SHORT);
                        if (getActivity() instanceof MainActivity)
                            ((MainActivity) getActivity()).Talk(R.string.security_wrong_code);
                    } else {
                        UsefulBits.showSnackbar(mContext, frameLayout, R.string.security_no_rights, Snackbar.LENGTH_SHORT);
                        if (getActivity() instanceof MainActivity)
                            ((MainActivity) getActivity()).Talk(R.string.security_no_rights);
                    }
                }
            });
        }
    }

    @Override

    public void onPause() {
        super.onPause();
    }

    @Override

    public void errorHandling(Exception error) {
        if (error != null) {
            // Let's check if were still attached to an activity
            if (isAdded()) {
                if (mSwipeRefreshLayout != null)
                    mSwipeRefreshLayout.setRefreshing(false);
                super.errorHandling(error);
            }
        }
    }

    @Override
    public void onPermissionDeclined(@NonNull String[] permissionName) {
        Log.i("onPermissionDeclined", "Permission(s) " + Arrays.toString(permissionName) + " Declined");
        String[] neededPermission = PermissionFragmentHelper.declinedPermissions(this, PermissionsUtil.INITIAL_STORAGE_PERMS);
        StringBuilder builder = new StringBuilder(neededPermission.length);
        if (neededPermission.length > 0) {
            for (String permission : neededPermission) {
                builder.append(permission).append("\n");
            }
        }
        AlertDialog alert = PermissionsUtil.getAlertDialog(getActivity(), permissionFragmentHelper, getActivity().getString(R.string.permission_title),
                getActivity().getString(R.string.permission_desc_storage), neededPermission);
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
    }

    private class GetCachedDataTask extends AsyncTask<Boolean, Boolean, Boolean> {
        ArrayList<DevicesInfo> cacheSwitches = null;

        protected Boolean doInBackground(Boolean... geto) {
            if (mContext == null)
                return false;
            if (mPhoneConnectionUtil == null)
                mPhoneConnectionUtil = new PhoneConnectionUtil(mContext);
            if (mPhoneConnectionUtil != null && !mPhoneConnectionUtil.isNetworkAvailable()) {
                try {
                    cacheSwitches = (ArrayList<DevicesInfo>) SerializableManager.readSerializedObject(mContext, "Switches");
                    extendedStatusSwitches = cacheSwitches;
                } catch (Exception ex) {
                }
            }
            return true;
        }

        protected void onPostExecute(Boolean result) {
            if (mContext == null)
                return;
            if (cacheSwitches != null)
                createListView(cacheSwitches);

            StaticHelper.getDomoticz(mContext).getDevices(new DevicesReceiver() {
                @Override

                public void onReceiveDevices(ArrayList<DevicesInfo> switches) {
                    extendedStatusSwitches = switches;
                    SerializableManager.saveSerializable(mContext, switches, "Switches");
                    successHandling(switches.toString(), false);
                    createListView(switches);
                }

                @Override

                public void onReceiveDevice(DevicesInfo mDevicesInfo) {
                }

                @Override

                public void onError(Exception error) {
                    errorHandling(error);
                }
            }, 0, "light");
        }
    }
}