package com.edw.androidhookdemo.core

import android.widget.Toast
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
class UIUtils {
    companion object {
        fun toast(msg: String) {
            Toast.makeText(App.appContext(), msg, Toast.LENGTH_SHORT).show()
        }

    }

}