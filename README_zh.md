# conventions plugin for gradle

[![](https://img.shields.io/badge/中文-README-lightgreen.svg)](https://github.com/5hmlA/conventions/blob/main/README_zh.md)

![](https://img.shields.io/badge/Android-Plugins-brightgreen.svg)

[![License](https://img.shields.io/badge/LICENSE-Apache%202-green.svg?style=flat-square)](https://www.apache.org/licenses/LICENSE-2.0)

![](https://img.shields.io/badge/Android%20Gradle%20Plugin-8.3+-lightgreen.svg)

# 介绍
一些简化配置gradle项目的插件，可以提高gradle项目配置的效率，特别是在多模块项目中，能够统一各个模块的一致性，且支持配置具体依赖的版本，一处修改全局生效

# 开始
## 首先你必须启用*version catalog*
- 你可以从[android/nowinandroid](https://github.com/android/nowinandroid)获取(当然也可以从本项目获取) ```libs.versions.toml```文件配置到你项目的gradle目录下
- 你可以自定义修改 ```libs.versions.toml```中的版本号，注意只能修改版本号
## 插件使用
### 1，为Android项目配置compose能力，添加此插件后即可在项目中使用compose
```kotlin
plugins {
    id("com.android.application")
    id("io.github.5hmlA.android.compose")
}

android {
    namespace = "yor applicationId"
}
```
在使用此插件之前你必须配置如下, 多模块的项目中每个模块都要这么配置
```kotlin
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_20
        targetCompatibility = JavaVersion.VERSION_20
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = libs.versions.androidx.compose.compiler.get()
    }
    namespace = "yor applicationId"
}

dependencies {
    //一些android项目必要的依赖
    ...
    //compose 必要的依赖
    val composeBom = platform(libs.androidx.compose.bom)
    implementation(composeBom)
    implementation(libs.bundles.compose)
    androidTestImplementation(composeBom)
    debugImplementation(libs.androidx.compose.ui.tooling)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
```

### 2， Android项目的基础配置，添加如下插件即可，此插件会自动添加Android项目必要的依赖
```kotlin
plugins {
    id("com.android.application")
    id("io.github.5hmlA.android")
}
```

### 3， 为gradle项目配置protobuf, 配置此插件之后即可在项目中正常使用protobuf不需要额外配置
```kotlin
plugins {
    id("io.github.5hmlA.protobuf")
}
```

### 4，组合使用插件，android项目同时使用compose和protobuf
```kotlin
plugins {
    id("com.android.application")
    id("io.github.5hmlA.android.compose")
    id("io.github.5hmlA.protobuf")
}
```

#### 5 简化AGP api的使用，隔离AGP不同版本api的差异
```kotlin
plugins {
    id("com.android.application")
    id("io.github.5hmlA.knife")
}
```
如果你要监听Android项目中apk的生成，然后做一些操作比如备份apk，重签名等
使用 knife 插件简化如下, 如果使用agp你必须通过自定义Task完成
```kotlin
knife {
    onVariants { variants ->
        if (variants.name.contains("debug")) {
            knifeActions {
                asmTransform {
                    //配置ASM处理
                    //格式为【完整类名#方法名#方法签名|*|?】=>【完整类名#方法名#方法签名|*|?】->【完整类名】
                    // 1 置空方法的实现：
                    // "com.osp.app.MainActivity#testEmpty#*"
                    // 把类MainActivity里面的testEmpty方法实现置空
                    // 2 移除方法的调用：
                    // "com.osp.app.MainActivity#onCreate#*=>*#testRemove#*"
                    // 移除 类MainActivity方法onCreate里面调用的testRemove()
                    // 3 修改方法的调用：这里必须注意，如果替换的是对象的调用，静态方法必须第一个参数是那个对象 
                    // "com.osp.app.MainActivity#testChange#?=>java/io/PrintStream#println#*->hello/change"
                    // 修改 类MainActivity方法testChange里面调用的System.out.println()修改为静态调用hello.change.println()
                    configs(
                        "com.osp.app.MainActivity#testChange#?=>java/io/PrintStream#println#*->hello/change",
                        "com.osp.app.MainActivity#testEmpty#*",
                        "com.osp.app.MainActivity#onCreate#*=>*#testRemove#*",
                    )
                }
                onArtifactBuilt {
                    //当apk编译好的时候回调，备份apk
                    copy {
                        //copy apk to rootDir
                        from(it)
                        //into a directory
                        into(rootDir.absolutePath)
                    }
                }
            }
        }
    }
}
```
>todo更多简化api正在持续开发中..
