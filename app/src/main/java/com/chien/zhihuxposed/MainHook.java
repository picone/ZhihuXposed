package com.chien.zhihuxposed;

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
        // TODO
        // 去除答案页面大图广告
        // document.querySelector('.Advert-largeAd').style.display="none"
        // 去除答案页面小广告
        // document.querySelector('.AdvertRecommend-answer').style.display="none"
        /*findAndHookMethod("com.zhihu.android.app.appview.AppView2", loadPackageParam.classLoader, "onPageFinished", String.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                XposedBridge.log("ZHIHU:WEBVIEW:INFO:" + param.args[0].toString());
                callMethod(param.thisObject, "loadUrl", "document.querySelector('.Advert-largeAd').style.display='none';document.querySelector('.AdvertRecommend-answer').style.display='none'");
            }
        });*/
    }
}
