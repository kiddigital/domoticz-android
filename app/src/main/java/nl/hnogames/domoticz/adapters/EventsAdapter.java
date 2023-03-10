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

package nl.hnogames.domoticz.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.switchmaterial.SwitchMaterial;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.Collections;

import nl.hnogames.domoticz.R;
import nl.hnogames.domoticz.interfaces.EventsClickListener;
import nl.hnogames.domoticz.utils.SharedPrefUtil;
import nl.hnogames.domoticzapi.Containers.EventInfo;
import nl.hnogames.domoticzapi.Domoticz;

@SuppressWarnings("unused")
public class EventsAdapter extends RecyclerView.Adapter<EventsAdapter.DataObjectHolder> {
    private static final String TAG = EventsAdapter.class.getSimpleName();
    private final EventsClickListener listener;
    private final Domoticz domoticz;
    private final SharedPrefUtil mSharedPrefs;
    private final ItemFilter mFilter = new ItemFilter();
    public Context context;
    private ArrayList<EventInfo> filteredData = null;
    private ArrayList<EventInfo> data = null;

    public EventsAdapter(Context context,
                         Domoticz mDomoticz,
                         ArrayList<EventInfo> data,
                         EventsClickListener listener) {
        super();

        this.context = context;
        this.domoticz = mDomoticz;
        this.listener = listener;
        mSharedPrefs = new SharedPrefUtil(context);
        setData(data);
    }

    public void setData(ArrayList<EventInfo> data) {
        Collections.reverse(data);

        this.data = data;
        this.filteredData = data;
    }

    public Filter getFilter() {
        return mFilter;
    }

    @Override
    public DataObjectHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.event_row_default, parent, false);

        return new DataObjectHolder(view);
    }

    @Override
    public void onBindViewHolder(final DataObjectHolder holder, int position) {

        if (filteredData != null && filteredData.size() > 0) {
            final EventInfo mEventInfo = filteredData.get(position);

            if (holder.buttonON != null) {
                holder.buttonON.setId(mEventInfo.getId());
                holder.buttonON.setEnabled(true);
                holder.buttonON.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        for (EventInfo e : data) {
                            if (e.getId() == v.getId()) {
                                listener.onEventClick(e.getId(), !e.getStatusBoolean());
                            }
                        }
                    }
                });

                holder.buttonON.setChecked(mEventInfo.getStatusBoolean());
            }

            if (holder.name != null)
                holder.name.setText(mEventInfo.getName());

            if (holder.message != null) {
                if (mEventInfo.getStatusBoolean()) {
                    holder.message.setText("Status: " + context.getString(R.string.button_state_on));
                } else {
                    holder.message.setText("Status: " + context.getString(R.string.button_state_off));
                }
            }

            Picasso.get().load(R.drawable.power).into(holder.iconRow);
        }
    }

    @Override
    public int getItemCount() {
        return filteredData.size();
    }

    public interface onClickListener {
        void onItemClick(int position, View v);
    }

    public static class DataObjectHolder extends RecyclerView.ViewHolder
            implements View.OnClickListener {
        TextView name;
        TextView message;
        SwitchMaterial buttonON;
        ImageView iconRow;

        public DataObjectHolder(View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.logs_name);
            message = itemView.findViewById(R.id.logs_message);
            iconRow = itemView.findViewById(R.id.rowIcon);
            buttonON = itemView.findViewById(R.id.switch_button);
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
        }
    }

    private class ItemFilter extends Filter {
        @Override
        protected FilterResults performFiltering(CharSequence constraint) {

            String filterString = constraint.toString().toLowerCase();

            Filter.FilterResults results = new FilterResults();

            final ArrayList<EventInfo> list = data;

            int count = list.size();
            final ArrayList<EventInfo> nlist = new ArrayList<EventInfo>(count);

            EventInfo filterableObject;

            for (int i = 0; i < count; i++) {
                filterableObject = list.get(i);
                if (filterableObject.getName().toLowerCase().contains(filterString)) {
                    nlist.add(filterableObject);
                }
            }

            results.values = nlist;
            results.count = nlist.size();

            return results;
        }

        @SuppressWarnings("unchecked")
        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            filteredData = (ArrayList<EventInfo>) results.values;
            notifyDataSetChanged();
        }
    }
}