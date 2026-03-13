package com.pluxity.weekly.task.service

import com.pluxity.common.auth.annotation.CheckPermission
import com.pluxity.common.auth.user.entity.PermissionAction
import com.pluxity.common.auth.user.entity.User
import com.pluxity.common.auth.user.repository.UserRepository
import com.pluxity.common.core.exception.CustomException
import com.pluxity.common.core.utils.findAllNotNull
import com.pluxity.weekly.chat.dto.TaskSearchFilter
import com.pluxity.weekly.epic.entity.Epic
import com.pluxity.weekly.epic.repository.EpicRepository
import com.pluxity.weekly.global.constant.WeeklyReportErrorCode
import com.pluxity.weekly.project.entity.Project
import com.pluxity.weekly.task.dto.TaskRequest
import com.pluxity.weekly.task.dto.TaskResponse
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
) {
    @CheckPermission(action = PermissionAction.READ_LIST, resourceType = "task")
    fun findAll(): List<TaskResponse> = taskRepository.findAll().map { it.toResponse() }

    @CheckPermission(action = PermissionAction.READ_LIST, resourceType = "task")
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

    @CheckPermission(action = PermissionAction.READ_SINGLE, resourceType = "task")
    fun findById(id: Long): TaskResponse = getTaskById(id).toResponse()

    @Transactional
    fun create(request: TaskRequest): Long {
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
                    assignee = request.assigneeId?.let { getUserById(it) },
                ),
            ).requiredId

        // resource_permission 직접 추가
    }

    @CheckPermission(action = PermissionAction.UPDATE, resourceType = "task")
    @Transactional
    fun update(
        id: Long,
        request: TaskRequest,
    ) {
        getTaskById(id).update(
            epic = getEpicById(request.epicId),
            name = request.name,
            description = request.description,
            status = request.status,
            progress = request.progress,
            startDate = request.startDate,
            dueDate = request.dueDate,
            assignee = request.assigneeId?.let { getUserById(it) },
        )
    }

    @CheckPermission(action = PermissionAction.DELETE, resourceType = "task")
    @Transactional
    fun delete(id: Long) {
        taskRepository.delete(getTaskById(id))
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
