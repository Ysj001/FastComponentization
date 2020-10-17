package com.ysj.lib.route.entity

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.os.IBinder
import android.os.Parcelable
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import com.ysj.lib.route.YCR
import com.ysj.lib.route.annotation.RouteBean
import com.ysj.lib.route.callback.InterceptorCallback
import com.ysj.lib.route.callback.RouteResultCallback
import com.ysj.lib.route.lifecycle.RouteLifecycleObserver
import com.ysj.lib.route.type.checkMethodParameterType
import java.io.Serializable

/**
 * 用于构建路由过程的实体
 *
 * @author Ysj
 * Create time: 2020/8/4
 */
class Postman(group: String, path: String) : RouteBean(group, path), RouteLifecycleObserver {

    /** 路由所携带的数据 */
    val bundle = Bundle()

    /** [Activity] 的 requestCode */
    var requestCode: Int = -1
        private set

    /** 要执行的行为名称 */
    var actionName: String = ""
        private set

    private var lifecycle: Lifecycle? = null

    internal var routeResultCallbacks: MutableCollection<RouteResultCallback<Any?>?>? = null

    internal var continueCallback: InterceptorCallback.ContinueCallback? = null

    internal var interruptCallback: InterceptorCallback.InterruptCallback? = null

    override fun onDestroy(owner: LifecycleOwner) {
        routeResultCallbacks = null
        continueCallback = null
        interruptCallback = null
        lifecycle = null
        owner.lifecycle.removeObserver(this)
    }

    /**
     * 绑定生命周期，当生命周期状态变更为 [Lifecycle.State.DESTROYED] 时会中断路由过程
     */
    fun bindLifecycle(lifecycle: Lifecycle) = apply {
        this.lifecycle = lifecycle
        lifecycle.addObserver(this)
    }

    /**
     * 解绑生命周期，会在路由结束后自动解绑
     */
    fun unBindLifecycle() = this.lifecycle?.removeObserver(this) ?: Unit

    /**
     * 路由调用链的最后一步，开始路由导航
     */
    fun navigation(context: Context) = YCR.getInstance().navigation(context, this)

    /**
     * 用于获取路由成功的结果，你可以添加多个类型用于适应路由过程中行为的改变
     * - 如果导航过程中被拦截则不会执行
     * - 当添加了多个监听时只会回调类型匹配的那个
     */
    @Suppress("UNCHECKED_CAST")
    fun <T> addOnResultCallback(callback: RouteResultCallback<T?>) = apply {
        if (this.routeResultCallbacks == null) this.routeResultCallbacks = ArrayList()
        this.routeResultCallbacks?.add(object : RouteResultCallback<Any?>() {
            override fun onResult(result: Any?) {
                if (checkMethodParameterType(
                        callback.javaClass,
                        "onResult",
                        result?.javaClass
                    )
                ) callback.onResult(result as T?)
            }
        })
    }

    @JvmSynthetic
    @Suppress("UNCHECKED_CAST")
    fun <T> addOnResultCallback(callback: (T?) -> Unit) = apply {
        if (this.routeResultCallbacks == null) this.routeResultCallbacks = ArrayList()
        this.routeResultCallbacks?.add(object : RouteResultCallback<Any?>() {
            override fun onResult(result: Any?) {
                if (checkMethodParameterType(
                        callback.javaClass,
                        "invoke",
                        result?.javaClass
                    )
                ) callback(result as T?)
            }
        })
    }

    /**
     * 当被拦截器拦截，拦截器执行 [InterceptorCallback.onContinue] 后回调
     */
    fun doOnContinue(callback: InterceptorCallback.ContinueCallback) = apply {
        this.continueCallback = callback
    }

    @JvmSynthetic
    fun doOnContinue(callback: (Postman) -> Unit) = apply {
        doOnContinue(object : InterceptorCallback.ContinueCallback {
            override fun onContinue(postman: Postman) = callback(postman)
        })
    }

    /**
     * 当被拦截器拦截，拦截器执行 [InterceptorCallback.onInterrupt] 后回调
     */
    fun doOnInterrupt(callback: InterceptorCallback.InterruptCallback) = apply {
        this.interruptCallback = callback
    }

