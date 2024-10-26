package june.knife

import com.android.build.api.variant.BuiltArtifactsLoader
import june.wing.blue
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

abstract class TaskListenApk : DefaultTask() {

    //属性需要一个值，但未提供任何值。默认情况下，Gradle 属性是必需的，也就是说，如果未配置输入或输出属性，
    // 无论是通过常规值还是通过构建脚本显式配置，Gradle 都会失败，因为它不知道要使用什么值当任务执行时。
    //要解决该问题，您必须：
    // - 显式为此属性提供一个值（例如通过在构建脚本中配置任务）
    // - 或者通过注释使属性可选@get:Optional
    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:Optional //必须配置为Optional,因为apk只在application项目中才有
    abstract val apkFolder: DirectoryProperty

    @get:InputFile
    @get:Optional //必须配置为Optional,因为aar只在library项目中才有
    abstract val aarFile: RegularFileProperty

    @get:Internal
    abstract val builtArtifactsLoader: Property<BuiltArtifactsLoader>

    //@Internal 注解
    //附加到任务属性以指示在进行最新检查时不考虑该属性。
    //该注释应该附加到 Java 中的 getter 方法或 Groovy 中的属性上。 Java 中 setter 上的注释或仅字段的注释将被忽略。
    //这将导致当属性发生变化时任务不会被认为是过时的。
    @get:Internal
    var listenArtifact: ((String) -> Unit)? = null

    @TaskAction
    fun taskAction(){
        apkFolder.orNull?.let {
            //application项目
            val builtArtifacts = builtArtifactsLoader.get().load(it)
                ?: throw RuntimeException("Cannot load APKs")
            builtArtifacts.elements.forEach {
                listenArtifact?.invoke(it.outputFile)
                println("> Got an APK at ${it.outputFile}".blue)
            }
        }
        aarFile.orNull?.let {
            //library项目
            val file = it.asFile
            listenArtifact?.invoke(file.absolutePath)
            println("> Got an AAR at $file".blue)
        }
        // The above will only save the artifact themselves. It will not save the
        // metadata associated with them. Depending on our needs we may need to copy it.
        // This is required when transforming such an artifact. We'll do it here for demonstration
        // purpose.
//        builtArtifacts.save(outputDirectory)
    }
}