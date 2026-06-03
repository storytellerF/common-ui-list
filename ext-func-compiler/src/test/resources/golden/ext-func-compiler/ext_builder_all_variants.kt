package sample
//5
//service##bindingService##dp##vm##delegate
//com.google.devtools.ksp.impl.symbol.kotlin.KSPropertyDeclarationImpl@@com.google.devtools.ksp.impl.symbol.kotlin.KSPropertyDeclarationImpl@@com.google.devtools.ksp.impl.symbol.kotlin.KSPropertyDeclarationImpl@@com.google.devtools.ksp.impl.symbol.kotlin.KSFunctionDeclarationImpl@@com.google.devtools.ksp.impl.symbol.kotlin.KSPropertyDeclarationImplimport sample.service
import sample.bindingService
import sample.dp
import sample.vm
import sample.delegate
import androidx.fragment.app.Fragment
import android.view.View
import androidx.viewbinding.ViewBinding
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.HasDefaultViewModelProviderFactory
import androidx.lifecycle.ViewModelStoreOwner
import kotlin.Any
import com.storyteller_f.ext_func_definition.ExtFuncFlat
import androidx.annotation.MainThread
import kotlin.Function0
import androidx.lifecycle.ViewModelStore
import kotlin.Function1
import androidx.activity.ComponentActivity

val Fragment.service get() = requireContext().service
val View.service get() = context.service
val Fragment.bindingService get() = requireContext().bindingService
val View.bindingService get() = context.bindingService
val ViewBinding.bindingService get() = binding.root.context.bindingService
context(ctx: Context)
val Int.dp
get() = toFloat().dp

context(v: View)
val Int.dp1
get() = v.context.run {
    toFloat().dp
}

context(f: Fragment)
val Int.dp2
get() = f.requireContext().run {
    toFloat().dp
}
//com.google.devtools.ksp.impl.symbol.kotlin.KSCallableReferenceImpl@<hash>,com.google.devtools.ksp.impl.symbol.kotlin.KSCallableReferenceImpl@<hash>,com.google.devtools.ksp.impl.symbol.kotlin.KSCallableReferenceImpl@<hash>,com.google.devtools.ksp.impl.symbol.kotlin.KSCallableReferenceImpl@<hash>
@MainThread
inline fun <reified VM : ViewModel, ARG> Fragment.avm(
    crossinline arg : Function0<ARG>,
    crossinline vmProducer : Function1<ARG, VM>
) = vm(arg, { requireActivity().viewModelStore }, { requireActivity() }, vmProducer)
@MainThread
inline fun <reified VM : ViewModel, ARG> Fragment.pvm(
    crossinline arg : Function0<ARG>,
    crossinline vmProducer : Function1<ARG, VM>
) = vm(arg, { requireParentFragment().viewModelStore }, { requireParentFragment() }, vmProducer)
@MainThread
inline fun <reified VM : ViewModel, T, ARG> T.vm(
    crossinline arg : Function0<ARG>,
    vmScope: VMScope,
    crossinline vmProducer : Function1<ARG, VM>
)  where T : HasDefaultViewModelProviderFactory, T : ViewModelStoreOwner = vm(arg, vmScope.storeProducer, vmScope.ownerProducer, vmProducer)
class DelegateOwnerImpl : DelegateOwner() {
   fun sayTest() = delegate.sayTest()
}
