package com.edw.androidhookdemo.core

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Handler
import android.os.Message
import android.util.Log
import android.view.View
import com.edw.androidhookdemo.App
import java.lang.reflect.Field
import java.lang.reflect.Method
import java.lang.reflect.Proxy

/*****************************************************************************************************
 * Project Name:    AndroidHookDemo
 *
 * Date:            2021-08-02
 *
 * Author:         EdwardWMD
 *
 * Github:          https://github.com/Edwardwmd
 *
 * Blog:            https://edwardwmd.github.io/
 *
 * Description:    Hook Activity
 * 思路：同一Module下，使用一个已注册的Activity作为代理Activity（这里是第一个在MainActivity注册的Activity，即MainActivity），
 * 所以MainActivity既做宿主Activity又做代理Activity，只不过代理Activity的Intent是跳转目标Activity时的Intent,
 * 通过代理Activity实现瞒天过海的过程，当绕过AMS检测目标Activity注册后，此时将代理Activity换成目标Activity。
 ****************************************************************************************************
 */
object HookUtils {

    private const val TAG: String = "HookUtils"
    private const val PROXY_TAG = "proxy_tag"

    /**
     * 将PluginActivity替换成ProxyActivity，这里拿SDK28作为例子
     */
    fun hookActivityHelper() {
        val activityTaskManagerClz = clz("android.app.ActivityManager")
        val getService = methodCreate(activityTaskManagerClz, "getService")
        getService.isAccessible = true
        //静态方法的获取IActivityTaskManager实例
        /**
         *  public static IActivityTaskManager getService() {
         *  return IActivityTaskManagerSingleton.get();
         *  }
         */
        val staticGetService = getService.invoke(null)
        val iActivityTaskManagerClz = clz("android.app.IActivityManager")
        //创建自己的代理对象
        val proxy = Proxy.newProxyInstance(Thread.currentThread().contextClassLoader,
            arrayOf(iActivityTaskManagerClz)
        ) { proxy, method, args ->
            //判断当前方法是否为startActivity
            if (method.name == "startActivity") {
                var index = 0
                var mIntent: Intent? = null
                //获取startActivity 中的Intent参数
                for (i in args.indices) {
                    //拿到去PluginActivity的Intent
                    if (args[i] is Intent) {
                        index = i
                        mIntent = args[index] as Intent
                    }
                }

                //创建一个瞒天过海的Intent,绕过AndroidManifest.xml注册的这一步，避免被AMS检测。
                //使用当前Activity作为代理Activity
                val packageName = App.appContext().packageName
                val mProxyIntent = Intent().setClassName(packageName,
                    CommonUtil.getHostClzName(packageName)!!)
                mProxyIntent.putExtra(PROXY_TAG, mIntent)
                args[index] = mProxyIntent
            }

            return@newProxyInstance method.invoke(staticGetService, *args)
        }

        //拿到IActivityManagerSingleton属性
        val iActivityManagerSingleton =
            fieldCreate(activityTaskManagerClz, "IActivityManagerSingleton")
        iActivityManagerSingleton?.isAccessible = true
        //拿到静态变量，静态变量的get的参数为null
        val staticIAMS = iActivityManagerSingleton?.get(null)
        val singletonClz = clz("android.util.Singleton")
        val mInstanceField = fieldCreate(singletonClz, "mInstance")
        mInstanceField?.isAccessible = true
        mInstanceField?.set(staticIAMS, proxy)
    }


    /**
     * 将ProxyActivity替换成PluginActivity 这种方式目前只能适配同一个Module下的Activity无注册跳转
     * 因为跳转到不同的Module下的Activity需要将该Module的资源、类都要hook进宿主Activity中
     */
    @SuppressLint("DiscouragedPrivateApi")
    fun hookHandler() {
        val activityThreadClz = clz("android.app.ActivityThread")
        val sCurrentActivityThread = fieldCreate(activityThreadClz, "sCurrentActivityThread")
        sCurrentActivityThread?.isAccessible = true
        val mSCAT = sCurrentActivityThread?.get(null)

        val mHField = fieldCreate(activityThreadClz, "mH")
        mHField?.isAccessible = true
        val mH = mHField?.get(mSCAT)

        val callbackField = fieldCreate(Handler::class.java, "mCallback")
        callbackField?.isAccessible = true

        val callback = ActivityInterceptorHandlerCall()
        callbackField?.set(mH, callback)
    }

