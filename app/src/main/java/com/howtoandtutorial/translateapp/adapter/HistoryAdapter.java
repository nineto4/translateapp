package com.howtoandtutorial.translateapp.adapter;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.howtoandtutorial.translateapp.Common.Common;
import com.howtoandtutorial.translateapp.MainActivity;
import com.howtoandtutorial.translateapp.R;
import com.howtoandtutorial.translateapp.model.Database;
import com.howtoandtutorial.translateapp.model.History;

import java.util.ArrayList;

/**
 * Created by Dao on 5/25/2017.
 * HistoryAdapter:
 * Adapter of History ListView
 */

public class HistoryAdapter extends ArrayAdapter<History> {
    private MainActivity mContext;
    private LayoutInflater mLayoutInflater;
    private ArrayList<History> mHistory;
    private TextView tvTextKey;
    private TextView tvTextResult;
    private ImageButton imgDelete;

    public HistoryAdapter(MainActivity context, ArrayList<History> objects) {
        super(context, 0, objects);
        mContext = context;
        mHistory = objects;
        mLayoutInflater = LayoutInflater.from(context);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        if (convertView == null) {
            convertView = mLayoutInflater.inflate(R.layout.item_history, parent, false);
        }

        final History history = mHistory.get(position);

        if(history != null){
            tvTextKey = (TextView) convertView.findViewById(R.id.tvTextKey);
            tvTextResult = (TextView) convertView.findViewById(R.id.tvTextResult);
            imgDelete = (ImageButton) convertView.findViewById(R.id.imgDelete);

            if(history.getTextKey().length() > 42){
                tvTextKey.setText(history.getTextKey().substring(0, 41)+"...");
            }else {
                tvTextKey.setText(history.getTextKey());
            }
            if(history.getTextResult().length() > 42){
                tvTextResult.setText(history.getTextResult().substring(0, 41)+"...");
            }else {
                tvTextResult.setText(history.getTextResult());
            }

            imgDelete.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mContext.deleteTableHistory(history.getId());
                }
            });
        }
        return convertView;
    }
}
