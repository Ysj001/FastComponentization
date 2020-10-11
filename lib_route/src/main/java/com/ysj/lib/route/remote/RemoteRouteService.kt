package com.ysj.lib.route.remote

import android.os.Parcelable
import android.util.Log
import com.ysj.lib.route.Caches
import com.ysj.lib.route.annotation.RouteBean
import com.ysj.lib.route.callback.InterceptorCallback
import com.ysj.lib.route.entity.InterruptReason
import com.ysj.lib.route.entity.Postman
import com.ysj.lib.route.template.IActionProcessor
import java.io.Serializable
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * 夸进程的路由服务
 *
 * @author Ysj
 * Create time: 2020/8/18
 */
internal class RemoteRouteService : IRouteService.Stub() {

    companion object {
        private const val TAG = "RouteService"

        /** 用于获取路由服务的 KEY */
        const val ROUTE_SERVICE = "ROUTE_SERVICE"
    }

    override fun registerApplicationId(applicationId: String) {
        RemoteRouteProvider.instance!!.allApplicationId.add(applicationId)
    }

    override fun getAllApplicationId() = RemoteParam().also {
        it.params[REMOTE_ALL_APPLICATION_ID] = RemoteRouteProvider.instance?.allApplicationId!!
    }

    override fun registerRouteGroup(group: String, param: RemoteParam) {
        val routeMap = HashMap<String, RouteBean>()
        param.params.forEach { routeMap[it.key] = (it.value as RemoteRouteBean).routeBean }
        Caches.routeCache[group] = routeMap
        Log.i(TAG, "registerRouteGroup: ${Caches.routeCache.size} , $group , $param")
    }

    override fun findRouteBean(group: String?, path: String?): RemoteRouteBean? {
        if (group.isNullOrEmpty() || path.isNullOrEmpty()) return null
        val routeBean = Caches.routeCache[group]?.get(path)
        return if (routeBean == null) null else RemoteRouteBean(routeBean)
    }

    override fun doAction(className: String?, actionName: String?): RemoteParam? {
        if (className.isNullOrEmpty() || actionName.isNullOrEmpty()) return null
        try {
            val processor = Caches.actionCache[className]
                ?: Class.forName(className).getConstructor().newInstance() as IActionProcessor
            Caches.actionCache[className] = processor
            val actionResult = processor.doAction(actionName) ?: return null
            if (actionResult !is Serializable && actionResult !is Parcelable) return null
            return RemoteParam().apply { params[REMOTE_ACTION_RESULT] = actionResult }
        } catch (e: Exception) {
            Log.w(TAG, "$className 行为处理器没有在该进程找到 --> ${e.message}")
        }
        return null
    }

    override fun handleInterceptor(
        timeout: Long,
        remote: RemoteRouteBean,
        callback: RemoteInterceptorCallback
    ) {
        val postman = remote.routeBean as Postman
        // 取得匹配的拦截器
        val matchInterceptor = Caches.interceptors.filter { it.match(postman) }
        val countDownLatch = CountDownLatch(matchInterceptor.size)
        val localCallback = object : InterceptorCallback {
            override fun onContinue(postman: Postman) {
                callback.onContinue()
                countDownLatch.countDown()
            }

            override fun onInterrupt(postman: Postman, reason: InterruptReason<*>) {
                callback.onInterrupt(RemoteParam().also { param ->
                    param.params[REMOTE_INTERRUPT_REASON] = reason
                })
                countDownLatch.countDown()
            }
        }
        matchInterceptor.forEach {
            it.onIntercept(RemoteRouteProvider.instance!!.context!!, postman, localCallback)
        }
        countDownLatch.await(timeout, TimeUnit.MILLISECONDS)
    }

}