package com.chien.zhihuxposed.utils;

import com.chien.zhihuxposed.BuildConfig;

import de.robv.android.xposed.XSharedPreferences;

public class PreferenceUtils {
    private static XSharedPreferences instance = null;

    private static XSharedPreferences getInstance() {
        if (instance == null) {
            instance = new XSharedPreferences(BuildConfig.APPLICATION_ID);
        } else {
            instance.reload();
        }
        return instance;
    }

    public static boolean disableFeedAdvert() {
        return getInstance().getBoolean("DISABLE_FEED_ADVERT", true);
    }

    public static boolean disableMarketCard() {
        return getInstance().getBoolean("DISABLE_MARKET_CARD", false);
    }

    public static boolean disableAnswerPageAdvert() {
        return getInstance().getBoolean("DISABLE_ANSWER_PAGE_ADVERT", true);
    }

    public static boolean disableIdeaTab() {
        return getInstance().getBoolean("DISABLE_IDEA_TAB", false);
    }

    public static boolean disableCollegeTab() {
        return getInstance().getBoolean("DISABLE_COLLEGE_TAB", true);
    }

    public static boolean disableAnswerListAd() {
        return getInstance().getBoolean("DISABLE_ANSWER_LIST_AD", true);
    }
}
