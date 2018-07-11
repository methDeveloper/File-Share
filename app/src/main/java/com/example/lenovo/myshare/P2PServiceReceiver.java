package com.example.lenovo.myshare;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.NetworkInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.util.Log;
import android.widget.Toast;

/**
 * Created by Lenovo on 03-Jul-18.
 */

public class P2PServiceReceiver extends BroadcastReceiver {

    private MainActivity mainActivity;
    private WifiP2pManager p2pManager;
    private WifiP2pManager.Channel channel;
    private WifiP2pManager.PeerListListener peerListListener;
    private WifiP2pManager.ConnectionInfoListener connectionInfoListener;

    public P2PServiceReceiver(MainActivity mainActivity, WifiP2pManager p2pManager, WifiP2pManager.Channel channel,
                              WifiP2pManager.PeerListListener peerListListener, WifiP2pManager.ConnectionInfoListener con) {
        this.mainActivity = mainActivity;
        this.p2pManager = p2pManager;
        this.channel = channel;
        this.peerListListener = peerListListener;
        this.connectionInfoListener = con;
    }

    @Override
    public void onReceive(Context context, Intent intent) {

        String action = intent.getAction();

        if(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action))
        {

            if(p2pManager == null)
                return;

            NetworkInfo networkInfo =  intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);

            if(networkInfo.isConnected())
            {
                p2pManager.requestConnectionInfo(channel,connectionInfoListener);
            }else
            {
                Log.e("Network INFO","is NULL");
                Toast.makeText(mainActivity, "Disconnected",Toast.LENGTH_LONG).show();
            }

        }else if(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action))
        {
            int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE,-1);
            if(state == WifiP2pManager.WIFI_P2P_STATE_ENABLED)
            {
                mainActivity.setIsWifiP2pEnabled(true);
            }else{
                //Toast.makeText(mainActivity.getApplicationContext(),"Wifi Turned Off",Toast.LENGTH_LONG).show();
                mainActivity.setIsWifiP2pEnabled(false);
                mainActivity.enableWifiP2p();
            }

        }else if(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action))
        {

            if(p2pManager != null)
            {
                p2pManager.requestPeers(channel,peerListListener);
            }
        }else if(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action))
        {
            //mainActivity.resetDevice();
        }
    }
}
