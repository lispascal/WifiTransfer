package com.lis.pascal.wifitransfer;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import java.io.File;
import java.util.Map;


public class MainActivity extends ActionBarActivity {

    ConnectionAcceptor acceptor;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        SharedPreferences pref = getPreferences(MODE_PRIVATE);

        ((CheckBox)findViewById(R.id.checkbox_download)).setChecked(pref.getBoolean("download", true));
        ((CheckBox)findViewById(R.id.checkbox_upload)).setChecked(pref.getBoolean("upload", true));
        ((CheckBox)findViewById(R.id.checkbox_rename)).setChecked(pref.getBoolean("rename", false));
        ((CheckBox)findViewById(R.id.checkbox_deletion)).setChecked(pref.getBoolean("delete", false));
        ((CheckBox)findViewById(R.id.checkbox_toasts)).setChecked(pref.getBoolean("toasts", true));


        if(makeWifiTransferDir()) { // only if default folder exists.
            acceptor = new ConnectionAcceptor(this);
            new Thread(acceptor).start();
        }
    }

    /**
     * Makes directory for this app
     * @return True if successful or it already exists. False if unsuccessful, external storage
     * cannot be mounted, or it does not exist and could not be made.
     */
    private boolean makeWifiTransferDir() {
        if (!Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
//            System.out.println("Can't open external storage");
            return false;
        }

        File mainFolder = new File(Environment.getExternalStorageDirectory(), "WifiTransfer");
        if(!(mainFolder.exists() && mainFolder.isDirectory())) // if doesn't exist, create it.
            return mainFolder.mkdir(); //  if fail to make, return false
        return true;
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

    @Override
    public void onSaveInstanceState (Bundle outState) {
        outState.putBoolean("download", ((CheckBox)findViewById(R.id.checkbox_download)).isChecked());
        outState.putBoolean("upload", ((CheckBox)findViewById(R.id.checkbox_upload)).isChecked());
        outState.putBoolean("rename", ((CheckBox)findViewById(R.id.checkbox_rename)).isChecked());
        outState.putBoolean("delete", ((CheckBox)findViewById(R.id.checkbox_deletion)).isChecked());
        outState.putBoolean("toasts", ((CheckBox)findViewById(R.id.checkbox_toasts)).isChecked());
        super.onSaveInstanceState(outState);
    }

    protected void onPause() {
        super.onPause();

        SharedPreferences pref = getPreferences(MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();

        editor.putBoolean("download", ((CheckBox)findViewById(R.id.checkbox_download)).isChecked());
        editor.putBoolean("upload", ((CheckBox)findViewById(R.id.checkbox_upload)).isChecked());
        editor.putBoolean("rename", ((CheckBox)findViewById(R.id.checkbox_rename)).isChecked());
        editor.putBoolean("delete", ((CheckBox)findViewById(R.id.checkbox_deletion)).isChecked());
        editor.putBoolean("toasts", ((CheckBox)findViewById(R.id.checkbox_toasts)).isChecked());
        editor.commit();

    }
    protected void onStop() {
        super.onStop();
    }

    protected void onDestroy() {
        super.onDestroy();
        acceptor.stop();
//        System.out.println("thread stopped");
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
//        System.out.println(item.toString() + ":" + id);
//        System.out.println(R.id.action_settings);
        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
//            checkPassword();
            Intent i = new Intent(getApplicationContext(), SettingsActivity.class);
            startActivity(i);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
