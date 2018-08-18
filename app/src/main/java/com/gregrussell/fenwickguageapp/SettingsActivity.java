package com.gregrussell.fenwickguageapp;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.provider.Settings;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;

public class SettingsActivity extends PreferenceActivity {


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_layout);
        addPreferencesFromResource(R.xml.preferences);

        Toolbar toolbar = (Toolbar)findViewById(R.id.settings_toolbar);
        toolbar.setNavigationIcon(getResources().getDrawable(R.drawable.baseline_arrow_back_black));
        toolbar.setTitle(getResources().getString(R.string.settings_title));

        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();


            }
        });

        Preference settingsPref = (Preference)findPreference(getResources().getString(R.string.settings_pref_key));
        settingsPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Intent intent = new Intent();
                intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                Uri uri = Uri.fromParts("package", getPackageName(), null);
                Log.d("settings",uri.toString());
                intent.setData(uri);
                startActivity(intent);

                return true;
            }
        });


    }
}
