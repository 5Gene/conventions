
plugins {
    `kotlin-dsl`
    //https://plugins.gradle.org/plugin/org.gradle.kotlin.kotlin-dsl
    //https://docs.gradle.org/current/userguide/kotlin_dsl.html
//    id("org.gradle.kotlin.kotlin-dsl") version "4.4.0"
    `kotlin-dsl-precompiled-script-plugins`
//    `java-gradle-plugin`
//    `maven-publish`
    //define『plugin portal -> publishPlugins』 task
    id("com.gradle.plugin-publish") version "1.3.0"
}

repositories {
    gradlePluginPortal()
    google()
}

dependencies {
    //includeBuild()中拿不到项目的properties，这里通过System.property取
    //编译插件的时候就会用到，不需要配置，编译的时候修改就行了
    compileOnly("com.android.tools.build:gradle-api:${vcl.versions.android.gradle.plugin.get()}")
    //compileOnly("com.android.tools.build:gradle:${libs.versions.android.gradle.plugin.get()}")
    //gradle plugin id 规则 plugin_id:plugin_id.gradle.plugin:version
    compileOnly("com.google.devtools.ksp:com.google.devtools.ksp.gradle.plugin:${vcl.versions.google.ksp.get()}")
    compileOnly("androidx.room:androidx.room.gradle.plugin:${vcl.versions.androidx.room.get()}")
    compileOnly("org.jetbrains.kotlin.plugin.compose:org.jetbrains.kotlin.plugin.compose.gradle.plugin:${vcl.versions.kotlin.get()}")
//    https://mvnrepository.com/artifact/org.jetbrains.kotlin/kotlin-gradle-plugin
//    https://plugins.gradle.org/plugin/org.jetbrains.kotlin.android
//    https://github.com/JetBrains/kotlin/
//    kotlin("gradle-plugin", "1.9.24") == org.jetbrains.kotlin:kotlin-gradle-plugin:1.9.24

    compileOnly(libs.plugin.publish)
    implementation(kotlin(module = "gradle-plugin", version = vcl.versions.kotlin.get()))
    implementation("com.google.protobuf:protobuf-gradle-plugin:${vcl.versions.protobuf.plugin.get()}")
    testImplementation(vcl.test.junit)
//    compileOnly(gradleKotlinDsl())
    // help->dependencies只会输出implementation的库的依赖关系
}

group = "io.github.5hmlA"
version = libs.versions.gene.conventions.get()

//afterEvaluate {
//    //不打包源码
//    tasks.named<Jar>("sourcesJar") {
//        exclude("**/*")
//    }
//}

publishing {
    //MavenLocal本地地址默认为："${System.getProperty("user.home")}/.m2/repository"
    repositories {
        maven {
            name = "ZoyLocal"
            setUrl(".maven")
        }
    }
}

gradlePlugin {
    website = "https://github.com/5hmlA/conventions"
    vcsUrl = "https://github.com/5hmlA/conventions"

    plugins {
        register("android-config") {
            id = "${group}.android"
            displayName = "android config plugin"
            description = "android build common config for build.gradle, this will auto add android necessary dependencies"
            tags = listOf("config", "android", "convention")
            implementationClass = "june.plugins.android.AndroidPlugin"
        }
        register("android-compose") {
            id = "${group}.android.compose"
            displayName = "android compose config plugin"
            description = "android compose config for build.gradle, necessary related settings for compose will be automatically set"
            tags = listOf("compose", "config", "android", "convention")
            implementationClass = "june.plugins.android.AndroidComposePlugin"
        }
        register("protobuf-config") {
            id = "${group}.protobuf"
            displayName = "protobuf config plugin"
            description =
                "protobuf config for any gradle project, necessary configuration and dependencies will be automatically set up"
            tags = listOf("protobuf", "config", "convention")
            implementationClass = "june.plugins.ProtobufPlugin"
        }
        register("agp-knife") {
            id = "${group}.knife"
            displayName = "agp knife plugin"
            description = "Simplify the use of complex agp api and isolate the differences between different agp versions"
            tags = listOf("android gradle plugin", "knife", "convention")
            implementationClass = "june.plugins.android.AGPKnifePlugin"
        }

        //因为xxx.gradle.kts注册插件的时候不会设置displayName 尝试这里覆盖注册，结果无效，
        //publishTask里会检测所有的plugin,被认为是重复注册了直接报错,所以同一个plugin再创建个id
        create("proto-convention") {
            id = "${group}.protobuf-convention"
            displayName = "protobuf convention plugin"
            description =
                "protobuf convention for any gradle project, necessary configuration and dependencies will be automatically set up"
            tags = listOf("protobuf", "config", "convention")
            implementationClass = "ProtobufConventionPlugin"
        }

    }
    //因为通过 xxx.gradle.kts创建的预编译脚本 会自动创建plugin但是没设置displayName和description
    //所以这里判断补充必要数据否则发布不了，执行 [plugin portal -> publishPlugins]的时候会报错
    val plugins = extensions.getByType<GradlePluginDevelopmentExtension>().plugins
//    这里不修改 上传的时候再处理
//    plugins.forEach {
//        if (it.displayName.isNullOrEmpty()) {
//            it.id = "$group.${it.id}"
//            it.displayName = "protobuf convention plugin"
//            it.description = "protobuf convention for any gradle project, necessary configuration and dependencies will be automatically set up"
//            it.tags = listOf("protobuf", "config", "convention")
//        }
//    }
    plugins.forEach {
        "- plugin -- ${it.name} ${it.id} ${it.displayName}".print()
    }
    "插件地址: https://plugins.gradle.org/u/ZuYun".print()
//    https://plugins.gradle.org/docs/mirroring
//    The URL to mirror is https://plugins.gradle.org/m2/
    "插件下载地址: https://plugins.gradle.org/m2/".print()
}


//插件推送之前 先去掉不符合规范的插件
tasks.findByName("publishPlugins")?.doFirst {
    //doFirst on task ':conventions:publishPlugins'
    ">> doFirst on $this ${this.javaClass}".print()
    //不太明白为什么这里也报错 Extension of type 'GradlePluginDevelopmentExtension' does not exist
    //因为取错对象的extensions了，这里的this是com.gradle.publish.PublishTask_Decorated, 这个task也有extensions
    val plugins = rootProject.extensions.getByType<GradlePluginDevelopmentExtension>().plugins
    plugins.removeIf {
        //移除不能上传的插件
        it.displayName.isNullOrEmpty()
    }
    plugins.forEach {
        "- plugin to publish > ${it.name} ${it.id} ${it.displayName}".print()
    }
}

tasks.getByName("publishPlugins").doLast {
    println("插件发布成功，点击🔗查看：https://plugins.gradle.org/")
}

//主动开启Junit,system.out日志输出显示在控制台,默认控制台不显示system.out输出的日志
//https://docs.gradle.org/current/kotlin-dsl/gradle/org.gradle.api.tasks.testing/-abstract-test-task/test-logging.html
//https://stackoverflow.com/questions/9356543/logging-while-testing-through-gradle
tasks.withType<Test>() {
    testLogging {
        showStandardStreams = true
//        testLogging.exceptionFormat = TestExceptionFormat.FULL
    }
}

fun String.print() {
    println("\u001B[93m✨ $name >> ${this}\u001B[0m")
}


//创建tag
//git tag v2.1
//git push origin v2.1

//查看/删除远端所有tag
//git ls-remote --tags origin
//git push origin --delete $(git tag -l)

//查看/删除本地所有tag
//git tag
//git tag -d $(git tag)


