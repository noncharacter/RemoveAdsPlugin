package com.chenyue.cancelAds.hook

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage

class ZhihuHook : IXposedHookLoadPackage {

    private val PACKAGE_NAME = "com.zhihu.android"
    private val TAG = "知乎-hook-"

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        val packageName = lpparam.packageName
        val classLoader = lpparam.classLoader

        if (packageName != PACKAGE_NAME) {
            return
        }

        XposedBridge.log(TAG)

        val clazz_ZHIntent =
            XposedHelpers.findClass("com.zhihu.android.app.util.ZHIntent", classLoader)
        XposedHelpers.findAndHookMethod(
            "com.zhihu.android.app.ui.activity.HostActivity",
            classLoader,
            "parseZHIntent",
            Intent::class.java,
            object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    val zhIntent = param.result
//                    val t = XposedHelpers.callMethod(zhIntent, "t")
//                    XposedBridge.log(TAG + "t=$t")
//                    val bundle = XposedHelpers.callMethod(zhIntent, "a") as Bundle
//                    XposedBridge.log(TAG + "bundle=${bundle.str()}")
                    val mTag = XposedHelpers.callMethod(zhIntent, "e") as String
                    if (mTag.startsWith("fakeurl://fake_empty_tag_com.zhihu.android.db.fragment._db_detail_with_relation_fragment")) {
                        val bundle = XposedHelpers.callMethod(zhIntent, "a") as Bundle
                        val url = bundle.get("key_router_raw_url") as String
                        XposedBridge.log(TAG + "url=$url")
                        val activity = param.thisObject as Activity
                        activity.startActivity(
                            Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse(url)
                            )
                        )
                        activity.finish()
                    }
                }
            })
    }

    private fun Bundle.str(): String {
        val map = mutableMapOf<String, Any>()
        keySet().forEach { key ->
            get(key)?.let { value ->
                map[key] = value
            }
        }
        return map.toList().joinToString()
    }

}