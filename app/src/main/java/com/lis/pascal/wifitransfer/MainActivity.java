package com.lis.pascal.wifitransfer;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.audiofx.BassBoost;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.Toast;

import java.util.Collection;
import java.util.Map;
import java.util.Set;


public class MainActivity extends ActionBarActivity {

    ConnectionAcceptor acceptor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        acceptor = new ConnectionAcceptor(this);
        new Thread(acceptor).start();
    }

    public void onCheckboxClicked(View view) {

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
        acceptor.stop();
        System.out.println("thread stopped");
    }

    String getPassword(){
        return PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getString("password_value_checkbox", "not found");
    }

    void checkPassword(){
        String x = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getString("password_value_checkbox", "");
        System.out.println("pass=" + x);
//        Set<? extends Map.Entry<String, ?>> x = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getAll().entrySet();
//        System.out.println("prefs");
//        for(Map.Entry i : x) {
//            System.out.println(i.getKey() + ":" + i.getValue());
//        }
//        System.out.println("prefsEnd");
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
