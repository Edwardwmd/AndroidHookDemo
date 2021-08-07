package com.edw.androidhookdemo.core

import android.content.pm.ActivityInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.util.Log
import com.edw.androidhookdemo.App

/*****************************************************************************************************
 * Project Name:    AndroidHookDemo
 *
 * Date:            2021-08-07
 *
 * Author:         EdwardWMD
 *
 * Github:          https://github.com/Edwardwmd
 *
 * Blog:            https://edwardwmd.github.io/
 *
 * Description:    ToDo
 ****************************************************************************************************
 */
class CommonUtil {
    companion object {
        private const val TAG: String = "CommonUtil"

        /**
         *获取当前应用第一个Activity name,
         */
        fun getHostClzName(packageName:String): String? {
            var packageInfo: PackageInfo? = null
            try {
                packageInfo = App.appContext().packageManager.getPackageInfo(packageName,
                    PackageManager.GET_ACTIVITIES)

            } catch (e: Exception) {
                Log.e(TAG, "getPackageInfo error ---->${e.message}")
                return ""
            }

            val acts = packageInfo.activities
            if (acts == null || acts.isEmpty()) {
                return ""
            }
//            获取应用当前第一个Activity
            return acts[0].name
        }

    }

}