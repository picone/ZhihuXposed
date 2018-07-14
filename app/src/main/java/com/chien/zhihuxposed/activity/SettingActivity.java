package com.chien.zhihuxposed.activity;

import android.annotation.SuppressLint;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.util.Log;

import com.chien.zhihuxposed.BuildConfig;
import com.chien.zhihuxposed.fragment.SettingFragment;

import java.io.File;
import java.io.IOException;

public class SettingActivity extends PreferenceActivity {

    private final static String TAG = "ZhihuXposed";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, new SettingFragment())
                .commit();
    }

    @Override
    protected void onStop() {
        super.onStop();
        setWorldReadable();// 允许所有人读取preference文件
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @SuppressLint("SetWorldReadable")
    private void setWorldReadable() {
        File f = new File(getApplicationInfo().dataDir, "shared_prefs/" + BuildConfig.APPLICATION_ID + "_preferences.xml");
        if (f.exists()) {
            f.setReadable(true, false);
            try {
                int result = Runtime.getRuntime().exec("chmod o+r " + f.getAbsolutePath()).waitFor();
                Log.i(TAG, "Preference file path:" + f.getAbsolutePath());
                Log.i(TAG, "Change file mode" + (result == 0 ? "success" : "failed"));
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        } else {
            Log.w(TAG, "File not exists");
        }
    }
}
