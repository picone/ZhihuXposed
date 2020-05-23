package com.chien.zhihuxposed;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;

import com.chien.zhihuxposed.utils.PreferenceUtils;

import java.lang.reflect.Method;

import de.robv.android.xposed.IXposedHookInitPackageResources;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_InitPackageResources;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import static com.chien.zhihuxposed.ZhihuConstant.CLASS_WEBVIEW_FRAGMENT;
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
                hookAnswerPageAdvert(loadPackageParam);
            }
            //开屏广告
            hookLaunchActivity(loadPackageParam);

            if (PreferenceUtils.openUrlInExternalBrowser()) {
                hookExternalBrowser(loadPackageParam);
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
     * 禁止通过ClassLoader获取de.robv.android.xposed开头的class，让它认为没有安装xposed
     * @param loadPackageParam lpparam
     */
    private void hookXposedUtil(XC_LoadPackage.LoadPackageParam loadPackageParam) {
        try {
            findAndHookMethod(ClassLoader.class, "loadClass", String.class, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    super.afterHookedMethod(param);
                    if (param.args.length > 0 && param.args[0] != null && param.args[0] instanceof String) {
                        String clazzName = (String)param.args[0];
                        if (clazzName.startsWith(ZhihuConstant.CLASS_XPOSED_PREFIX)) {
                            //如果调用栈包含EdXposed则允许
                            StackTraceElement[] stackTraceElements = (new Throwable()).getStackTrace();
                            for (StackTraceElement stackTraceElement:stackTraceElements) {
                                if(stackTraceElement.getClassName().contains("elderdrivers")){
                                    return;
                                }
                            }
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
                        }
                    } else if (resultClazz.equals(ZhihuConstant.CLASS_MODEL_MARKET_CARD)) {
                        if (disableMarketCard) {
                            param.setResult(null);
                        }
                    }
                }
            });
            findAndHookMethod(ZhihuConstant.CLASS_FEED_ZH_OBJECT_DESERIALIZER, loadPackageParam.classLoader, "registerZHObject", Class.class, String.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    super.beforeHookedMethod(param);
                    String fieldName = (String)param.args[1];
                    if (fieldName.equals("feed_advert")) {
                        if (disableFeedAdvert) {
                            param.args[1] = "";
                        }
                    } else if (fieldName.equals("market_card")) {
                        if (disableMarketCard) {
                            param.args[1] = "";
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
        try {
            XposedBridge.hookAllMethods(findClass(ZhihuConstant.CLASS_ANSWER_APP_VIEW, loadPackageParam.classLoader), "processHtmlContent", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    if (!(param.args[0] instanceof String)) {
                        return;
                    }
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
                }
            });
        } catch (XposedHelpers.ClassNotFoundError | NoSuchMethodError e) {
            XposedBridge.log("ZhihuXposed:" + e.toString());
        }
    }

    /**
     * 去除开屏广告
     * @param loadPackageParam lpparam
     */
    private void hookLaunchActivity(final XC_LoadPackage.LoadPackageParam loadPackageParam) {
        try {
            findAndHookMethod(ZhihuConstant.CLASS_LAUNCH_FRAGMENT, loadPackageParam.classLoader, "q", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    super.afterHookedMethod(param);
                    param.setResult(0);
                }
            });
        } catch (XposedHelpers.ClassNotFoundError | NoSuchFieldError e) {
            XposedBridge.log("ZhihuXposed:" + e.toString());
        }
    }

    /**
     * 增加copyright
     * @param resourcesParam rparam
     */
    private void replaceCopyright(XC_InitPackageResources.InitPackageResourcesParam resourcesParam) {
        int copyrightId = resourcesParam.res.getIdentifier("label_app_copyright", "string", ZhihuConstant.PACKAGE_NAME);
        String copyright = resourcesParam.res.getString(copyrightId);
        copyright += "\n@知乎助手 by ChienHo";
        resourcesParam.res.setReplacement(copyrightId, copyright);
    }

    private void hookExternalBrowser(final XC_LoadPackage.LoadPackageParam loadPackageParam){
        final Class<?> clazz = XposedHelpers.findClass(ZhihuConstant.CLASS_WEBVIEW_FRAGMENT, loadPackageParam.classLoader);

        XposedBridge.hookAllMethods(clazz, "a", new XC_MethodReplacement() {
            @Override
            protected Object replaceHookedMethod(XC_MethodHook.MethodHookParam param) throws Throwable {
                //非目标函数
                if(param.args[0].getClass() != String.class){
                    return XposedBridge.invokeOriginalMethod(param.method, param.thisObject, param.args);
                }

                String url = (String)param.args[0];
                if(url.startsWith("https://link.zhihu.com/")){
                    XposedBridge.log("ZhihuXposed: open URL in browser: " + url);
                    Object that = param.thisObject;
                    Method getActivityMethod = clazz.getMethod("getActivity");
                    Activity activity = (Activity)getActivityMethod.invoke(that);

                    //调用外部浏览器
                    Uri uri = Uri.parse(url);
                    Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                    activity.startActivity(intent);

                    //关掉空白的webview页面 TODO
                    /*Method popBackMethod = clazz.getDeclaredMethod("d", View.class);
                    popBackMethod.setAccessible(true);
                    popBackMethod.invoke(that, (View)null);*/
                    return null;
                }else{
                    XposedBridge.log("ZhihuXposed: not open URL in browser: " + url);
                    return XposedBridge.invokeOriginalMethod(param.method, param.thisObject, param.args);
                }
            }
        });
    }
}
