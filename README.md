# conventions plugin for gradle

# Summary
Some plugins that simplify the configuration of gradle projects can improve the efficiency of gradle project configuration. Especially in multi-module projects, they can unify the consistency of each module and support the configuration of specific dependent versions. Modifications in one place will take effect globally.

# Getting Start
## First you have to enable *version catalog*
- You can get ````libs.versions.toml```` file from [android/nowinandroid](https://github.com/android/nowinandroid) (of course you can also get it from this project) , then Configure it in the gradle directory of your project
- 你可以自定义修改 ```libs.versions.toml```中的版本号，注意只能修改版本号
## Plugin usage
##### 1，Configure the compose capability for the Android project. After adding this plug-in, you can use compose in the project
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
    ...
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


##### 2， For the basic configuration of the Android project, just add the following plug-in. This plug-in will automatically add the necessary dependencies for the Android project.
```kotlin
plugins {
    id("com.android.application")
    id("io.github.5hmlA.android")
}
```

##### 3， Configure protobuf for the gradle project. After configuring this plug-in, protobuf can be used normally in the project without additional configuration.
```kotlin
plugins {
    id("io.github.5hmlA.protobuf")
}
```

##### 4，Use plug-ins in combination, android projects use compose and protobuf at the same time
```kotlin
plugins {
    id("com.android.application")
    id("io.github.5hmlA.android.compose")
    id("io.github.5hmlA.protobuf")
}
```

#### 5， Simplify the use of AGP API and isolate the differences between different versions of AGP API
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
    onVariants {
        //Configure to back up apk only on release
        if (it.name.contains("release")) {
            onArtifactBuilt {
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
```
>TODO More simplified APIs are under continuous development..
