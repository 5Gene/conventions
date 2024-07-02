package tasks

import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileType
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.jetbrains.kotlin.konan.properties.loadProperties
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.assign
import org.gradle.kotlin.dsl.internal.sharedruntime.codegen.sourceNameOfBinaryName
import org.gradle.kotlin.dsl.register
import org.gradle.work.ChangeType
import org.gradle.work.Incremental
import org.gradle.work.InputChanges
import org.jetbrains.kotlin.gradle.internal.ConfigurationPhaseAware
import wing.androidExtension
import wing.javaExtension
import wing.srcDirs
import java.io.File

fun Project.generateGradlePropTask(): TaskProvider<GenerateGradlePropertiesTask> {
    javaExtension?.apply {
        sourceSets.getByName("main").java.srcDirs("build/generated/prop/main/kotlin")
    } ?: androidExtension?.srcDirs("build/generated/prop/main/kotlin")
    return tasks.register<GenerateGradlePropertiesTask>("gradleProperties") {
        group = "build"
        outputDir = layout.buildDirectory.dir("generated/prop/main/kotlin")
        inputFile = rootProject.file("gradle.properties")
        projectName = project.name
    }
}

abstract class GenerateGradlePropertiesTask : DefaultTask() {

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty
//    val outputDir: DirectoryProperty = objects.directoryProperty()

    @get:Incremental
    @get:InputFile
    abstract val inputFile: RegularFileProperty
//    val inputFile: RegularFileProperty = objects.fileProperty()

    @get:Input
    abstract val projectName: Property<String>

    //https://docs.gradle.org/current/userguide/custom_tasks.html#sec:implementing_an_incremental_task
    @TaskAction
    fun generate(inputs: InputChanges) {
        val msg = if (inputs.isIncremental) "CHANGED inputs are out of date" else "ALL inputs are out of date"
        println(msg)

        inputs.getFileChanges(inputFile).forEach { change ->
            if (change.fileType == FileType.DIRECTORY) return@forEach
            println("${change.changeType}: ${change.normalizedPath}")
            val properties = loadProperties(change.normalizedPath)
            val targetFile = outputDir.file("BuildProperties.kt").get().asFile
            targetFile.parentFile.mkdirs()
            if (change.changeType == ChangeType.REMOVED) {
                targetFile.delete()
            } else {
                targetFile.writeText("""
package ${projectName.get().replace("-", ".")}

internal object BuildProperties {
${
                    properties.filter { !it.key.toString().contains(".") }.map { (k, v) ->
                        "    internal const val ${k.toString().uppercase()}: String = \"$v\""
                    }.joinToString(separator = "\n")
                }
}
                """.trim()
                )
            }
        }
    }
}