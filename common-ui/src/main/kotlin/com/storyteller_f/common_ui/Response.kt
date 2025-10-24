package com.storyteller_f.common_ui

import android.os.Bundle
import android.os.Parcelable
import androidx.annotation.IdRes
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.LifecycleOwner
import androidx.navigation.NavController
import androidx.navigation.NavDirections
import androidx.navigation.NavOptions
import androidx.navigation.Navigator
import com.storyteller_f.common_vm_ktx.StateValueModel
import com.storyteller_f.common_vm_ktx.keyPrefix
import com.storyteller_f.common_vm_ktx.stateValueModel
import com.storyteller_f.common_vm_ktx.svm
import com.storyteller_f.compat_ktx.getParcelableCompat
import com.storyteller_f.compat_ktx.getSerializableCompat
import java.util.UUID
import kotlin.reflect.KClass

class ActivityAction(val action: (FragmentActivity, Parcelable) -> Unit, val requestKey: String)
class FragmentAction(val action: (Registry, Parcelable) -> Unit, val requestKey: String)

val waitingInActivity = mutableMapOf<String, List<ActivityAction>>()
val waitingInFragment = mutableMapOf<String, List<FragmentAction>>()

/**
 * 可以被请求。返回的结构通过requestKey 辨别
 */
interface ResponseFragment {
    val vm: StateValueModel<UUID?>
}

data class FragmentRequest(private val uuid: UUID?)

internal val ResponseFragment.fragmentRequest: FragmentRequest
    get() = FragmentRequest(vm.data.value)

internal fun <T : Parcelable> Fragment.setFragmentResult(fragmentRequest: FragmentRequest, result: T) =
    fm.setFragmentResult(fragmentRequest.toString(), Bundle().apply {
        putParcelable(FRAGMENT_RESULT_KEY, result)
    })

/**
 * 返回数据给当前的FragmentRequest。
 */
fun <T, F> F.setFragmentResult(result: T) where T : Parcelable, F : Fragment, F : ResponseFragment =
    setFragmentResult(fragmentRequest, result)

/**
 * 可以发起请求，发起的请求通过registryKey 辨别
 */
interface Registry {
    /**
     * 代表当前关心的事件
     */
    fun registryKey(): String = "${this.javaClass.simpleName}-registry"
}

/**
 * Fragment 返回的结果在Bundle 中的Key
 */
const val FRAGMENT_RESULT_KEY = "result"

val <T : Fragment> T.responseModel
    get() = keyPrefix("response", svm({ arguments }) { handle, arg ->
        stateValueModel(arg?.getSerializableCompat("uuid", UUID::class.java), handle)
    })

/**
 * 生成一个在Fragment从全局变量中获取所需结果的高阶函数
 */
inline fun <T : Parcelable, F> F.buildCallback(
    result: Class<T>,
    crossinline action: (F, T) -> Unit
): (String, Bundle) -> Unit where F : Fragment, F : Registry {
    return { requestKey: String, bundle: Bundle ->
        val registry = registryKey()
        waitingInFragment[registry]?.let { list ->
            bundle.getParcelableCompat(FRAGMENT_RESULT_KEY, result)?.let {
                action(this, it)
            }
            waitingInFragment[registry] = list.filter {
                it.requestKey != requestKey
            }
        }
    }
}

/**
 * 生成一个在Activity 从全局变量中获取所需结果的高阶函数
 */
inline fun <T : Parcelable, A> A.buildCallback(
    result: Class<T>,
    crossinline action: A.(T) -> Unit
): (String, Bundle) -> Unit where A : FragmentActivity, A : Registry {
    return { requestKey: String, bundle: Bundle ->
        val registry = registryKey()
        waitingInActivity[registry]?.let { list ->
            bundle.getParcelableCompat(FRAGMENT_RESULT_KEY, result)?.let {
                action(this, it)
            }
            waitingInActivity[registry] = list.filter {
                it.requestKey != requestKey
            }
        }
    }
}

/**
 * 在Fragment 中监听结果。
 * 如果启动是通过navigation 启动dialog，需要使用parentFragmentManager 接受结果
 */
private fun <T : Parcelable, F> F.waitingResponseInFragment(
    fragmentRequest: FragmentRequest,
    action: F.(T) -> Unit,
    callback: (String, Bundle) -> Unit
) where F : Registry, F : Fragment {
    val key = fragmentRequest.toString()
    val registerKey = registryKey()

    @Suppress("UNCHECKED_CAST")
    val actions = waitingInFragment.getOrPut(registerKey) {
        listOf()
    } + FragmentAction(action as (Registry, Parcelable) -> Unit, key)
    waitingInFragment[registerKey] = actions
    fm.setFragmentResultListener(key, owner, callback)
}

/**
 * 在Activity 中监听结果。
 */
