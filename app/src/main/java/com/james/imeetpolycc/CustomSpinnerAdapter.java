package com.james.imeetpolycc;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

public class CustomSpinnerAdapter extends ArrayAdapter<String> {
    private final Context context;
    private final String[] values;

    public CustomSpinnerAdapter(Context context, String[] values) {
        super(context, R.layout.spinner_item, values); // Use the custom layout
        this.context = context;
        this.values = values;
    }

    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {
        View view = super.getDropDownView(position, convertView, parent);
        TextView textView = view.findViewById(R.id.spinner_item_text);
        textView.setTextColor(Color.WHITE); // Set the dropdown text color to white
        return view;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = super.getView(position, convertView, parent);
        TextView textView = view.findViewById(R.id.spinner_item_text);
        textView.setTextColor(Color.WHITE); // Set the selected item text color to white
        textView.setText(values[position]);
        return view;
    }
}
