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

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import androidx.fragment.app.Fragment;

import com.alexvasilkov.gestures.Settings;
import com.alexvasilkov.gestures.views.GestureImageView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.squareup.picasso.Callback;
import com.squareup.picasso.MemoryPolicy;
import com.squareup.picasso.NetworkPolicy;
import com.squareup.picasso.Picasso;

import java.io.IOException;
import java.io.OutputStream;

import nl.hnogames.domoticz.R;
import nl.hnogames.domoticz.helpers.StaticHelper;
import nl.hnogames.domoticz.utils.CameraUtil;
import nl.hnogames.domoticz.utils.PicassoUtil;
import nl.hnogames.domoticz.utils.SharedPrefUtil;
import nl.hnogames.domoticzapi.Domoticz;

public class Camera extends Fragment {
    private static final int CREATE_SNAPSHOT_IMAGE = 111;
    private GestureImageView root;
    private SharedPrefUtil mSharedPrefs;
    private Picasso picasso;
    private Domoticz domoticz;
    private Context mContext;
    private int idx;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {
        RelativeLayout group = (RelativeLayout) inflater.inflate(R.layout.camera_default, null);

        root = group.findViewById(R.id.image);
        root.getController().getSettings()
                .setMaxZoom(2f)
                .setDoubleTapZoom(-1f)
                .setPanEnabled(true)
                .setZoomEnabled(true)
                .setDoubleTapEnabled(true)
                .setRotationEnabled(false)
                .setRestrictRotation(false)
                .setFillViewport(true)
                .setFitMethod(Settings.Fit.VERTICAL)
                .setGravity(Gravity.CENTER);

        FloatingActionButton fabButton = group.findViewById(R.id.fab);
        fabButton.setOnClickListener(v -> processImage());
        if (idx > 0)
            setImage(idx);
        return group;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        this.mContext = context;
        mSharedPrefs = new SharedPrefUtil(context);
        this.domoticz = StaticHelper.getDomoticz(mContext);
        picasso = new PicassoUtil().getPicasso(mContext,
                domoticz.getSessionUtil().getSessionCookie(),
                domoticz.getUserCredentials(Domoticz.Authentication.USERNAME),
                domoticz.getUserCredentials(Domoticz.Authentication.PASSWORD));
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        onAttachFragment(this);
        super.onActivityCreated(savedInstanceState);
    }

    private void processImage() {
        if (root != null) {
            Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("image/jpg");
            intent.putExtra(Intent.EXTRA_TITLE, "snapshot_share-image.jpg");
            startActivityForResult(intent, CREATE_SNAPSHOT_IMAGE);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == CREATE_SNAPSHOT_IMAGE) {
            switch (resultCode) {
                case Activity.RESULT_OK:
                    if (data != null && data.getData() != null) {
                        OutputStream outputStream;
                        try {
                            BitmapDrawable drawable = (BitmapDrawable) root.getDrawable();
                            Bitmap bitmap = drawable.getBitmap();
                            outputStream = mContext.getContentResolver().openOutputStream(data.getData());
                            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream);
                            outputStream.flush();
                            outputStream.close();

                            Intent shareIntent = new Intent();
                            shareIntent.setAction(Intent.ACTION_SEND);
                            shareIntent.putExtra(Intent.EXTRA_STREAM, data.getData());
                            shareIntent.setType("image/*");
                            startActivity(Intent.createChooser(shareIntent, "Share Image"));
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    break;
                case Activity.RESULT_CANCELED:
                    break;
            }
        }
    }

    public void setImage(int idx) {
        this.idx = idx;
        if (root != null) {
            final String imageUrl = domoticz.getSnapshotUrl(idx);
            Drawable cache = CameraUtil.getDrawable(imageUrl);
            if (cache == null) {
                picasso.load(imageUrl)
                        .noPlaceholder()
                        .networkPolicy(NetworkPolicy.NO_CACHE, NetworkPolicy.NO_STORE)
                        .into(root, new Callback() {
                            @Override
                            public void onSuccess() {
                                CameraUtil.setDrawable(imageUrl, root.getDrawable());
                            }

                            @Override
                            public void onError(Exception e) {
                            }
                        });
            } else {
                picasso.load(imageUrl)
                        .memoryPolicy(MemoryPolicy.NO_CACHE)
                        .noFade()
                        .placeholder(cache)
                        .networkPolicy(NetworkPolicy.NO_CACHE, NetworkPolicy.NO_STORE)
                        .into(root, new Callback() {
                            @Override
                            public void onSuccess() {
                                CameraUtil.setDrawable(imageUrl, root.getDrawable());
                            }

                            @Override
                            public void onError(Exception e) {
                            }
                        });
            }
        }
    }
}