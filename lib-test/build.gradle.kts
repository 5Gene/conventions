import wing.publish5hmlA

plugins {
    alias(vcl.plugins.android.library) apply true
    alias(vcl.plugins.kotlin.android) apply false
    alias(vcl.plugins.room) apply false
    alias(vcl.plugins.ksp) apply false
    id("io.github.5hmlA.android")
    id("io.github.5hmlA.knife")
}

knife {
    println("--knife ->------- build config")
    onVariants {
        println("--knife ->------- build config $it")
        if (it.name.contains("debug")) {
            utility {
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
}

android {
    namespace = "com.osp.lib"

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    implementation(vcl.bundles.android.view)
}

//publish5hmlA("test", withSource = true)
publish5hmlA("test")
//publishMavenCentral("test", "debug", true)
//publishMavenCentral("test")