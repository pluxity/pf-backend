package com.pluxity.weekly.dashboard.service

import com.pluxity.weekly.dashboard.dto.WorkerDashboardResponse
import com.pluxity.weekly.dashboard.dto.WorkerEpicItem
import com.pluxity.weekly.dashboard.dto.WorkerSummary
import com.pluxity.weekly.dashboard.dto.WorkerTaskItem
import com.pluxity.weekly.epic.repository.EpicRepository
import com.pluxity.weekly.global.auth.AuthorizationService
import com.pluxity.weekly.task.entity.Task
import com.pluxity.weekly.task.entity.TaskStatus
import com.pluxity.weekly.task.repository.TaskRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.temporal.ChronoUnit

@Service
@Transactional(readOnly = true)
class DashboardService(
    private val authorizationService: AuthorizationService,
    private val epicRepository: EpicRepository,
    private val taskRepository: TaskRepository,
) {
    fun getWorkerDashboard(): WorkerDashboardResponse {
        val user = authorizationService.currentUser()
        val userId = user.requiredId

        val epics = epicRepository.findByAssignmentsUserIdWithProject(userId)
        val tasks =
            taskRepository.findByAssigneeId(userId)
        val tasksByEpicId = tasks.groupBy { it.epic.requiredId }
        val now = LocalDate.now()

        return WorkerDashboardResponse(
            summary = buildSummary(tasks, now),
            epics =
                epics.map { epic ->
                    val epicTasks = tasksByEpicId[epic.requiredId] ?: emptyList()
                    WorkerEpicItem(
                        epicId = epic.requiredId,
                        epicName = epic.name,
                        projectId = epic.project.requiredId,
                        projectName = epic.project.name,
                        status = epic.status,
                        progress = if (epicTasks.isEmpty()) 0 else epicTasks.map { it.progress }.average().toInt(),
                        startDate = epic.startDate,
                        dueDate = epic.dueDate,
                        tasks = epicTasks.map { it.toWorkerTaskItem(now) },
                    )
                },
        )
    }

    private fun buildSummary(
        tasks: List<Task>,
        now: LocalDate,
    ): WorkerSummary =
        WorkerSummary(
            approachingDeadline =
                tasks.count { task ->
                    task.dueDate != null &&
                        task.status != TaskStatus.DONE &&
                        ChronoUnit.DAYS.between(now, task.dueDate) in 0..7
                },
            inProgress = tasks.count { it.status == TaskStatus.IN_PROGRESS },
            completed = tasks.count { it.status == TaskStatus.DONE },
            total = tasks.size,
        )

    private fun Task.toWorkerTaskItem(now: LocalDate): WorkerTaskItem =
        WorkerTaskItem(
            taskId = this.requiredId,
            taskName = this.name,
            status = this.status,
            progress = this.progress,
            dueDate = this.dueDate,
            daysUntilDue = this.dueDate?.let { ChronoUnit.DAYS.between(now, it).toInt() },
        )
}
