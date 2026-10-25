# 开发与验证

使用 JDK 21 和 Android SDK API 37。通过本地 `local.properties` 的 `sdk.dir` 或 `ANDROID_HOME` 指定 SDK，在仓库根目录执行：

```sh
./gradlew :ui-list:testDebugUnitTest :ui-list:assembleDebug :ui-list:detekt
```

## Gradle 并行构建

默认开启跨项目并行，最多 4 个 worker；不设置 `Test.maxParallelForks`，保留 Gradle 默认的单 fork。Gradle daemon 的 4 GiB 堆限制不包含测试 JVM、Kotlin daemon 和其他 worker。内存较小的环境可用 `--max-workers=2`，或用 `--no-parallel` 退回串行。

各模块的编译、KSP 生成代码、测试和 Kover 报告使用各自的 `build/` 目录。Kover 汇总通过项目依赖收集产物。Detekt 保留现有自动修复行为，因此分析任务与 baseline 生成任务共用一个独占 build service；SARIF 汇总等待本次调度的所有 Detekt 生产任务结束。共享的 `config/detekt/baseline.xml` 仍为现有全局基线，更新它时应显式选择一个模块的 baseline 任务并审查差异，不能把多模块依次覆盖的结果当作合并基线。

`connected*AndroidTest` 任务共用一个独占 build service，同一次 Gradle 调用中不会同时占用设备；CI 的 `connectedCheck` 也显式传入 `--no-parallel`。独立 Gradle 调用或其他项目共用同一设备时，仍需在调用外持有设备锁；build service 只约束本次调用。

### 重现性能与内存对比

安装 Python 3.11+ 和 `psutil`，先运行一次两个编译器模块的测试，预下载依赖，然后运行：

```sh
./gradlew :ext-func-compiler:test :ui-list-annotation-compiler-ksp:test
python tools/benchmark-gradle.py --repeats 3 --workers 4
```

输出位于 `build/parallel-benchmark/`：每次调用的完整日志与 `results.json`。脚本交替串行/并行顺序，使用相同任务和 worker 数。冷缓存清除两个编译器及三个依赖模块的构建产物，禁用 Gradle 构建缓存和配置缓存；热缓存保留刚生成的编译产物，使用任务级 `--rerun` 强制执行两个测试。两者均不清除下载依赖或系统文件缓存。此处的热缓存衡量复用编译产物后的实际测试，不代表全部任务 UP-TO-DATE 的空跑或 FROM-CACHE 恢复性能。

每次使用独立的单次 Gradle daemon，并用临时 Kotlin daemon registry 隔离其他构建；保留默认的独立 Kotlin 编译进程和 JVM 堆设置，记录冷构建是否确实启动了该进程。因此测量不包含 daemon/JIT 热身复用收益。每 200 ms 对本次启动的进程树采样，记录同时刻的工作集总和（RSS）和 Windows private bytes 总和的峰值，不把不同进程各自的历史峰值相加；RSS 可能重复计算共享页。非 Windows 系统记录 USS 作为独占内存指标。测试任务的真实起止时间用于计算重叠量，JUnit XML 必须存在且无失败，并按轮次保存副本。短于采样间隔的内存尖峰可能遗漏。

本次验收以测量进程树峰值 RSS ≤ 6 GiB、private bytes ≤ 8 GiB 为预算，串行对照使用同一预算；超过时先减少 worker。共享机器的其他任务会影响耗时与可用内存，解读结果时应同时查看 `min_system_available_mib`，并在目标 CI 机器上复测。

### Issue #32 验证结果（2026-10-25）

基于提交 `693b452` 加本次构建配置改动，在 Windows 11、28 个逻辑 CPU、约 31.84 GiB RAM、Temurin 21.0.12.1、Gradle 9.8.1、Kotlin 2.4.21、Python 3.12 / psutil 7.2.2 上执行。两个独立 JVM 模块为 `ext-func-compiler` 和 `ui-list-annotation-compiler-ksp`，三轮交替顺序，共 12 次测试调用、72 个测试用例，全部通过且无跳过。