    /**
     * 点击事件Hook
     */
    @SuppressLint("DiscouragedPrivateApi")
    fun hookClickListener(v: View) {
        /**
         * 源码
         *   ListenerInfo getListenerInfo() {
         *       if (mListenerInfo != null) {
         *            return mListenerInfo;
         *        }
         *        mListenerInfo = new ListenerInfo();
         *     return mListenerInfo;
         *   }
         */

        val getListenerInfoMethod = methodCreate(View::class.java, "getListenerInfo")
        getListenerInfoMethod.isAccessible = true
        val mListenerInfo = getListenerInfoMethod.invoke(v)

        val listenerInfoClz = clz("android.view.View\$ListenerInfo") // 这是内部类的表示方法
        val clickListenerField = fieldCreate(listenerInfoClz, "mOnClickListener")
        val mOnClickListener = clickListenerField?.get(mListenerInfo)
        //如果控件注册点击监听在后,hookClickListener在前则会出现mOnClickListener为null的情况
        if (mOnClickListener == null) {
            Log.e(TAG, "ClickListener 不能为空，请确保ClickListener初始化的位置~~~~")
            return
        }
        val clickProxy = Proxy.newProxyInstance(Thread.currentThread().contextClassLoader,
            arrayOf(View.OnClickListener::class.java)
        ) { proxy, method, args ->
            UIUtils.toast("Hook 拦截控件${v.javaClass.simpleName}的点击成功！！")
            Log.e(TAG, "我们可以在${v.javaClass.simpleName}点击之前做一些其他的事~~~~")
            return@newProxyInstance method.invoke(mOnClickListener, *args)
        }

        clickListenerField.set(mListenerInfo, clickProxy)

    }

    private class ActivityInterceptorHandlerCall : Handler.Callback {
        companion object {
            private const val EXECUTE_TRANSACTION = 159
        }

        override fun handleMessage(msg: Message): Boolean {
            //替换Intent
            when (msg.what) {
                EXECUTE_TRANSACTION -> {
                    try {
                        val mActivityCallbacksField =
                            fieldCreate(msg.obj.javaClass, "mActivityCallbacks")
                        mActivityCallbacksField?.isAccessible = true
                        val mActivityCallbacks =
                            mActivityCallbacksField?.get(msg.obj) as MutableList<*>
                        for (i in 0 until mActivityCallbacks.size) {
                            if (mActivityCallbacks[i]?.javaClass?.name == "android.app.servertransaction.LaunchActivityItem") {
                                //拿到LaunchActivityItem Object
                                val launchActivityItem = mActivityCallbacks[i]
                                val mIntentField =
                                    fieldCreate(launchActivityItem?.javaClass, "mIntent")
                                mIntentField?.isAccessible = true
                                val mIntentObj = mIntentField?.get(launchActivityItem) as Intent
                                val proxyIntent =
                                    mIntentObj.getParcelableExtra<Intent>(PROXY_TAG)
                                if (proxyIntent != null) {
                                    mIntentField[launchActivityItem] = proxyIntent
                                }
                            }
                        }

                    } catch (e: Exception) {
                        Log.e(TAG, "hookHandler error ------> ${e.message} ")
                    }

                }
            }
            return false
        }

    }


    private fun clz(name: String): Class<*> {
        return Class.forName(checkObject(name))
    }

    private fun fieldCreate(clz: Class<*>?, fieldName: String): Field? {
        return clz?.getDeclaredField(fieldName)
    }

    private fun methodCreate(clz: Class<*>, methodName: String): Method {
        return clz.getDeclaredMethod(methodName)
    }

    private fun <T> checkObject(obj: T?): T {
        if (obj == null) {
            throw NullPointerException("全类名不能为NULL")
        }
        return obj
    }
}
