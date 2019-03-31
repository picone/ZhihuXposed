package com.chien.zhihuxposed;

import android.webkit.WebResourceResponse;

import com.chien.zhihuxposed.utils.PreferenceUtils;

import java.io.ByteArrayInputStream;
import java.util.List;

import de.robv.android.xposed.IXposedHookInitPackageResources;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_InitPackageResources;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;
import static de.robv.android.xposed.XposedHelpers.findClass;

public class MainHook implements IXposedHookLoadPackage, IXposedHookInitPackageResources {

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam loadPackageParam) throws Throwable {
        if (ZhihuConstant.PACKAGE_NAME.equals(loadPackageParam.packageName)) {
            // 反制Xposed检测
            hookXposedUtil(loadPackageParam);
            // 去除广告
            boolean disableFeedAdvert = PreferenceUtils.disableFeedAdvert(),
                    disableMarketCard = PreferenceUtils.disableMarketCard();
            if (disableFeedAdvert || disableMarketCard) {
                hookFeedAdvert(loadPackageParam, disableFeedAdvert, disableMarketCard);
            }
            if (PreferenceUtils.disableAnswerPageAdvert()) {
                //hookAnswerPageAdvert(loadPackageParam);
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
     * 假装已经放了/sdcard/zhihu/.allowXposed
     * @param loadPackageParam lpparam
     */
    private void hookXposedUtil(XC_LoadPackage.LoadPackageParam loadPackageParam) {
        try {
            findAndHookMethod(ZhihuConstant.CLASS_XPOSED_UTILS, loadPackageParam.classLoader, "a", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    param.setResult(false);
                }
            });
        } catch (XposedHelpers.ClassNotFoundError | NoSuchMethodError e) {
            XposedBridge.log("ZhihuXposed:" + e.toString());
        }
    }

    /**
     * 去除首页Feed广告
     * @param loadPackageParam lpparam
     */
    private void hookFeedAdvert(XC_LoadPackage.LoadPackageParam loadPackageParam, final boolean disableFeedAdvert, final boolean disableMarketCard) {
        Class<?> JsonParser = findClass(ZhihuConstant.CLASS_JSON_PARSER, loadPackageParam.classLoader);
        Class<?> DeserializationContext = findClass(ZhihuConstant.CLASS_JSON_DESERIALIZATION_CONTEXT, loadPackageParam.classLoader);
        try {
            findAndHookMethod(ZhihuConstant.CLASS_ZH_OBJECT_DESERIALIZER, loadPackageParam.classLoader, "deserialize", JsonParser, DeserializationContext, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    Object result = param.getResult();
                    String resultClazz = result.getClass().getName();
                    XposedBridge.log("ZhihuXposed:deserializer result clazz is_" + resultClazz);
                    if (resultClazz.equals(ZhihuConstant.CLASS_MODEL_FEED_ADVERT)) {
                        if (disableFeedAdvert) {
                            param.setResult(null);
                        }
                    } else if (resultClazz.equals(ZhihuConstant.CLASS_MODEL_MARKET_CARD)) {
                        if (disableMarketCard) {
                            param.setResult(null);
                        }
                    }
                }
            });
        } catch (XposedHelpers.ClassNotFoundError | NoSuchMethodError e) {
            XposedBridge.log("ZhihuXposed:" + e.toString());
        }
    }

    /**
     * 去除答案页面WebView内广告
     * @param loadPackageParam lpparam
     */
    private void hookAnswerPageAdvert(XC_LoadPackage.LoadPackageParam loadPackageParam) {
        try {
            findAndHookMethod(ZhihuConstant.CLASS_APP_VIEW2, loadPackageParam.classLoader, "shouldInterceptRequest", String.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    // 页面加载完成了，inject
                    String url = (String) param.args[0];
                    XposedBridge.log("ZhihuXposed:request api:" + url);
                    if (url.matches("^https?://www.zhihu.com/api/v\\d/creator/people/\\w+/promotion.+")) {
                        // 直接替换返回的内容，够狠
                        param.setResult(new WebResourceResponse("application/json", "UTF-8", new ByteArrayInputStream("{}".getBytes())));
                    }
                }
            });
        } catch (XposedHelpers.ClassNotFoundError | NoSuchMethodError e) {
            XposedBridge.log("ZhihuXposed:" + e.toString());
        }
    }

    private void replaceCopyright(XC_InitPackageResources.InitPackageResourcesParam resourcesParam) {
        int copyrightId = resourcesParam.res.getIdentifier("label_app_copyright", "string", ZhihuConstant.PACKAGE_NAME);
        String copyright = resourcesParam.res.getString(copyrightId);
        copyright += "\n@知乎助手 by ChienHo";
        resourcesParam.res.setReplacement(copyrightId, copyright);
    }
}
