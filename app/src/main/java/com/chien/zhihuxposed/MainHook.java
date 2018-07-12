package com.chien.zhihuxposed;

import android.webkit.WebResourceResponse;

import com.chien.zhihuxposed.utils.PreferenceUtils;

import java.io.ByteArrayInputStream;

import de.robv.android.xposed.IXposedHookInitPackageResources;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.callbacks.XC_InitPackageResources;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;
import static de.robv.android.xposed.XposedHelpers.findClass;

public class MainHook implements IXposedHookLoadPackage, IXposedHookInitPackageResources {

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam loadPackageParam) throws Throwable {
        if (ZhihuConstant.PACKAGE_NAME.equals(loadPackageParam.packageName)) {
            // 去除广告
            if (PreferenceUtils.disableFeedAdvert()) {
                hookFeedAdvert(loadPackageParam);
            }
            if (PreferenceUtils.disableAnswerPageAdvert()) {
                hookAnswerPageAdvert(loadPackageParam);
            }
        }
    }

    @Override
    public void handleInitPackageResources(XC_InitPackageResources.InitPackageResourcesParam resourcesParam) throws Throwable {
        if (ZhihuConstant.PACKAGE_NAME.equals(resourcesParam.packageName)) {
            // 修改copyright信息
            replaceCopyright(resourcesParam);
        }
    }

    /**
     * 去除广告
     * @param loadPackageParam lpparam
     */
    private void hookFeedAdvert(XC_LoadPackage.LoadPackageParam loadPackageParam) {
        // 去除首页Feed推荐广告
        Class<?> JsonParser = findClass(ZhihuConstant.CLASS_JSON_PARSER, loadPackageParam.classLoader);
        Class<?> DeserializationContext = findClass(ZhihuConstant.CLASS_JSON_DESERIALIZATION_CONTEXT, loadPackageParam.classLoader);
        findAndHookMethod(ZhihuConstant.CLASS_JSON_DESERIALIZER, loadPackageParam.classLoader, "a", JsonParser, DeserializationContext, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                Object result = param.getResult();
                if (result.getClass().getName().equals(ZhihuConstant.CLASS_MODEL_FEED_ADVERT)) {
                    param.setResult(null);
                }
            }
        });
    }

    private void hookAnswerPageAdvert(XC_LoadPackage.LoadPackageParam loadPackageParam) {
        // 去除答案页面webview里的广告
        findAndHookMethod(ZhihuConstant.CLASS_APP_VIEW2, loadPackageParam.classLoader, "shouldInterceptRequest", String.class, new XC_MethodHook() {
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

    private void replaceCopyright(XC_InitPackageResources.InitPackageResourcesParam resourcesParam) {
        int copyrightId = resourcesParam.res.getIdentifier("label_app_copyright", "string", ZhihuConstant.PACKAGE_NAME);
        String copyright = resourcesParam.res.getString(copyrightId);
        copyright += "\n@知乎助手 by ChienHo";
        resourcesParam.res.setReplacement(copyrightId, copyright);
    }
}
