package com.pluxity.weekly.task.service

import com.pluxity.common.auth.user.entity.User
import com.pluxity.common.auth.user.repository.UserRepository
import com.pluxity.common.core.exception.CustomException
import com.pluxity.common.core.utils.findAllNotNull
import com.pluxity.weekly.chat.dto.TaskSearchFilter
import com.pluxity.weekly.epic.entity.Epic
import com.pluxity.weekly.epic.repository.EpicRepository
import com.pluxity.weekly.global.auth.AuthorizationService
import com.pluxity.weekly.global.constant.WeeklyReportErrorCode
import com.pluxity.weekly.project.entity.Project
import com.pluxity.weekly.project.repository.ProjectRepository
import com.pluxity.weekly.task.dto.TaskRequest
import com.pluxity.weekly.task.dto.TaskResponse
import com.pluxity.weekly.task.dto.TaskUpdateRequest
import com.pluxity.weekly.task.dto.toResponse
import com.pluxity.weekly.task.entity.Task
import com.pluxity.weekly.task.repository.TaskRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class TaskService(
    private val taskRepository: TaskRepository,
    private val epicRepository: EpicRepository,
    private val userRepository: UserRepository,
    private val authorizationService: AuthorizationService,
    private val projectRepository: ProjectRepository,
) {
    fun findAll(): List<TaskResponse> {
        val user = authorizationService.currentUser()
        if (user.isAdmin()) return taskRepository.findAll().map { it.toResponse() }
        val pmProjects = projectRepository.findByPmId(user.requiredId)
        if (pmProjects.isNotEmpty()) {
            val epics = epicRepository.findByProjectIdIn(pmProjects.map { it.requiredId })
            return taskRepository.findByEpicIn(epics).map { it.toResponse() }
        }
        val epics = epicRepository.findByAssignmentsUserId(user.requiredId)
        if (epics.isEmpty()) return emptyList()
        return taskRepository.findByEpicInAndAssigneeId(epics, user.requiredId).map { it.toResponse() }
    }

    fun search(filter: TaskSearchFilter): List<TaskResponse> =
        taskRepository
            .findAllNotNull {
                select(entity(Task::class))
                    .from(entity(Task::class))
                    .whereAnd(
                        filter.status?.let { path(Task::status).eq(it) },
                        filter.epicId?.let { path(Task::epic)(Epic::id).eq(it) },
                        filter.projectId?.let { path(Task::epic)(Epic::project)(Project::id).eq(it) },
                        filter.assigneeId?.let { path(Task::assignee)(User::id).eq(it) },
                        filter.name?.let { path(Task::name).like("%$it%") },
                        filter.dueDateFrom?.let { path(Task::dueDate).greaterThanOrEqualTo(it) },
                        filter.dueDateTo?.let { path(Task::dueDate).lessThanOrEqualTo(it) },
                    )
            }.map { it.toResponse() }

    fun findById(id: Long): TaskResponse = getTaskById(id).toResponse()

    @Transactional
    fun create(request: TaskRequest): Long {
        val user = authorizationService.currentUser()
        authorizationService.requireEpicAccess(user, request.epicId)
        if (taskRepository.existsByEpicIdAndName(request.epicId, request.name)) {
            throw CustomException(WeeklyReportErrorCode.DUPLICATE_TASK, request.epicId, request.name)
        }
        return taskRepository
            .save(
                Task(
                    epic = getEpicById(request.epicId),
                    name = request.name,
                    description = request.description,
                    status = request.status,
                    progress = request.progress,
                    startDate = request.startDate,
                    dueDate = request.dueDate,
                    assignee = request.assigneeId?.let { getUserById(it) } ?: user,
                ),
            ).requiredId
    }

    @Transactional
    fun update(
        id: Long,
        request: TaskUpdateRequest,
    ) {
        val user = authorizationService.currentUser()
        val task = getTaskById(id)
        authorizationService.requireTaskOwner(user, task)
        task.update(
            epic = request.epicId?.let { getEpicById(it) },
            name = request.name,
            description = request.description,
            status = request.status,
            progress = request.progress,
            startDate = request.startDate,
            dueDate = request.dueDate,
            assignee = request.assigneeId?.let { getUserById(it) },
        )
    }

    @Transactional
    fun delete(id: Long) {
        val user = authorizationService.currentUser()
        val task = getTaskById(id)
        authorizationService.requireTaskOwner(user, task)
        taskRepository.delete(task)
    }

    private fun getTaskById(id: Long): Task =
        taskRepository.findByIdOrNull(id)
            ?: throw CustomException(WeeklyReportErrorCode.NOT_FOUND_TASK, id)

    private fun getEpicById(id: Long): Epic =
        epicRepository.findByIdOrNull(id)
            ?: throw CustomException(WeeklyReportErrorCode.NOT_FOUND_EPIC, id)

    private fun getUserById(id: Long): User =
        userRepository.findByIdOrNull(id)
            ?: throw CustomException(WeeklyReportErrorCode.NOT_FOUND_USER, id)
}
