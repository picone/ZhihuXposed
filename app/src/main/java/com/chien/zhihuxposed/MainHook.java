package com.chien.zhihuxposed;

import android.webkit.WebResourceResponse;

import java.io.ByteArrayInputStream;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;
import static de.robv.android.xposed.XposedHelpers.findClass;

public class MainHook implements IXposedHookLoadPackage {
    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam loadPackageParam) throws Throwable {
        if ("com.zhihu.android".equals(loadPackageParam.packageName)) {
            // 去除广告
            hookAd(loadPackageParam);
        }
    }

    /**
     * 去除广告
     * @param loadPackageParam lpparam
     */
    private void hookAd(final XC_LoadPackage.LoadPackageParam loadPackageParam) {
        // 去除首页Feed推荐广告
        Class<?> JsonParser = findClass("com.fasterxml.jackson.core.JsonParser", loadPackageParam.classLoader);
        Class<?> DeserializationContext = findClass("com.fasterxml.jackson.databind.DeserializationContext", loadPackageParam.classLoader);
        findAndHookMethod("com.zhihu.android.api.util.l", loadPackageParam.classLoader, "a", JsonParser, DeserializationContext, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                Object result = param.getResult();
                if (result.getClass().getName().equals("com.zhihu.android.api.model.FeedAdvert")) {
                    param.setResult(null);
                }
            }
        });
        // 去除答案页面webview里的广告
        findAndHookMethod("com.zhihu.android.app.appview.AppView2", loadPackageParam.classLoader, "shouldInterceptRequest", String.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                // 页面加载完成了，inject
                String url = (String) param.args[0];
                if (url.matches("^https?://www.zhihu.com/api/v\\d/community-ad/answers/\\d+/bottom-recommend-ad")) {
                    // 直接替换返回的内容，够狠
                    param.setResult(new WebResourceResponse("application/json", "UTF-8", new ByteArrayInputStream("{}".getBytes())));
                }
            }
        });
    }
}
