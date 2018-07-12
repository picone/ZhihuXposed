package com.chien.zhihuxposed.activity;

import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

import com.chien.zhihuxposed.R;
import com.chien.zhihuxposed.fragment.SettingFragment;

public class SettingActivity extends PreferenceActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, new SettingFragment())
                .commit();

        PreferenceManager.setDefaultValues(this, R.xml.pref, false);
    }
}
