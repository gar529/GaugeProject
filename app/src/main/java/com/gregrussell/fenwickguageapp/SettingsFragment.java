package com.gregrussell.fenwickguageapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.support.v7.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;


public class SettingsFragment extends PreferenceFragmentCompat{

    public static final String KEY_PREF_UNITS = "pref_units";

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container, Bundle onSavedInstanceState) {

        View view = inflater.inflate(R.layout.settings_layout, container, false);




        return view;
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        // Load the preferences from an XML resource
        setPreferencesFromResource(R.xml.preferences, rootKey);

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getContext());

        Uri uri = Uri.fromParts("package", getContext().getPackageName(), null);
        Log.d("settings",uri.toString());

        Preference settingsPref = (Preference)findPreference(getActivity().getResources().getString(R.string.settings_pref_key));
        settingsPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Intent intent = new Intent();
                intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                Uri uri = Uri.fromParts("package", getContext().getPackageName(), null);
                Log.d("settings",uri.toString());
                intent.setData(uri);
                startActivity(intent);

                return true;
            }
        });




    }


}