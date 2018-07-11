package com.example.lenovo.myshare;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Lenovo on 04-Jul-18.
 */

public class ChatActivity extends AppCompatActivity {


    final static int SERVER = 0;
    final static int CLIENT = 1;
    private int type;
    ListView listView;
    EditText editText;
    Button send_button;
    Button send_file;
    Button rec_file;
    List<String> messageList = new ArrayList<>();
    ArrayAdapter<String> arrayAdapter;
    Server mServer;
    Client mClient;
    FileSender fileSender;
    FileReceiver fileReceiver;
    InetAddress host_address;
    Thread sender,receiver;
    File rootdir;
    private static final String KEY = "key";

    ReadWriteThread thread;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_chat);
        Log.e("Chat","ACTIVITY");
        listView = (ListView) findViewById(R.id.listview);
        editText = (EditText) findViewById(R.id.editText);
        send_button = (Button) findViewById(R.id.send);
        send_file = (Button) findViewById(R.id.send_file);
        rec_file = (Button) findViewById(R.id.receive_file);

        rootdir = new File(Environment.getExternalStorageDirectory(),"MyShare");
        Log.e("Looking for","Folder");
        if(!rootdir.exists())
        {
            if(!rootdir.mkdirs())
            {
                Log.e("Folder","Not Created");
            }else
            {
                Log.e("Folder","Created");
            }
        }else
        {
            Log.e("Folder","Exists");
        }

        arrayAdapter = new ArrayAdapter<>(this,android.R.layout.simple_list_item_activated_1,messageList);
        listView.setAdapter(arrayAdapter);

        send_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String mmessage = editText.getText().toString();
                Log.e("Message is",mmessage);
                editText.setText("");
                editText.setHint("Enter Message ...");

                mmessage = mmessage.trim();
                if(!mmessage.equals(""))
                {
                    if(type == SERVER)
                        mmessage = "Host: " + mmessage;
                    else if(type == CLIENT)
                        mmessage = "Client: " + mmessage;

                    thread.send_message(mmessage);
                    messageList.add(mmessage);
                    arrayAdapter.notifyDataSetChanged();
                }

            }
        });

        send_file.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(receiver  == null || !receiver.isAlive()) {
                    fileSender = new FileSender(rootdir,"attachment.pdf");
                    sender = new Thread(fileSender);
                    sender.start();
                }
            }
        });

        rec_file.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.e("Receive","Clicked");
                if(sender == null || !sender.isAlive()) {
                    Log.e("Starting","Receiver");
                    fileReceiver = new FileReceiver(host_address, rootdir, "downladed.pdf");
                    receiver = new Thread(fileReceiver);
                    receiver.start();
                }
            }
        });

        Intent in = getIntent();
        type = in.getIntExtra(MainActivity.TYPE,-1);

        if(type != -1)
        {
            host_address = (InetAddress) in.getSerializableExtra(MainActivity.HOST_ADDRESS);
        }else
        {
            finish();
        }

        if(type == SERVER)
        {
            Toast.makeText(getApplicationContext(),"Host",Toast.LENGTH_SHORT).show();
            mServer = new Server();
            Thread t = new Thread(mServer);
            t.start();
        }else if(type == CLIENT)
        {
            Toast.makeText(getApplicationContext(),"Client",Toast.LENGTH_SHORT).show();
            mClient = new Client(host_address);
            Thread t = new Thread(mClient);
            t.start();
        }
    }

    Handler handler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message message) {
            Bundle bundle = message.getData();
            String msg = bundle.getString(KEY);
            messageList.add(msg);
            arrayAdapter.notifyDataSetChanged();
            return true;
        }
    });

    private class Server implements Runnable
    {
        private ServerSocket serverSocket;
        private Socket socket;

        @Override
        public void run() {
            try {
                serverSocket = new ServerSocket(2048);
                socket = serverSocket.accept();
                thread = new ReadWriteThread(socket);
                Thread t = new Thread(thread);
                t.start();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        void stop()
        {

            try {
                socket.close();
                serverSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            thread.stop();
        }
    }

    private class Client implements Runnable
    {
        private Socket socket;
        private InetAddress host_address;

        Client(InetAddress host_address) {
            this.host_address = host_address;
        }

        @Override
        public void run() {
            try {
                socket = new Socket(host_address,2048);
                thread = new ReadWriteThread(socket);
                Thread t = new Thread(thread);
                t.start();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        void stop()
        {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            thread.stop();
        }
    }


    private class ReadWriteThread implements Runnable
    {
        private DataInputStream dataInputStream;
        private DataOutputStream dataOutputStream;
        private Socket socket;
        private volatile boolean exit;
        ReadWriteThread(Socket socket) {
            try {
                this.exit = false;
                this.socket = socket;
                this.dataInputStream = new DataInputStream(this.socket.getInputStream());
                this.dataOutputStream = new DataOutputStream(this.socket.getOutputStream());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            String recmessage;

            try {

                while(!exit)
                {
                    recmessage = dataInputStream.readUTF();
                    Message message = handler.obtainMessage();
                    Bundle bundle = new Bundle();
                    bundle.putString(KEY,recmessage);
                    message.setData(bundle);
                    handler.sendMessage(message);

                    if(recmessage.equalsIgnoreCase("bye"))
                    {
                        stop();
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();

            }finally {
                try {
                    dataInputStream.close();
                    dataOutputStream.close();
                    socket.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }

        }

        void send_message(String message)
        {
            if(message != null)
            {
                try {
                    dataOutputStream.writeUTF(message);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        void stop()
        {
            this.exit = true;
        }
    }

    @Override
    public void onBackPressed() {
        if(mServer != null)
        {
            mServer.stop();
        }

        if(mClient != null)
        {
            mClient.stop();
        }

        if(fileSender != null)
        {
            fileSender.stop();
        }

        if(fileReceiver != null)
        {
            fileReceiver.stop();
        }


        Intent intent = new Intent();
        setResult(MainActivity.RETURN_CODE,intent);
        finish();
    }

    private class FileSender implements Runnable
    {
        private String filename;
        private File mFile;
        private File rootFolder;
        private ServerSocket serverSocket;
        private Socket socket;
        private FileInputStream fileInputStream;
        private BufferedInputStream bufferedInputStream;
        private DataOutputStream dataOutputStream;

        FileSender(File rootFolder, String filename) {
            this.rootFolder = rootFolder;
            this.filename = filename;
            this.mFile = new File(this.rootFolder.getAbsolutePath() + "/" + this.filename);
            if(mFile.exists())
            {
                Log.e("File","Found");
            }else
            {
                Log.e("File","NOT Found");
            }
        }

        @Override
        public void run() {
            try {
                Log.e("Connecting to","Client");
                serverSocket = new ServerSocket(2050);
                socket = serverSocket.accept();
                Log.e("Connection","Made");
                dataOutputStream = new DataOutputStream(socket.getOutputStream());
                fileInputStream = new FileInputStream(mFile);
                bufferedInputStream = new BufferedInputStream(fileInputStream);
                int len = (int)mFile.length();
                byte data[] = new byte[len];
                int l;
                while( (l = bufferedInputStream.read(data,0,len)) > 0)
                {
                    dataOutputStream.write(data,0,l);
                }
                //Toast.makeText(getApplicationContext(),"File Sent",Toast.LENGTH_LONG).show();
                Log.e("File","Sent");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        void stop()
        {
            try
            {
                dataOutputStream.close();
                bufferedInputStream.close();
                fileInputStream.close();
                socket.close();
                serverSocket.close();
            }catch(Exception e){

            }
        }
    }

    private class FileReceiver implements Runnable
    {
        InetAddress host_address;
        String filename;
        Socket socket;
        DataInputStream dataInputStream;
        File rootFolder;
        File mFile;
        FileOutputStream fileOutputStream;
        BufferedOutputStream bufferedOutputStream;
        boolean fileExists;

        FileReceiver(InetAddress host_address,File rootFolder,String filename) {
            this.host_address = host_address;
            this.filename = filename;
            this.rootFolder = rootFolder;
            this.mFile = new File(this.rootFolder.getAbsolutePath() + "/" + this.filename);

            if(!mFile.exists())
            {
                try {
                    if(mFile.createNewFile())
                    {
                        Log.e("File","Created");
                    }else
                    {
                        Log.e("File NOT","Created");
                    }
                    fileExists = false;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }else
            {
                Log.e("File","Already Exists");
                fileExists = true;
            }
        }

        @Override
        public void run() {
            try {
                Log.e("Connecting to","Server");
                socket = new Socket(host_address,2050);
                Log.e("Connection","Made");
                fileOutputStream = new FileOutputStream(mFile);
                bufferedOutputStream = new BufferedOutputStream(fileOutputStream);
                dataInputStream = new DataInputStream(socket.getInputStream());

                byte data[] = new byte[1000005];
                int l;
                while( (l = dataInputStream.read(data,0,1000005)) > 0)
                {
                    bufferedOutputStream.write(data,0,l);
                }
                Toast.makeText(getApplication(),"File Received",Toast.LENGTH_LONG).show();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        void stop()
        {

            try {
                if(bufferedOutputStream != null)
                bufferedOutputStream.close();
                if(fileOutputStream!=null)
                fileOutputStream.close();
                if(dataInputStream != null)
                dataInputStream.close();
                if(socket!=null)
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
