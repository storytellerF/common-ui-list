# 开发与验证

使用 JDK 21 和 Android SDK API 37。通过本地 `local.properties` 的 `sdk.dir` 或 `ANDROID_HOME` 指定 SDK，在仓库根目录执行：

```sh
./gradlew :ui-list:testDebugUnitTest :ui-list:assembleDebug :ui-list:detekt
```

## Gradle 并行构建

默认开启跨项目并行，最多 4 个 worker；不设置 `Test.maxParallelForks`，保留 Gradle 默认的单 fork。Gradle daemon 的 4 GiB 堆限制不包含测试 JVM、Kotlin daemon 和其他 worker。内存较小的环境可用 `--max-workers=2`，或用 `--no-parallel` 退回串行。

各模块的编译、KSP 生成代码、测试和 Kover 报告使用各自的 `build/` 目录。Kover 汇总通过项目依赖收集产物。Detekt 默认只读，可与编译、测试在同一次调用中执行；需要修复格式时显式运行 `./gradlew detekt --auto-correct`。只有显式自动修复时需要避免与读取同一源码的编译或分析任务重叠，修复后重新验证编译和测试。Detekt 内部 AST 并行默认开启，与 Gradle 跨项目并行独立；`--no-parallel` 只关闭后者，不会关闭 AST 并行。

为保护显式自动修复写入的源码和共享 baseline，分析任务与 baseline 生成任务仍共用一个独占 build service；SARIF 汇总等待本次调度的所有 Detekt 生产任务结束。共享的 `config/detekt/baseline.xml` 仍为现有全局基线，更新它时应显式选择一个模块的 baseline 任务并审查差异，不能把多模块依次覆盖的结果当作合并基线。

当前 `excludeCorrectable: false` 策略会继续报告当次发现的可修复违规，因此 `--auto-correct` 即使已修复源码也可能返回失败；修复后重新运行普通 Detekt 确认通过。

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

### Issue #39：Detekt AST 并行基准（2026-10-25）

基于提交 `e8a6bd2`，在 Windows 11、28 逻辑 CPU、31.84 GiB RAM、Temurin 21.0.12.1、Gradle 9.8.1、Detekt 1.23.8 上比较 `parallel=false/true`。固定 Gradle 跨模块并行、4 个 worker、4 GiB daemon 堆、现有 Detekt 独占 service、规则和 baseline，不启用 Worker API；自动修复关闭。

显式运行 11 个模块的普通 `:模块:detekt`（共 88 个源码文件），用任务级 `--rerun` 强制分析，禁用构建及配置缓存。依赖先预热，离线测量；每个场景各三组，交替 false→true、true→false、false→true，共 18 次正式调用。新进程场景每次启动 single-use daemon；预热场景在同一个独立 daemon 上先分别预热两个开关，再采集六次正式样本。

| 场景 | 关闭：整次调用中位数（秒） | 开启：整次调用中位数（秒） | 缩短 | 关闭/开启：Detekt 任务合计中位数（秒） |
| --- | ---: | ---: | ---: | ---: |
| 新进程，清除 Detekt 报告 | 30.991 | 25.902 | 16.4% | 8.249 / 5.195 |
| 新进程，保留报告并强制重跑 | 31.890 | 28.603 | 10.3% | 8.208 / 5.726 |
| 复用已预热的 daemon，强制重跑 | 12.360 | 10.648 | 13.9% | 3.795 / 1.381 |

新进程场景仍保留编译产物、下载依赖和系统文件缓存，不代表首次 checkout 的完整冷构建。预热进程逐轮整次调用 false/true 为 12.360/10.848、11.582/10.648、15.934/9.192 秒；三轮开启均更快。Detekt 任务计时包含插件与报告工作，不是纯 AST 解析时间。

每 200 ms 对本次进程树同时采样并求和。全部样本最高 RSS 为 1807.2 MiB、Windows private bytes 为 2212.8 MiB；新进程且保留报告的场景中，开启后的峰值中位数分别增加 228.5 MiB 和 276.3 MiB，其他场景未观察到增加。预热 daemon 的累计 JVM 状态会影响内存，不能将这些差异完全归因于开关，也不能声称并行省内存。

18 次分析全部成功，每次 11 个任务实际执行。每次的 55 份模块报告及合并 SARIF 在去除 HTML/Markdown 生成时间后完全一致，findings 均为 0；已跟踪文件的 SHA-256 未改变。此基准未覆盖自动修复、有违规输入或类型解析任务。共享机器最低可用内存曾降至 693.8 MiB，且一次关闭并行的调用达到 43.035 秒；所有样本均保留，未删除慢样本。小样本结果不能保证目标 CI 上的收益，尤其是低核或内存紧张的 runner；Gradle worker 上限也不等于 Detekt 内部线程上限。