    @JvmSynthetic
    fun doOnInterrupt(callback: (Postman, InterruptReason<*>) -> Unit) = apply {
        doOnInterrupt(object : InterceptorCallback.InterruptCallback {
            override fun onInterrupt(postman: Postman, reason: InterruptReason<*>) =
                callback(postman, reason)
        })
    }

    /**
     * 设置要执行的行为
     *
     * @param actionName 要执行的行为的名称
     */
    fun withRouteAction(actionName: String?) = apply {
        if (actionName.isNullOrEmpty()) return@apply
        this.actionName = actionName
    }

    /**
     * 设置 [Activity] 的 requestCode
     *
     * @param requestCode
     * int: If >= 0, this code will be returned in onActivityResult() when the activity exits.
     */
    fun withRequestCode(requestCode: Int) = apply { this.requestCode = requestCode }

    /**
     * Inserts an int value into the mapping of this Bundle, replacing
     * any existing value for the given key.
     *
     * @param key a String, or null
     * @param value an int
     */
    fun withInt(key: String?, value: Int) = apply { bundle.putInt(key, value) }

    /**
     * Inserts a long value into the mapping of this Bundle, replacing
     * any existing value for the given key.
     *
     * @param key a String, or null
     * @param value a long
     */
    fun withLong(key: String?, value: Long) = apply { bundle.putLong(key, value) }

    /**
     * Inserts a Boolean value into the mapping of this Bundle, replacing
     * any existing value for the given key.  Either key or value may be null.
     *
     * @param key a String, or null
     * @param value a boolean
     */
    fun withBoolean(key: String?, value: Boolean) = apply { bundle.putBoolean(key, value) }

    /**
     * Inserts a String value into the mapping of this Bundle, replacing
     * any existing value for the given key.  Either key or value may be null.
     *
     * @param key a String, or null
     * @param value a String, or null
     */
    fun withString(key: String?, value: String?) = apply { bundle.putString(key, value) }

    /**
     * Inserts a Serializable value into the mapping of this Bundle, replacing
     * any existing value for the given key.  Either key or value may be null.
     *
     * @param key a String, or null
     * @param value a Serializable object, or null
     */
    fun withSerializable(key: String?, value: Serializable?) =
        apply { bundle.putSerializable(key, value) }

    /**
     * Inserts a Parcelable value into the mapping of this Bundle, replacing
     * any existing value for the given key.  Either key or value may be null.
     *
     * @param key a String, or null
     * @param value a Parcelable object, or null
     */
    fun withParcelable(key: String?, value: Parcelable?) =
        apply { bundle.putParcelable(key, value) }

    /**
     * Inserts an {@link IBinder} value into the mapping of this Bundle, replacing
     * any existing value for the given key.  Either key or value may be null.
     *
     * <p class="note">You should be very careful when using this function.  In many
     * places where Bundles are used (such as inside of Intent objects), the Bundle
     * can live longer inside of another process than the process that had originally
     * created it.  In that case, the IBinder you supply here will become invalid
     * when your process goes away, and no longer usable, even if a new process is
     * created for you later on.</p>
     *
     * @param key a String, or null
     * @param value an IBinder object, or null
     */
    fun withBinder(key: String?, value: IBinder?) = apply { bundle.putBinder(key, value) }

    /**
     * Inserts all mappings from the given Bundle into this Bundle.
     *
     * @param bundle a Bundle
     */
    fun withAll(bundle: Bundle?) = apply {
        if (bundle == null) return@apply
        this.bundle.putAll(bundle)
    }

    /**
     * 将另一个 [Postman] 中的数据复制过来
     */
    internal fun from(postman: Postman) {
        withAll(postman.bundle)
        withRouteAction(postman.actionName)
        withRequestCode(postman.requestCode)
        from(postman as RouteBean)
    }

    /**
     * 将 [RouteBean] 的信息赋值到 [Postman] 中
     */
    internal fun from(routeBean: RouteBean) {
        this.group = routeBean.group
        this.path = routeBean.path
        this.types = routeBean.types
        this.typeElement = routeBean.typeElement
        this.moduleId = routeBean.moduleId
        this.className = routeBean.className
    }
}