package com.chien.zhihuxposed;

import android.content.Context;
import android.content.IntentFilter;
import android.util.Log;
import android.webkit.ValueCallback;
import android.webkit.WebResourceResponse;

import com.chien.zhihuxposed.utils.PreferenceUtils;

import java.io.ByteArrayInputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Enumeration;
import java.util.Map;

import dalvik.system.DexFile;
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

    private final String TAG = "ZhihuXposed";
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
                hookAnswerPageAdvert(loadPackageParam);
            }
            XposedBridge.log("ZhihuXposed:inject into zhihu!");
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
                    XposedBridge.log("ZhihuXposed: " + resultClazz);
                    if (resultClazz.equals(ZhihuConstant.CLASS_MODEL_FEED_ADVERT)) {
                        if (disableFeedAdvert) {
                            param.setResult(null);
                            XposedBridge.log("ZhihuXposed:block a feed card");
                        }
                    } else if (resultClazz.equals(ZhihuConstant.CLASS_MODEL_MARKET_CARD)) {
                        if (disableMarketCard) {
                            param.setResult(null);
                            XposedBridge.log("ZhihuXposed:block a market card");
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
    private void hookAnswerPageAdvert(final XC_LoadPackage.LoadPackageParam loadPackageParam) {
        XposedBridge.log("ZhihuXposed: hookAnswerPageAdvert");
        try {
            /*
            XposedBridge.hookAllMethods(findClass("com.zhihu.android.answer.module.content.appview.AnswerAppView", loadPackageParam.classLoader), "setContentPaddingTop", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {

                }
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    Field[] field = param.thisObject.getClass().getDeclaredFields();
                    boolean isCache = false;
                    for (Field f : field) {
                        f.setAccessible(true);
                        if(f.getName().equals("mHitCache")){

                            isCache = f.getBoolean(param.thisObject);
                        }
                        if(f.getType().getName().equals(java.lang.String.class.getName())){
                            XposedBridge.log("Field Name = " + f.getName()+" == " + f.get(param.thisObject));
                        }else if(f.getType().getName().equals(int.class.getName())){
                            XposedBridge.log("Field Name = " + f.getName()+" == " + f.getInt(param.thisObject));
                        }else{
                            XposedBridge.log("Field Name = " + f.getName()+" == " + f.toGenericString());
                        }

                    }
                    XposedBridge.log("ZhihuXposed: AnswerAppView setContentPaddingTop");
                    if(!isCache){
                        XposedHelpers.callMethod(param.thisObject, "evaluateJavascript", "document.getElementsByClassName(\"AnswerFooter\")[0].innerText += \"・Xposed已生效\";", new ValueCallback<String>() {
                            @Override
                            public void onReceiveValue(String s) {
                                XposedBridge.log(s);
                            }
                        });
                        XposedHelpers.callMethod(param.thisObject, "evaluateJavascript", "document.getElementsByClassName(\"AnswerRecomReading-KMAnswerAdsCard\")[0].remove();", new ValueCallback<String>() {
                            @Override
                            public void onReceiveValue(String s) {
                                XposedBridge.log(s);
                            }
                        });
                    }
                }
            });*/
            XposedBridge.hookAllMethods(findClass("com.zhihu.android.answer.module.content.appview.AnswerAppView", loadPackageParam.classLoader), "processHtmlContent", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    XposedBridge.log("ZhihuXposed: AnswerAppView processHtmlContent");
                    String src = (String)param.args[0];

                    int index = src.indexOf("</body></html>");
                    if(index < 0){
                        XposedBridge.log("ZhihuXposed: 找不到HTML结尾");
                        return;
                    }
                    String result = src.substring(0, index);
                    result += "<script>" +
                            "window.onload = function () {" +
                            "  document.getElementsByClassName(\"AnswerRecomReading\")[0].parentElement.remove();" +
                            "  document.getElementsByClassName(\"AnswerFooter\")[0].innerText += \"・Xposed已生效\";" + //这条在无广告的时候不会显示（上一行报错）
                            "}" +
                            "</script></body></html>";
                    param.args[0] = result;

                    for(int i = 0; i < result.length(); i += 3000){
                        if(i + 3000 > result.length()){
                            XposedBridge.log("ZhihuXposed: " + result.substring(i));
                        }else{
                            XposedBridge.log("ZhihuXposed: " + result.substring(i, i+3000));
                        }
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
