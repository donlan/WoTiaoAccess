package gui.dong.wotiaoaccess

import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.text.TextUtils

/**
 * @author 梁桂栋
 * @version 1.0
 * @date 2018/11/12  15:22.
 * e-mail 760625325@qq.com
 * GitHub: https://github.com/donlan
 * description: PACKAGE_NAME
 */
object AccessibilityUtils {

    /**
     * 该辅助功能开关是否打开了
     * @param accessibilityServiceName：指定辅助服务名字
     * @param context：上下文
     * @return
     */
    fun isAccessibilitySettingsOn(accessibilityServiceName: String, context: Context): Boolean {
        var accessibilityEnable = 0
        val serviceName = context.packageName + "/" + accessibilityServiceName
        try {
            accessibilityEnable =
                    Settings.Secure.getInt(context.contentResolver, Settings.Secure.ACCESSIBILITY_ENABLED, 0)
        } catch (e: Exception) {
            //ALog.e( "get accessibility enable failed, the err:" + e.message)
        }

        if (accessibilityEnable == 1) {
            val mStringColonSplitter = TextUtils.SimpleStringSplitter(':')
            val settingValue =
                Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
            if (settingValue != null) {
                mStringColonSplitter.setString(settingValue)
                while (mStringColonSplitter.hasNext()) {
                    val accessibilityService = mStringColonSplitter.next()
                    if (accessibilityService.equals(serviceName, ignoreCase = true)) {
              //          ALog.v("We've found the correct setting - accessibility is switched on!")
                        return true
                    }
                }
            }
        } else {
            //ALog.d( "Accessibility service disable")
        }
        return false
    }

    /**
     * 跳转到系统设置页面开启辅助功能
     * @param accessibilityServiceName：指定辅助服务名字
     * @param context：上下文
     */
    fun openAccessibility(accessibilityServiceName: String, context: Context) {
        if (!isAccessibilitySettingsOn(accessibilityServiceName, context)) {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            context.startActivity(intent)
        }
    }
}
