package com.chien.zhihuxposed.activity;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.ActionMode;

import com.chien.zhihuxposed.BuildConfig;
import com.chien.zhihuxposed.R;
import com.chien.zhihuxposed.fragment.SettingFragment;

import java.io.File;
import java.io.IOException;

public class SettingActivity extends PreferenceActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, new SettingFragment())
                .commit();

        PreferenceManager.setDefaultValues(this, R.xml.pref, false);
    }

    @Override
    protected void onStop() {
        super.onStop();
        setWorldReadable();// 允许所有人读取preference文件
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @SuppressLint("SetWorldReadable")
    private void setWorldReadable() {
        File dataDir = new File(getApplicationInfo().dataDir);
        File prefsDir = new File(dataDir, "shared_prefs");
        File prefsFile = new File(prefsDir, BuildConfig.APPLICATION_ID + "_preferences.xml");
        if (prefsFile.exists()) {
            Runtime runtime = Runtime.getRuntime();
            for (File file : new File[]{dataDir, prefsDir, prefsFile}) {
                try {
                    runtime.exec("chmod +r " + file.getAbsolutePath());
                } catch (IOException e) {
                    Log.w("ZhihuXposed", e);
                }
            }
        }
    }
}