| 缓存 | 模式 | 三轮耗时（秒） | 中位数（秒） | 最大 RSS（MiB） | 最大 private bytes（MiB） | 测试任务重叠（秒） |
| --- | --- | --- | ---: | ---: | ---: | --- |
| 冷 | 串行 | 76.757 / 77.605 / 81.474 | 77.605 | 2704.3 | 3894.1 | 0 / 0 / 0 |
| 冷 | 并行 | 61.696 / 49.614 / 73.104 | 61.696 | 3397.1 | 4773.7 | 20.386 / 20.298 / 22.707 |
| 热 | 串行 | 65.473 / 66.362 / 68.795 | 66.362 | 1893.4 | 2428.4 | 0 / 0 / 0 |
| 热 | 并行 | 32.978 / 48.033 / 79.605 | 48.033 | 2333.2 | 2968.8 | 13.665 / 20.506 / 31.589 |

冷、热耗时中位数分别减少 20.5% 和 27.6%，全部样本均在上述进程内存预算内，因此保留跨模块并行和 4 worker 上限。第三轮并行热测量比对应串行慢 15.7%，当时整机可用内存最低仅 300.9 MiB；这个样本保留在统计中，说明内存紧张时并行仍可能退化，不能将中位数收益视为保证。原始日志、逐次内存峰值、时间区间和 JUnit XML 副本位于 `build/parallel-benchmark/`，不纳入版本控制。

其他验收结果：

- `koverXmlReport koverHtmlReport detekt --no-auto-correct --continue --parallel`：选中的宿主测试报告共 64 个用例，无失败、错误或跳过（两个编译器测试任务从构建缓存恢复，其余实际执行）。10 个库模块的 XML/HTML 覆盖率报告全部生成，根报告包含全部 105 个模块报告类，另含 2 个现有 ViewBinding 生成类，没有遗漏。实际应用的 KSP 编译也通过，生成 17 个 Kotlin 文件。
- `connectedCheck --parallel`：在模拟器设备锁内执行，6 个模块各 1 个设备测试全部通过；真实任务时间区间验证最大设备任务并发数为 1，执行后释放设备锁。
- Detekt 的 11 个模块均生成 XML、HTML、TXT、SARIF、MD 报告；11 个实际分析任务最大并发数为 1，SARIF 汇总在全部生产任务结束后调度，模块与总报告均保留同一条诊断。
- 初次静态检查发现 `ExtFuncCompilerTest.kt:16` 的已有 `LongMethod`，并行和 `--no-parallel` 均以同一条诊断退出 1；串行 Detekt 的配置缓存成功存储并在下一次调用复用。后续将内嵌示例源码提取为文件级 `annotationVariantsSource` fixture，保留 `trimIndent` 后的源码内容和全部 golden 断言，解决该问题，没有修改 baseline 或增加规则抑制。
- 修复后运行 `:ext-func-compiler:test --rerun detekt --no-auto-correct --continue --parallel`：重新编译并执行的 2 个编译器测试全部通过，11 个模块的 Detekt 全部重新执行，模块及 SARIF 汇总诊断均为 0，构建成功。日志位于 `build/issue32-longmethod-fix.log`。上表性能数据保留自 fixture 提取前的测量。

报告与验收日志分别位于 `build/issue32-reports.log`、`build/issue32-device.log`、`build/issue32-detekt-parallel.log`、`build/issue32-detekt-serial.log` 和 `build/issue32-detekt-serial-reuse.log`。

## ViewHolder 生命周期

`AbstractViewHolder` 在 RecyclerView attach 时监听所属 Fragment 的 Lifecycle；没有 Fragment 时使用 ComponentActivity。宿主 START/RESUME/PAUSE/STOP 驱动 holder 的内部 Lifecycle。STOP 保留监听，以便宿主恢复时接收 START；回收或宿主 DESTROY 时移除监听并销毁内部 owner。

注册时必须保存实际的 Lifecycle，解绑时使用同一个对象。回收的行视图可能已经离开 Fragment 的 view tree，再从 `itemView` 查找 owner 会回退到 Activity，导致 Fragment 上残留 observer。生命周期回调不应再次从视图查找 owner 或重新注册 observer。

Fragment 中的数据订阅仍应使用 `viewLifecycleOwner`，adapter 应按 view lifecycle 创建和清理。这与 holder 内部的 observer 管理独立，不能替代库自身的解绑。

`AbstractViewHolderTest` 覆盖行视图脱离父树后回收、宿主恢复与销毁。`FragmentGridNavigationTest` 使用真实 RecyclerView 和 FragmentManager，覆盖列表切换到网格后压入子 Fragment，再返回原网格。两者固定使用 Robolectric API 36；测试 JVM 的 `jdk.internal.access` 导出仅用于 Robolectric 的 shared-memory 初始化，不用于运行时库或应用。