基于本机预热约省 1.7 秒、新进程约省 3–5 秒且报告一致，开启共享 `detekt { parallel = true }`。保留只读默认、现有共享锁与全部报告。原始日志、逐次内存与报告副本位于本地 `build/issue39-benchmark/`，不纳入版本控制。

## ViewHolder 生命周期

`AbstractViewHolder` 在 RecyclerView attach 时监听所属 Fragment 的 Lifecycle；没有 Fragment 时使用 ComponentActivity。宿主 START/RESUME/PAUSE/STOP 驱动 holder 的内部 Lifecycle。STOP 保留监听，以便宿主恢复时接收 START；回收或宿主 DESTROY 时移除监听并销毁内部 owner。

注册时必须保存实际的 Lifecycle，解绑时使用同一个对象。回收的行视图可能已经离开 Fragment 的 view tree，再从 `itemView` 查找 owner 会回退到 Activity，导致 Fragment 上残留 observer。生命周期回调不应再次从视图查找 owner 或重新注册 observer。

Fragment 中的数据订阅仍应使用 `viewLifecycleOwner`，adapter 应按 view lifecycle 创建和清理。这与 holder 内部的 observer 管理独立，不能替代库自身的解绑。

`AbstractViewHolderTest` 覆盖行视图脱离父树后回收、宿主恢复与销毁。`FragmentGridNavigationTest` 使用真实 RecyclerView 和 FragmentManager，覆盖列表切换到网格后压入子 Fragment，再返回原网格。两者固定使用 Robolectric API 36；测试 JVM 的 `jdk.internal.access` 导出仅用于 Robolectric 的 shared-memory 初始化，不用于运行时库或应用。

## KSP 校验与弃用诊断（Issue #35）

两个编译器显式使用 `validate(enableNewFeatures = false)`，与 KSP 2.3.12 的旧重载行为一致。
当前生成器未提供 context parameters 和 explicit backing fields 的生成支持，因此不在此次弃用迁移中扩大校验范围。
之后支持这些语言特性时，应一起更新校验选项、生成器及对应测试。

生成文件会聚合同一批符号。若本轮存在不可解析的符号，两个处理器均返回整批源符号，暂不写文件；
下一轮重新获取符号，避免漏掉已有效的事件或生成部分文件后重复写入。测试使用另一轮生成的类型别名验证恢复，
并覆盖始终无法解析时不生成 builder、有效与无效符号混合、原有 golden 输出和生成回调行为。

验证命令：

```sh
./gradlew :ext-func-compiler:test :ui-list-annotation-compiler-ksp:test :ext-func-compiler:detekt --no-auto-correct :ui-list-annotation-compiler-ksp:detekt --no-auto-correct --no-parallel
./gradlew help --warning-mode all --no-configuration-cache -Dorg.gradle.deprecation.trace=true
```

2026-10-25 在 JDK 21 / Gradle 9.8.1 上验证：两个编译器共 10 个测试通过，保留原有 golden；两个模块的 Detekt 通过。
发布验证使用临时 init script，仅对两个模块关闭签名任务并移除 `.asc` 发布附件，验证 `publishToMavenLocal`；
正式发布配置没有修改。当前环境没有发布签名密钥，未验证签名或执行 Maven Central 发布，正式发布仍由带密钥的上游 CI 执行。

使用 `--warning-mode all` 和 `-Dorg.gradle.deprecation.trace=true` 展开后的来源：

- 项目根脚本的 `tasks.registering` 委托语法（Gradle 10 移除）：已改为 `tasks.register<ReportMergeTask>`，保留延迟配置。
- Detekt 1.23.8 的 `DetektPlugin.apply`（`DetektPlugin.kt:28`）调用 `ReportingExtension.file(String)`，将在 Gradle 10 移除。
- 同一插件的 `configurePluginDependencies`（`DetektPlugin.kt:99,107`）调用 `Configuration.setVisible(boolean)`，将在 Gradle 11 移除。

后两项来自第三方插件，仍会报告；本次没有屏蔽警告或跳过符号校验，也不声称已兼容 Gradle 10。
升级 Detekt 时需同时验证规则、格式化插件、baseline 和 SARIF 汇总，不能只替换版本来消除提示。
