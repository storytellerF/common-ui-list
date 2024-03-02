package com.storyteller_f.common_pr

import android.os.Parcelable
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.navigation.NavDirections
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import com.storyteller_f.common_ui.FragmentRequest
import com.storyteller_f.common_ui.Registry
import com.storyteller_f.common_ui.observeResponse
import com.storyteller_f.common_ui.request

context (FragmentRequest)
fun <T : Parcelable, F> F.response(
    result: Class<T>,
    action: F.(T) -> Unit
) where F : Fragment, F : Registry {
    observeResponse(this@FragmentRequest, result, action)
}

context (FragmentRequest)
fun <T : Parcelable, A> A.response(
    result: Class<T>,
    action: A.(T) -> Unit
) where A : FragmentActivity, A : Registry {
    observeResponse(this@FragmentRequest, result, action)
}

context (F)
fun <T : Parcelable, F> FragmentRequest.response(
    result: Class<T>,
    action: F.(T) -> Unit
) where F : Fragment, F : Registry {
    observeResponse(this@FragmentRequest, result, action)
}

context (A)
fun <T : Parcelable, A> FragmentRequest.response(
    result: Class<T>,
    action: A.(T) -> Unit
) where A : FragmentActivity, A : Registry {
    observeResponse(this@FragmentRequest, result, action)
}

context (F)
fun <F> NavDirections.request() where F : Fragment, F : Registry =
    findNavController().request(this)

context (A)
fun <A> NavDirections.request(viewId: Int) where A : FragmentActivity, A : Registry =
    findNavController(viewId).request(this)
