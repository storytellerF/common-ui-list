val Fragment.service get() = requireContext().service
val View.service get() = context.service
---
val Fragment.bindingService get() = requireContext().bindingService
val View.bindingService get() = context.bindingService
val ViewBinding.bindingService get() = binding.root.context.bindingService
