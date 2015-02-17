package com.lis.pascal.wifitransfer;

import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;


public class MainActivity extends ActionBarActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ServerAcceptor acceptor = new ServerAcceptor();
        new Thread(acceptor).start();
    }

    public class ServerAcceptor implements Runnable {
        boolean stop = false;
        int port = 0;
        ServerSocket ssock;

        ServerAcceptor(){

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



            WifiManager wm = (WifiManager) getSystemService(Context.WIFI_SERVICE);
            WifiInfo wi = wm.getConnectionInfo();
            int ip = wi.getIpAddress();
            final String ipstr = (ip & 0xff) + "." + ((ip >> 8) & 0xff) + "." + ((ip >> 16) & 0xff)
                    + "." + ((ip >> 24) & 0xff) + ":" + ssock.getLocalPort();


            // changes the ip address shown in the app
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    TextView tv = (TextView) findViewById(R.id.ip);
                    tv.setText(ipstr);

                }
            });

            System.out.println("Set ip: " + ipstr);
            while(!stop)
            {
                try {
                    Socket s = ssock.accept();
                    System.out.println("sendbuffersize:" + s.getSendBufferSize());
                    System.out.println("recvbuffersize:" + s.getReceiveBufferSize());
                    SingleServer serv = new SingleServer(MainActivity.this, s, ipstr);
                    new Thread(serv).start();

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            try {
                ssock.close();
            } catch (IOException e) {
                e.printStackTrace();
                System.out.println("could not close server socket used in acceptor");
            }
        }
    }



    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
