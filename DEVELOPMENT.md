# 开发与验证

使用 JDK 21 和 Android SDK API 37。通过本地 `local.properties` 的 `sdk.dir` 或 `ANDROID_HOME` 指定 SDK，在仓库根目录执行：

```sh
./gradlew :ui-list:testDebugUnitTest :ui-list:assembleDebug :ui-list:detekt
```

## ViewHolder 生命周期

`AbstractViewHolder` 在 RecyclerView attach 时监听所属 Fragment 的 Lifecycle；没有 Fragment 时使用 ComponentActivity。宿主 START/RESUME/PAUSE/STOP 驱动 holder 的内部 Lifecycle。STOP 保留监听，以便宿主恢复时接收 START；回收或宿主 DESTROY 时移除监听并销毁内部 owner。

注册时必须保存实际的 Lifecycle，解绑时使用同一个对象。回收的行视图可能已经离开 Fragment 的 view tree，再从 `itemView` 查找 owner 会回退到 Activity，导致 Fragment 上残留 observer。生命周期回调不应再次从视图查找 owner 或重新注册 observer。

Fragment 中的数据订阅仍应使用 `viewLifecycleOwner`，adapter 应按 view lifecycle 创建和清理。这与 holder 内部的 observer 管理独立，不能替代库自身的解绑。

`AbstractViewHolderTest` 覆盖行视图脱离父树后回收、宿主恢复与销毁。`FragmentGridNavigationTest` 使用真实 RecyclerView 和 FragmentManager，覆盖列表切换到网格后压入子 Fragment，再返回原网格。两者固定使用 Robolectric API 36；测试 JVM 的 `jdk.internal.access` 导出仅用于 Robolectric 的 shared-memory 初始化，不用于运行时库或应用。
