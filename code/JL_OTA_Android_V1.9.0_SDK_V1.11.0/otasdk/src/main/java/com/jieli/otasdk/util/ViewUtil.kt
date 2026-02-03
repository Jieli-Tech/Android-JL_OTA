package com.jieli.otasdk.util

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.Window
import android.view.WindowManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * @author zqjasonZhong
 * @since 2024/1/18
 * @email zhongzhuocheng@zh-jieli.com
 * @desc 控件拓展工具
 */

fun View.setViewVisibility(visibility: Int) {
    if (this.visibility != visibility) {
        this.visibility = visibility
    }
}

fun View.show() = setViewVisibility(View.VISIBLE)

fun View.hide() = setViewVisibility(View.INVISIBLE)

fun View.gone() = setViewVisibility(View.GONE)

fun View.isShow(): Boolean = visibility == View.VISIBLE

fun View.isHide(): Boolean = visibility == View.INVISIBLE

fun View.isGone(): Boolean = visibility == View.GONE

fun Context.getView(layoutId: Int): View = LayoutInflater.from(this).inflate(layoutId, null, false)

class ViewUtil {

    companion object {

        private var clickJob: Job? = null

        /**
         * 设置沉浸式状态栏
         *
         * @param window 窗口控件
         * @param isLight 是否高亮
         */
        fun setImmersiveStateBar(window: Window, isLight: Boolean = true) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) return
            window.addFlags(
                WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS
                        or WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION
            )
            window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_LAYOUT_STABLE)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                window.clearFlags(
                    WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS
                            or WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION
                )
                window.statusBarColor = Color.TRANSPARENT
            }
            if (isLight) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    window.decorView.systemUiVisibility =
                        View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                }
            }
        }

        /**
         * DP转换PX
         *
         * @param context 上下文
         * @param dp 值
         */
        fun dp2px(context: Context, dp: Int): Int = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp.toFloat(),
            context.resources.displayMetrics
        ).toInt()

        /**
         * 获取APP版本号
         * @param context 上下文
         * @return Int 版本号
         */
        fun getAppVersion(context: Context): Int {
            val pm = context.packageManager
            return try {
                if (Build.VERSION.SDK_INT >= 28) {
                    pm.getPackageInfo(context.packageName, 0).longVersionCode.toInt()
                } else {
                    pm.getPackageInfo(context.packageName, 0).versionCode
                }
            } catch (e: PackageManager.NameNotFoundException) {
                e.printStackTrace()
                0
            }
        }

        /**
         * 获取APP版本名
         * @param context 上下文
         * @return String 版本号
         */
        fun getAppVersionName(context: Context): String {
            val pm = context.packageManager
            return try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    pm.getPackageInfo(
                        context.packageName,
                        PackageManager.PackageInfoFlags.of(0)
                    ).versionName ?: "0.0.0"
                } else {
                    pm.getPackageInfo(context.packageName, 0).versionName ?: "0.0.0"
                }
            } catch (var3: PackageManager.NameNotFoundException) {
                var3.printStackTrace()
                "0.0.0"
            }
        }

        /**
         * 防止快速点击事件
         */
        fun banQuickClick(scope: CoroutineScope, time: Long = 300, handler: () -> Unit) {
            clickJob?.cancel()
            clickJob = scope.launch {
                delay(time)
                handler()
                clickJob = null
            }
        }
    }
}