package com.chinanetcenter.streampusherdemo.adapter;

import java.util.List;

import com.chinanetcenter.streampusherdemo.R;
import com.chinanetcenter.streampusherdemo.object.SettingItem;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

public class SettingsAdapter extends ArrayAdapter<SettingItem> {
    LayoutInflater mInflater;

    public SettingsAdapter(Context context, List<SettingItem> items) {
        super(context, R.layout.row_item_with_icon, R.id.item_text, items);
        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        TextView textView;
        if (convertView == null) {
            textView = (TextView) mInflater.inflate(R.layout.row_item_with_icon, null);
        } else {
            textView = (TextView) convertView;
        }
        if (textView != null) {
            SettingItem item = getItem(position);
            textView.setId(item.settingId);
            textView.setText(item.settingName);
            textView.setCompoundDrawablesRelativeWithIntrinsicBounds(getItem(position).iconResId, 0, 0, 0);
        }
        return textView;
    }

    @Override
    public boolean areAllItemsEnabled() {
        return false;
    }

    @Override
    public boolean isEnabled(int position) {
        return true;
    }

    @Override
    public int getCount() {
        return super.getCount();
    }

}
