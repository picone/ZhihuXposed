package com.chien.zhihuxposed.fragment;

import android.os.Bundle;
import android.preference.PreferenceFragment;

import com.chien.zhihuxposed.R;

public class SettingFragment extends PreferenceFragment {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.pref);
    }
}
