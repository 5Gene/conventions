package june.wing

import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.execution.TaskExecutionGraph
import kotlin.text.contains

fun Project.disableLint() {
    modifyTask {
        if (name.contains("lint")) {
            enabled = false
        }
    }
}

fun Project.modifyTask(modify: Task.() -> Unit) {
    tasks.whenTaskAdded {
        modify()
    }
}

fun Project.modifyTaskWhenReady(action: Action<TaskExecutionGraph>) {
    gradle.taskGraph.whenReady(action)
}