package com.intel.yamba;

import android.app.ActionBar;
import android.app.Activity;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;


public class MainActivity extends Activity implements StatusFragment.StatusRequestCallback {

    private static final String         STATUS_FRAG = "STATUS";
    private static final String         SETTINGS_FRAG = "SETTINGS";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (savedInstanceState == null) {
            FragmentManager fm = getFragmentManager();
            FragmentTransaction ft = fm.beginTransaction();
            StatusFragment frag = StatusFragment.createInstance();
            ft.add(R.id.frag_container, frag, STATUS_FRAG);
            ft.commit();
        }

        ActionBar ab = getActionBar();
//        ab.setHomeButtonEnabled(true);
        ab.setDisplayHomeAsUpEnabled(true);
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_status, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            startSettingsFragment();
            return true;
        }

        if (id == android.R.id.home) {
            onBackPressed();
        }
        return super.onOptionsItemSelected(item);
    }

    private void startSettingsFragment() {
        FragmentManager fm = getFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();
        SettingsFragment frag = SettingsFragment.createInstance();
        ft.replace(R.id.frag_container, frag, SETTINGS_FRAG);
        ft.addToBackStack(SETTINGS_FRAG);
        ft.commit();
    }


    @Override
    public void needSettings() {
        startSettingsFragment();
    }
}
