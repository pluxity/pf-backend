package com.pluxity.weekly.chat.context

import com.pluxity.common.auth.user.repository.UserRepository
import com.pluxity.common.core.exception.CustomException
import com.pluxity.weekly.epic.entity.Epic
import com.pluxity.weekly.epic.repository.EpicRepository
import com.pluxity.weekly.global.constant.WeeklyReportErrorCode
import com.pluxity.weekly.project.entity.Project
import com.pluxity.weekly.task.entity.Task
import com.pluxity.weekly.task.repository.TaskRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper
import java.time.LocalDate

@Component
class ContextBuilder(
    private val epicRepository: EpicRepository,
    private val taskRepository: TaskRepository,
    private val userRepository: UserRepository,
    private val objectMapper: ObjectMapper,
) {
    // TODO: 현재는 worker전용(본인 task, 할당된 epic만)
    fun build(userId: Long): String {
        val user =
            userRepository.findByIdOrNull(userId)
                ?: throw CustomException(WeeklyReportErrorCode.NOT_FOUND_USER, userId)

        val epics = epicRepository.findByAssignmentsAssignedById(userId)

        val tasksByEpicId =
            if (epics.isEmpty()) {
                emptyMap()
            } else {
                taskRepository.findByEpicInAndAssigneeId(epics, userId).groupBy { it.epic.requiredId }
            }
        val epicsByProject = epics.groupBy { it.project.requiredId }

        val context =
            mapOf(
                "today" to LocalDate.now().toString(),
                "user" to mapOf("id" to user.requiredId, "name" to user.name),
                "projects" to
                    epicsByProject.map { (_, epics) ->
                        projectMap(epics.first().project, epics, tasksByEpicId)
                    },
            )

        return objectMapper.writeValueAsString(context)
    }

    private fun projectMap(
        project: Project,
        epics: List<Epic>,
        tasksByEpicId: Map<Long, List<Task>>,
    ): Map<String, Any?> =
        mapOf(
            "id" to project.requiredId,
            "name" to project.name,
            "epics" to
                epics.map { epic ->
                    epicMap(epic, tasksByEpicId[epic.requiredId] ?: emptyList())
                },
        )

    private fun epicMap(
        epic: Epic,
        tasks: List<Task>,
    ): Map<String, Any?> =
        mapOf(
            "id" to epic.requiredId,
            "name" to epic.name,
            "tasks" to tasks.map { taskMap(it) },
        )

    private fun taskMap(task: Task): Map<String, Any?> =
        mapOf(
            "id" to task.requiredId,
            "name" to task.name,
            "status" to task.status.name,
            "progress" to task.progress,
        )
}
