package com.edw.androidhookdemo

import android.app.Application
import android.content.Context
import com.edw.androidhookdemo.core.HookUtils
import kotlin.properties.Delegates

/*****************************************************************************************************
 * Project Name:    AndroidHookDemo
 *
 * Date:            2021-08-03
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
class App : Application() {
    companion object {
        private var mC by Delegates.notNull<Context>()
        fun appContext() = mC
    }

    override fun onCreate() {
        super.onCreate()
        mC = this.applicationContext
       HookUtils.hookActivityHelper()
       HookUtils.hookHandler()
    }
}