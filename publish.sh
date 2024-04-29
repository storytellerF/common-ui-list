# publish in jitpack
# group 和版本号使用jitpack 提供的
sh gradlew clean -xtest -xlint assemble publishToMavenLocal :version-manager:publishToMavenLocal