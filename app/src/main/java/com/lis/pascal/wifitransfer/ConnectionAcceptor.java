package com.lis.pascal.wifitransfer;

import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.widget.TextView;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashSet;

/**
* Created by Tath on 2/17/2015.
*/
public class ConnectionAcceptor implements Runnable {
    private MainActivity mainActivity;
    boolean stop = false;
    int port = 0;
    ServerSocket ssock;
    HashSet<SingleConnection> hsT;



    ConnectionAcceptor(MainActivity mainActivity){
        this.mainActivity = mainActivity;

    }

    synchronized void removeConn(SingleConnection conn)
    {
        if(hsT.contains(conn))
            hsT.remove(conn);
    }
    synchronized void addConn(SingleConnection conn)
    {
        hsT.add(conn);
    }


    public void stop() {
        stop = true;
    }

    @Override
    public void run() {
        try {
            ssock = new ServerSocket(port);
        } catch (IOException e) {
            e.printStackTrace();
        }



        WifiManager wm = (WifiManager) mainActivity.getSystemService(Context.WIFI_SERVICE);
        WifiInfo wi = wm.getConnectionInfo();
        int ip = wi.getIpAddress();
        final String ipstr = (ip & 0xff) + "." + ((ip >> 8) & 0xff) + "." + ((ip >> 16) & 0xff)
                + "." + ((ip >> 24) & 0xff) + ":" + ssock.getLocalPort();


        // changes the ip address shown in the app
        mainActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                TextView tv = (TextView) mainActivity.findViewById(R.id.ip);
                tv.setText(ipstr);
            }
        });

        System.out.println("Set ip: " + ipstr);

        HashSet<InetAddress> hs = new HashSet<>();

        hsT = new HashSet<>();

        while(!stop)
        {
            try {
                Socket s = ssock.accept();
                System.out.println("sendbuffersize:" + s.getSendBufferSize());
                System.out.println("recvbuffersize:" + s.getReceiveBufferSize());
                SingleConnection serv = new SingleConnection(mainActivity, this, s, ipstr);
                addConn(serv);
                new Thread(serv).start();

                if(!hs.contains(s.getInetAddress()))
                {
                    hs.add(s.getInetAddress());
                    mainActivity.makeToast("New connection: " + s.getInetAddress(), false);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        try {
            ssock.close();
            for(SingleConnection i : hsT)
            {
                i.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("could not close server socket used in acceptor");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
