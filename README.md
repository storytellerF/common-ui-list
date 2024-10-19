# common-ui-list

[![codecov](https://codecov.io/gh/storytellerF/common-ui-list/graph/badge.svg?token=5IK0PP6G9G)](https://codecov.io/gh/storytellerF/common-ui-list)

[![JITPACK](https://jitpack.io/v/storytellerF/common-ui-list.svg)](https://jitpack.io/#storytellerF/common-ui-list)

## Build

```shell
//build
sh gradlew build
//publish
sh gradlew -Pgroup=com.storyteller_f.common-ui-list clean -xtest -xlint assemble publishToMavenLocal :version-manager:publishToMavenLocal
//也可以选择和jitpack 使用相同的group。
//-Pgroup=com.github.storytellerF.common-ui-list -Pversion=version
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

具体可以参照[GiantExplorer](https://github.com/storytellerF/GiantExplorer) 和[Ping](https://github.com/storytellerF/Ping)

## 兼容性

[兼容性](gradle/libs.versions.toml)