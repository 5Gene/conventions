# conventions plugin for gradle

![](https://img.shields.io/badge/Android-Plugins-brightgreen.svg)

[![License](https://img.shields.io/badge/LICENSE-Apache%202-green.svg?style=flat-square)](https://www.apache.org/licenses/LICENSE-2.0) 

![](https://img.shields.io/badge/Android%20Gradle%20Plugin-8.3+-lightgreen.svg)

# Summary
Some plugins that simplify the configuration of gradle projects can improve the efficiency of gradle project configuration. Especially in multi-module projects, they can unify the consistency of each module and support the configuration of specific dependent versions. Modifications in one place will take effect globally.

# Getting Start

![](https://img.shields.io/badge/java-17-lightgreen.svg)
![](https://img.shields.io/badge/kotlin-2.0.0-lightgreen.svg)

## First you have to enable *version catalog*
- You can get ````libs.versions.toml```` file from [android/nowinandroid](https://github.com/android/nowinandroid) (of course you can also get it from this project) , then Configure it in the gradle directory of your project
- You can customize the version number in ````libs.versions.toml```` . Note that you can only modify the version number.
## Plugin usage
### 1，Configure the compose capability for the Android project. After adding this plug-in, you can use compose in the project
```kotlin
plugins {
    id("com.android.application")
    id("io.github.5hmlA.android.compose")
}

android {
    namespace = "yor applicationId"
}
```
~~Before using this plug-in, you must configure it as follows. In a multi-module project, each module must be configured like this.~~

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
    //Some necessary dependencies for android projects
    //...
    //necessary dependencies for compose
    val composeBom = platform(libs.androidx.compose.bom)
    implementation(composeBom)
    implementation(libs.bundles.compose)
    androidTestImplementation(composeBom)
    debugImplementation(libs.androidx.compose.ui.tooling)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
```


### 2， For the basic configuration of the Android project, just add the following plug-in. This plug-in will automatically add the necessary dependencies for the Android project.
```kotlin
plugins {
    id("com.android.application")
    id("io.github.5hmlA.android")
}
```

### 3， Configure protobuf for the gradle project. After configuring this plug-in, protobuf can be used normally in the project without additional configuration.
```kotlin
plugins {
    id("io.github.5hmlA.protobuf")
}
```

### 4，Use plug-ins in combination, android projects use compose and protobuf at the same time
```kotlin
plugins {
    id("com.android.application")
    id("io.github.5hmlA.android.compose")
    id("io.github.5hmlA.protobuf")
}
```

### 5， Simplify the use of AGP API and isolate the differences between different versions of AGP API
```kotlin
plugins {
    id("com.android.application")
    id("io.github.5hmlA.knife")
}
```
If you want to monitor the generation of apk in the Android project, and then do some operations such as backing up the apk, re-signing, etc.
Using the knife plug-in is simplified as follows, if you use agp you must complete it through a custom Task
```kotlin
knife {
    onVariants { variants ->
        if (variants.name.contains("debug")) {
            utility {
                asmTransform {
                    // Configure ASM processing
                    // Format: [full.class.name#MethodName|*#descriptor|*]=>[full.class.name|className|*#MethodName|*#descriptor|*]->[full.class.name]
                    // 1. Empty the method implementation:
                    // "com.osp.app.MainActivity#testEmpty#*"
                    // Empties the implementation of the testEmpty method in the MainActivity class.
                    // 2. Remove the method call:
                    // "com.osp.app.MainActivity#onCreate#*=>*#testRemove#*"
                    // Removes the testRemove() call inside the onCreate method of the MainActivity class.
                    // 3. Modify the method call:
                    // "com.osp.app.MainActivity#testChange#?=>java/io/PrintStream#println#*->hello.change"
                    // Modifies the System.out.println() call inside the testChange method of the MainActivity class to a static call to hello.change.println().
                    configs(
                        //change invoke owner [PrintStream.println()->hello.change.println()] in MainActivity.testChange
                        "com.osp.app.MainActivity#testChange#*=>java/io/PrintStream#println#*->hello/change",
                        //empty all fun name testEmpty in com.osp.app.MainActivity
                        "com.osp.app.MainActivity#testEmpty#*",
                        //empty all method in com.osp.app.RemoveAllMethod
                        "com.osp.app.RemoveAllMethod#*#*",
                        //remove all any owner invoke testRemove in fun MainActivity.onCreate
                        "com.osp.app.MainActivity#onCreate#*=>*#testRemove#*",
                    )
                }
                onArtifactBuilt {
                    //Invoke a callback when the APK is compiled and back up the APK.
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
>TODO More simplified APIs are under continuous development..
