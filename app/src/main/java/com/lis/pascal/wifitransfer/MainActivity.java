package com.lis.pascal.wifitransfer;

import android.content.Context;
import android.content.Intent;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.Toast;

import java.util.Map;


public class MainActivity extends ActionBarActivity {

    ConnectionAcceptor acceptor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        acceptor = new ConnectionAcceptor(this);
        new Thread(acceptor).start();
    }



    Context getAppContext(){
        return getApplicationContext();
    }

    void makeToast(final String s, final boolean displayForLongTime) {
        final Context context = getApplicationContext();
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                CheckBox canToast = (CheckBox) findViewById(R.id.checkbox_toasts);
                if(!canToast.isChecked())
                    return;
                Toast toast;
                if (displayForLongTime)
                    toast = Toast.makeText(context, s, Toast.LENGTH_LONG);
                else
                    toast = Toast.makeText(context, s, Toast.LENGTH_SHORT);
                toast.show();
            }
        });

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    protected void onStop() {
        super.onStop();
    }

    protected void onDestroy() {
        super.onDestroy();
        acceptor.stop();
        System.out.println("thread stopped");
    }

    String getPassword(){
        return PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getString("password_value", "not found");
    }

    boolean isPasswordRequired(){
        return PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getBoolean("password_active_checkbox", true);
    }

    void checkPassword(){
        String x = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getString("password_value", "");
        System.out.println("pass=" + x);
    }

    void showPreferenceList(){
        for (Map.Entry x : PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getAll().entrySet())
            System.out.println(x.getKey() + ":" + x.getValue());
    }

    public void deauth(View view) {
        acceptor.deauth();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        System.out.println(item.toString() + ":" + id);
        System.out.println(R.id.action_settings);
        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            checkPassword();
            Intent i = new Intent(getApplicationContext(), SettingsActivity.class);
            startActivity(i);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
