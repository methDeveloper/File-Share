package com.example.lenovo.myshare;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiManager;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private RecyclerAdapter recyclerAdapter;
    private LinearLayoutManager layoutManager;

    private final IntentFilter intentFilter = new IntentFilter();
    private P2PServiceReceiver serviceReceiver;
    WifiP2pManager pManager;
    WifiManager wifiManager;
    WifiP2pManager.Channel channel;
    boolean isWifiEnabled;
    boolean isWifiP2pEnabled;
    static String TYPE = "type_";
    static String HOST_ADDRESS = "host_address";
    static int REQUEST_CODE = 0;
    static int RETURN_CODE = 9;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);


        pManager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        channel = pManager.initialize(this,getMainLooper(),null); //Don't need the channel listener right now so the null param

        recyclerView = (RecyclerView) findViewById(R.id.recyclerview);
        recyclerAdapter = new RecyclerAdapter(peerDevices,this);
        layoutManager = new LinearLayoutManager(getApplicationContext());

        recyclerView.addItemDecoration(new DividerItemDecoration(recyclerView.getContext(),layoutManager.getOrientation()));

        recyclerView.setAdapter(recyclerAdapter);
        recyclerView.setLayoutManager(layoutManager);

    }

    @Override
    protected void onResume() {
        super.onResume();
        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        isWifiEnabled = wifiManager.isWifiEnabled();
        if(!isWifiEnabled)
        {
            setIsWifiP2pEnabled(false);
            enableWifiP2p();
        }

        serviceReceiver = new P2PServiceReceiver(this,pManager,channel,peerListListener,connectionInfoListener);
        registerReceiver(serviceReceiver,intentFilter);

        pManager.discoverPeers(channel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                Log.e("Peers","Discovered");
            }

            @Override
            public void onFailure(int i) {
                Log.e("Discovery","Failed");
                //Toast.makeText(getApplicationContext(),"No Devices Found: " + i,Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(serviceReceiver);
    }

    void setIsWifiP2pEnabled(boolean isWifiP2pEnabled)
    {
        this.isWifiP2pEnabled = isWifiP2pEnabled;
    }

    void enableWifiP2p()
    {
        if(!isWifiP2pEnabled)
        {
            //Toast.makeText(this,"Turning On Wifi ...",Toast.LENGTH_SHORT).show();
            enableWifi();
        }
    }

    void enableWifi()
    {
        if(!isWifiEnabled)
        {
            Log.e("Wifi","Enabled");
            wifiManager.setWifiEnabled(true);
        }
    }

    void resetDevice()
    {
        peerDevices.clear();
        recyclerAdapter.notifyDataSetChanged();
    }

    private List<WifiP2pDevice> peerDevices = new ArrayList<WifiP2pDevice>();

    private WifiP2pManager.PeerListListener peerListListener = new WifiP2pManager.PeerListListener() {
        @Override
        public void onPeersAvailable(WifiP2pDeviceList wifiP2pDeviceList) {
            List<WifiP2pDevice> newPeers = new ArrayList<>(wifiP2pDeviceList.getDeviceList());
            Log.e("Peers",String.valueOf(newPeers.size()));
            if(!newPeers.equals(peerDevices))
            {
                Log.e("Changing","Peers");
                peerDevices.clear();
                peerDevices.addAll(newPeers);
                //Notify the adpater that the list has changed
                recyclerAdapter.notifyDataSetChanged();

            }
        }
    };

    void connect(WifiP2pDevice device)
    {
        WifiP2pConfig p2pConfig = new WifiP2pConfig();

        p2pConfig.deviceAddress = device.deviceAddress;
        p2pConfig.wps.setup = WpsInfo.PBC; //Push Button Configuration

        pManager.connect(channel, p2pConfig, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                Toast.makeText(getApplicationContext(),"Connected Successfully",Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(int i) {
                Toast.makeText(getApplicationContext(),"Unable To Connect",Toast.LENGTH_SHORT).show();
            }
        });
    }

    WifiP2pManager.ConnectionInfoListener connectionInfoListener = new WifiP2pManager.ConnectionInfoListener() {
        @Override
        public void onConnectionInfoAvailable(WifiP2pInfo wifiP2pInfo) {

            final InetAddress groupOwnerAddress = wifiP2pInfo.groupOwnerAddress;

            if(wifiP2pInfo.groupFormed)
            {
                Log.e("Creating","Connection");
                Intent intent = new Intent(getApplicationContext(),ChatActivity.class);
                if(wifiP2pInfo.isGroupOwner)
                {
                    Toast.makeText(getApplicationContext(),"Connected as Host",Toast.LENGTH_LONG).show();
                    intent.putExtra(TYPE,0);
                }else {
                    Toast.makeText(getApplicationContext(), "Connected as Client", Toast.LENGTH_LONG).show();
                    intent.putExtra(TYPE,1);
                }
                intent.putExtra(HOST_ADDRESS,groupOwnerAddress);
                startActivityForResult(intent,REQUEST_CODE);
            }
        }
    };

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == REQUEST_CODE)
        {
            if(resultCode == RETURN_CODE)
            {
                disconnect();
            }
        }
    }

    private void disconnect() {
        if(pManager != null && channel != null)
        {
            pManager.removeGroup(channel, new WifiP2pManager.ActionListener() {
                @Override
                public void onSuccess() {

                }

                @Override
                public void onFailure(int i) {
                    Toast.makeText(getApplicationContext(),"Unable To Disconnect",Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    int getViewPosition(View v)
    {
        int pos = -1;
        try
        {
            pos = recyclerView.getChildAdapterPosition(v);
        }catch (Exception e)
        {
            Log.e("Error","View");
        }
        return pos;
    }
}
