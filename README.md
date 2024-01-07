# common-ui-list

[![codecov](https://codecov.io/gh/storytellerF/common-ui-list/graph/badge.svg?token=5IK0PP6G9G)](https://codecov.io/gh/storytellerF/common-ui-list)

[![JITPACK](https://jitpack.io/v/storytellerF/common-ui-list.svg)](https://jitpack.io/#storytellerF/common-ui-list)

兼容性对比：

```kotlin
val androidVersion = "8.2.0"
val kotlinVersion = "1.9.20"
val kspVersion = "1.9.20-1.0.14"
object Dependencies {
    const val COMPOSE_COMPILER = "1.5.4"
}
val gradleVersion = "8.4"
```

## Build

```shell
//build
sh gradlew build
//publish
sh gradlew clean -Pgroup=com.github.storytellerF -xtest -xlint assemble publishToMavenLocal :version-manager:publishToMavenLocal
```

## Usage

导入**version-manager**

```kts
buildscript {
    dependencies {
        classpath("com.github.storytellerF.common-ui-list:version-manager:$latestVersion")
    }
}
pluginManagement {
    repositories {
        //...
        maven { setUrl("https://jitpack.io") }
    }
}
```

在app 模块中

```kts
constraintCommonUIListVersion("e0696030b5")
baseApp()
```

具体可以参照[GiantExplorer](https://github.com/storytellerF/common-ui-list-structure/tree/master/examples/GiantExplorer)
