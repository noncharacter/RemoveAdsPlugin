package com.chenyue.cancelAds.hook

import android.app.AndroidAppHelper
import android.content.Context
import android.os.Bundle
import android.webkit.WebView
import android.widget.Toast
import com.chenyue.cancelAds.BuildConfig
import com.chenyue.cancelAds.ui.MainActivity
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XC_MethodReplacement
import de.robv.android.xposed.XSharedPreferences
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.XposedHelpers.ClassNotFoundError
import de.robv.android.xposed.XposedHelpers.findAndHookMethod
import de.robv.android.xposed.XposedHelpers.findClass
import de.robv.android.xposed.XposedHelpers.setFloatField
import de.robv.android.xposed.callbacks.XC_LoadPackage
import java.net.URLDecoder

/**
 * Created by chenyue404
 */
class WeiboHook : IXposedHookLoadPackage {

    //微博国际版
    private val PACKAGE_NAME = "com.weico.international"
    private val TAG = "微博-hook-"

    private val sp by lazy {
        XSharedPreferences(
            BuildConfig.APPLICATION_ID,
            MainActivity.PREF_NAME
        )
    }

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {

        val packageName = lpparam.packageName
        val classLoader = lpparam.classLoader

        if (packageName != PACKAGE_NAME) {
            return
        }

        XposedBridge.log(TAG)

        val StatusClass =
            findClass("com.weico.international.model.sina.Status", classLoader)
        val ViewHolderClass =
            findClass("com.weico.international.activity.v4.ViewHolder", classLoader)
        findAndHookMethod(
            "com.weico.international.utility.KotlinExtendKt",
            classLoader,
            "isWeiboUVEAd",
            StatusClass,
            object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
//                    log("isWeiboUVEAd")
                    param.result = false
                }
            })

        val PageInfo =
            findClass("com.weico.international.model.sina.PageInfo", classLoader)
        findAndHookMethod(
            "com.weico.international.utility.KotlinUtilKt",
            classLoader,
            "findUVEAd",
            PageInfo,
            object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    log("findUVEAd")
                    param.result = null
                }
            })

        findAndHookMethod(
            "com.weico.international.activity.LogoActivity",
            classLoader,
            "doWhatNext",
            object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam?) {
                    if (param != null) {
                        log("doWhatNext-" + param.result)
                        param.result = "main"
                    }
                }
            })

        findAndHookMethod(
            "com.weico.international.activity.LogoActivity",
            classLoader,
            "triggerPermission",
            Boolean::class.java,
            object : XC_MethodReplacement() {
                override fun replaceHookedMethod(param: MethodHookParam): Any {
                    log("triggerPermission")
                    XposedHelpers.callMethod(param.thisObject, "initPermission")
                    return true
                }
            })

        try {
            findAndHookMethod(
                WebView::class.java,
                "loadUrl",
                String::class.java,
                buildWebviewHook(0)
            )
        } catch (e: NoSuchMethodError) {
            log("NoSuchMethodError--android.webkit.WebView#loadUrl")
        } catch (e: ClassNotFoundError) {
            log("ClassNotFoundError--android.webkit.WebView#loadUrl")
        }

        runCatching {
            findAndHookMethod(
                "com.sina.wbs.webkit.WebView",
                classLoader,
                "loadUrl",
                String::class.java,
                buildWebviewHook(0)
            )
        }.onFailure {
            log("com.sina.wbs.webkit.WebView#loadUrl, ${it.message}")
        }

        runCatching {
            findAndHookMethod(
                "com.weico.international.browser.BrowserManager",
                classLoader,
                "loadUrl",
                Context::class.java,
                String::class.java,
                buildWebviewHook(1)
            )
        }.onFailure {
            log("com.weico.international.browser.BrowserManager#loadUrl, ${it.message}")
        }

        findAndHookMethod(
            "com.weico.international.activity.v4.Setting",
            classLoader,
            "loadBoolean",
            String::class.java,
            object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    val key = param.args[0] as String
                    log("loadBoolean--$key")
                    when {
                        key == "BOOL_UVE_FEED_AD" -> param.result = false
                        key.startsWith("BOOL_AD_ACTIVITY_BLOCK_") -> param.result = true
                    }
                }
            })

        findAndHookMethod(
            "com.weico.international.activity.v4.Setting",
            classLoader,
            "loadInt",
            String::class.java,
            object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    val key = param.args[0] as String
                    log("loadInt--$key")
                    when (key) {
                        "ad_interval" -> param.result = Int.MAX_VALUE
                        "display_ad" -> param.result = 0
                    }
                }
            }
        )

        findAndHookMethod(
            "com.weico.international.activity.v4.Setting",
            classLoader,
            "loadStringSet",
            String::class.java,
            object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    val key = param.args[0] as String
                    log("loadStringSet--$key")
                    when (key) {
                        "CYT_DAYS" -> param.result = setOf<String>()
                    }
                }
            }
        )

        findAndHookMethod(
            "com.weico.international.activity.v4.Setting",
            classLoader,
            "loadString",
            String::class.java,
            object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    val key = param.args[0] as String
                    log("loadString--$key")
                    when (key) {
                        "video_ad" -> param.result = ""
                    }
                }
            }
        )

        val queryUveAdRequestHook = object : XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) {
                log("queryUveAdRequest")
                param.result = ""
            }
        }
        try {
            findAndHookMethod(
                "com.weico.international.api.RxApiKt", classLoader,
                "queryUveAdRequest\$lambda$153",
                findClass("java.util.Map", classLoader),
                queryUveAdRequestHook
            )
        } catch (e: NoSuchMethodError) {
            log("NoSuchMethodError--com.weico.international.api.RxApiKt.queryUveAdRequest\$lambda-153")
        } catch (e: ClassNotFoundError) {
            log("ClassNotFoundError--com.weico.international.api.RxApiKt.queryUveAdRequest\$lambda-153")
        }
        findAndHookMethod(
            "com.weico.international.video.AbsPlayer", classLoader,
            "setUp",
            Bundle::class.java,
            findClass("com.weico.international.model.sina.Status", classLoader),
            object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    log("com.weico.international.video.AbsPlayer#setUp")
                    sp.reload()
                    val newSpeed = sp.getFloat(MainActivity.KEY_VIDEO_SPEED, 1.0f)
                    setFloatField(param.thisObject, "mSpeed", newSpeed)
                }
            }
        )

        findAndHookMethod(
            "com.weico.international.data.VideoModalOTO", classLoader,
            "getDownloadAble",
            object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    param.result = true
                }
            }
        )

        try {
            findAndHookMethod(
                "com.weico.international.model.weico.Account", classLoader,
                "isVip",
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        log("com.weico.international.model.weico.Account#isVip")
                        param.result = true
                    }
                }
            )
        } catch (e: NoSuchMethodError) {
            log("NoSuchMethodError--com.weico.international.model.weico.Account#isVip")
        } catch (e: ClassNotFoundError) {
            log("ClassNotFoundError--com.weico.international.model.weico.Account#isVip")
        }

        try {
            findAndHookMethod(
                "com.sina.push.service.PushAlarmManager", classLoader,
                "a",
                Int::class.java,
                Long::class.java,
                Long::class.java,
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        log("com.sina.push.service.PushAlarmManager#a(int, long, long)")
                        param.result = null
                    }
                }
            )
        } catch (e: NoSuchMethodError) {
            log("NoSuchMethodError--com.sina.push.service.PushAlarmManager#a(int, long, long)")
        } catch (e: ClassNotFoundError) {
            log("ClassNotFoundError--com.sina.push.service.PushAlarmManager#a(int, long, long)")
        }

        runCatching {
            findAndHookMethod(
                "com.weico.international.adapter.TimelineHelper", classLoader,
                "displayAd",
                StatusClass,
                ViewHolderClass,
                Int::class.java,
                Boolean::class.java,
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        log("displayAd")
                        param.result = null
                    }
                }
            )
        }.onFailure {
            log("com.weico.international.adapter.TimelineHelper#displayAd, ${it.message}")
        }

        runCatching {
            findAndHookMethod(
                "com.sina.weibo.ad.h", classLoader,
                "getAdInfo",
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        log("com.sina.weibo.ad.h#getAdInfo")
                        param.result = null
                    }
                }
            )
        }.onFailure {
            log("com.sina.weibo.ad.h#getAdInfo, ${it.message}")
        }
    }

    private fun log(log: String) {
        if (BuildConfig.DEBUG) {
            XposedBridge.log("$TAG-$log")
        }
    }

    private fun buildWebviewHook(index: Int): XC_MethodHook {
        return object : XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) {
                var url = param.args[index] as String
                log("url:$url")
                val sinaUrl = "://weibo.cn/sinaurl?u="
                if (url.contains(sinaUrl)) {
                    url = url.substring(url.indexOf(sinaUrl) + sinaUrl.length, url.length)
                    url = URLDecoder.decode(url)
                    log("true url:$url")
                    param.args[index] = url
                } else {
                    Toast.makeText(
                        AndroidAppHelper.currentApplication().applicationContext,
                        "WebviewActivity-$url", Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }
}