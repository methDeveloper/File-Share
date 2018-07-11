package com.example.lenovo.myshare;

import android.net.wifi.p2p.WifiP2pDevice;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Lenovo on 03-Jul-18.
 */

public class RecyclerAdapter extends RecyclerView.Adapter<RecyclerAdapter.RowHolder> {

    private MainActivity mainActivity;

    class RowHolder extends RecyclerView.ViewHolder{

        TextView textView;

        public RowHolder(View itemView) {
            super(itemView);
            this.textView = itemView.findViewById(R.id.name);
        }
    }

    private List<WifiP2pDevice> peerDevices = new ArrayList<WifiP2pDevice>();

    public RecyclerAdapter(List<WifiP2pDevice> peerDevices, MainActivity mainActivity) {
        this.peerDevices = peerDevices;
        this.mainActivity = mainActivity;
    }

    @Override
    public RowHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_row,parent,false);
        v.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int pos = mainActivity.getViewPosition(view);
                WifiP2pDevice device = peerDevices.get(pos);
                mainActivity.connect(device);
            }
        });
        RowHolder holder = new RowHolder(v);
        return holder;
    }

    @Override
    public void onBindViewHolder(RowHolder holder, int position) {
        WifiP2pDevice p2pDevice = peerDevices.get(position);
        String name = p2pDevice.deviceName;
        holder.textView.setText(name);
    }

    @Override
    public int getItemCount() {
        //Log.e("Item Count",String.valueOf(peerDevices.size()));
        return peerDevices.size();
    }

}