private fun <A, T : Parcelable> A.waitingResponseInActivity(
    fragmentRequest: FragmentRequest,
    action: A.(T) -> Unit,
    callback: (String, Bundle) -> Unit
) where A : FragmentActivity, A : Registry {
    val key = fragmentRequest.toString()
    val registerKey = registryKey()

    @Suppress("UNCHECKED_CAST")
    val actions = waitingInActivity.getOrPut(registerKey) {
        listOf()
    } + ActivityAction(action as (FragmentActivity, Parcelable) -> Unit, key)
    waitingInActivity[registerKey] = actions
    fm.setFragmentResultListener(key, this, callback)
}

private fun <A> A.show(
    dialog: Class<out CommonDialogFragment>,
    parameters: Bundle?
): UUID where A : LifecycleOwner {
    val randomUUID = UUID.randomUUID()
    parameters?.putSerializable("uuid", randomUUID)
    val dialogFragment = dialog.getConstructor().newInstance().apply {
        arguments = parameters
    }
    dialogFragment.show(fm, randomUUID.toString())
    return randomUUID
}

fun NavController.request(
    @IdRes resId: Int,
    args: Bundle = Bundle(),
    navOptions: NavOptions? = null,
    navigatorExtras: Navigator.Extras? = null
): FragmentRequest {
    val randomUUID = UUID.randomUUID()
    args.putSerializable("uuid", randomUUID)
    navigate(resId, args, navOptions, navigatorExtras)
    return randomUUID.requestKey()
}

fun NavController.request(
    directions: NavDirections,
    navOptions: NavOptions? = null,
    navigatorExtras: Navigator.Extras? = null
): FragmentRequest {
    val randomUUID = UUID.randomUUID()
    val resId = directions.actionId
    val args = directions.arguments
    args.putSerializable("uuid", randomUUID)
    navigate(resId, args, navOptions, navigatorExtras)
    return randomUUID.requestKey()
}

/**
 * 显示一个Dialog 并且返回FragmentResult
 */
fun <F> F.request(
    dialog: KClass<out CommonDialogFragment>,
    parameters: Bundle = Bundle()
): FragmentRequest where F : LifecycleOwner = show(dialog.java, parameters).requestKey()

/**
 * 显示一个Dialog 并且返回FragmentResult
 */
fun <F> F.request(
    dialog: Class<out CommonDialogFragment>,
    parameters: Bundle = Bundle()
): FragmentRequest where F : LifecycleOwner = show(dialog, parameters).requestKey()

private fun UUID.requestKey(): FragmentRequest = FragmentRequest(this)

/**
 * 在Fragment 中监听结果。
 */
fun <T : Parcelable, F> F.observeResponse(
    fragmentRequest: FragmentRequest,
    result: KClass<T>,
    action: F.(T) -> Unit
) where F : Fragment, F : Registry = observeResponse(fragmentRequest, result.java, action)

/**
 * 在Fragment 中监听结果。
 */
fun <T : Parcelable, F> F.observeResponse(
    fragmentRequest: FragmentRequest,
    result: Class<T>,
    action: F.(T) -> Unit
) where F : Fragment, F : Registry {
    val callback = buildCallback(result, action)
    waitingResponseInFragment(fragmentRequest, action, callback)
}

/**
 * 在Activity 中监听结果。
 */
fun <T : Parcelable, A> A.observeResponse(
    fragmentRequest: FragmentRequest,
    result: KClass<T>,
    action: A.(T) -> Unit
) where A : FragmentActivity, A : Registry = observeResponse(fragmentRequest, result.java, action)

/**
 * 在Activity 中监听结果。
 */
fun <T : Parcelable, A> A.observeResponse(
    fragmentRequest: FragmentRequest,
    result: Class<T>,
    action: A.(T) -> Unit
) where A : FragmentActivity, A : Registry {
    val callback = buildCallback(result, action)
    waitingResponseInActivity(fragmentRequest, action, callback)
}

/**
 * 在Activity 中监听结果。
 */
internal fun <A> A.observeResponse() where A : FragmentActivity, A : Registry {
    waitingInActivity[registryKey()]?.forEach {
        val action = it.action
        val requestKey = it.requestKey
        val callback = buildCallback(Parcelable::class.java, action)
        val supportFragmentManager = fm
        supportFragmentManager.clearFragmentResultListener(requestKey)
        supportFragmentManager.setFragmentResultListener(requestKey, this, callback)
    }
}

/**
 * 在Fragment 中监听结果。
 */
internal fun CommonFragment.observeResponse() {
    waitingInFragment[registryKey()]?.forEach {
        val action = it.action
        val requestKey = it.requestKey
        val callback = buildCallback(Parcelable::class.java, action)
        val fragmentManager = fm
        fragmentManager.clearFragmentResultListener(requestKey)
        fragmentManager.setFragmentResultListener(requestKey, owner, callback)
    }
}
