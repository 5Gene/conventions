plugins {
    //之所以要[apply true]是因为没在顶层build.gradle中apply所以这里需要
    alias(vcl.plugins.android.application) apply true
    alias(vcl.plugins.kotlin.android) apply false
//    alias(libs.plugins.hilt) apply false
    alias(vcl.plugins.ksp) apply false
    alias(vcl.plugins.room) apply false
    alias(vcl.plugins.compose.compiler) apply false
    id("io.github.5hmlA.android.compose")
//    id("io.github.5hmlA.android")
    id("io.github.5hmlA.protobuf")
    id("io.github.5hmlA.knife")
}
//https://stackoverflow.com/questions/32352816/what-the-difference-in-applying-gradle-plugin?rq=2

knife {
    onVariants { variants ->
        if (variants.name.contains("debug")) {
            utility {
                asmTransform {
                    configs(
                        "com.osp.app.MainActivity#testChange#*=>java/io/PrintStream#println#*->com.osp.app.Hello",
                        "com.osp.app.MainActivity#onCreate#*=>*#method2#*->com.osp.app.Hello",
                        "com.osp.app.MainActivity#testChange#*=>*#testRemove#*",
                        "com.osp.app.EmptyAllMethod#*#*",
                        "com.osp.app.EmptyAllMethodObject#*#*",
                        "com.osp.app.MainActivity#testEmpty#*",
                        "com.osp.app.MainActivity#testEmptyList#*",
                        "com.osp.app.MainActivity#onCreate#*=>*#testRemove#*",
                        "com.osp.app.MainActivity#testTryCatch#*=>TryCatch",
                    )
                    execludes(
                        //"com/osp/app/*",
                        //"**/EmptyAllMethod*",
                        //排除所有EmptyAllMethodObject类
                        //"**/EmptyAllMethodObject",
                        //排除android*下的所有类
                        "android**",
                    )
                }
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
    namespace = "com.osp.app"

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

    androidComponents {
        onVariants { variants ->
            println("--androidComponents ->------- build config $variants")
        }
    }
}

dependencies {
    implementation(vcl.google.material)
//    implementation(project(":lib-test"))
}