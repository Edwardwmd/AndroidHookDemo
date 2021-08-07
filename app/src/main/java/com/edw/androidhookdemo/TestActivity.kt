package com.edw.androidhookdemo

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import com.edw.androidhookdemo.core.HookUtils
import com.edw.androidhookdemo.core.UIUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

class TestActivity : AppCompatActivity(), CoroutineScope by MainScope() {
    companion object {
        private const val TAG: String = "TestActivity"
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_test)
        val button = findViewById<Button>(R.id.button)
        button.setOnClickListener {
            launch {
                Log.e(TAG,"Button 点击没有被Hook拦截")
                delay(3000)
                UIUtils.toast("按键被点击啦~~~")
            }
        }
        HookUtils.hookClickListener(button)

        findViewById<Button>(R.id.button3).setOnClickListener {
            startActivity(Intent(this,Test3Activity::class.java))
        }
    }
}