package com.pluxity.weekly.dashboard.service

import com.pluxity.common.auth.user.repository.UserRepository
import com.pluxity.common.core.exception.CustomException
import com.pluxity.weekly.dashboard.dto.AdminDashboardResponse
import com.pluxity.weekly.dashboard.dto.AdminProjectCard
import com.pluxity.weekly.dashboard.dto.EpicTaskGroup
import com.pluxity.weekly.dashboard.dto.EpicTaskRow
import com.pluxity.weekly.dashboard.dto.PmDashboardResponse
import com.pluxity.weekly.dashboard.dto.PmProjectSummary
import com.pluxity.weekly.dashboard.dto.RoadmapItem
import com.pluxity.weekly.dashboard.dto.RoadmapTaskBar
import com.pluxity.weekly.dashboard.dto.TeamSummaryItem
import com.pluxity.weekly.dashboard.dto.WorkerDashboardResponse
import com.pluxity.weekly.dashboard.dto.WorkerEpicItem
import com.pluxity.weekly.dashboard.dto.WorkerSummary
import com.pluxity.weekly.dashboard.dto.WorkerTaskItem
import com.pluxity.weekly.epic.repository.EpicRepository
import com.pluxity.weekly.global.auth.AuthorizationService
import com.pluxity.weekly.global.constant.WeeklyReportErrorCode
import com.pluxity.weekly.project.repository.ProjectRepository
import com.pluxity.weekly.task.entity.Task
import com.pluxity.weekly.task.entity.TaskStatus
import com.pluxity.weekly.task.repository.TaskRepository
import com.pluxity.weekly.team.repository.TeamMemberRepository
import com.pluxity.weekly.team.repository.TeamRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.temporal.ChronoUnit

@Service
@Transactional(readOnly = true)
class DashboardService(
    private val authorizationService: AuthorizationService,
    private val projectRepository: ProjectRepository,
    private val epicRepository: EpicRepository,
    private val taskRepository: TaskRepository,
    private val userRepository: UserRepository,
    private val teamRepository: TeamRepository,
    private val teamMemberRepository: TeamMemberRepository,
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

    fun getPmDashboard(projectId: Long): PmDashboardResponse {
        val user = authorizationService.currentUser()
        authorizationService.requireProjectManager(user, projectId)

        val project =
            projectRepository.findByIdOrNull(projectId)
                ?: throw CustomException(WeeklyReportErrorCode.NOT_FOUND_PROJECT, projectId)

        val pmName =
            project.pmId?.let { userRepository.findByIdOrNull(it)?.name } ?: ""

        val epics = epicRepository.findByProjectId(projectId)
        val tasks = taskRepository.findByEpicIn(epics)
        val tasksByEpicId = tasks.groupBy { it.epic.requiredId }
        val now = LocalDate.now()

        val memberCount = tasks.mapNotNull { it.assignee?.requiredId }.distinct().size

        return PmDashboardResponse(
            project =
                PmProjectSummary(
                    projectId = project.requiredId,
                    projectName = project.name,
                    pmName = pmName,
                    status = project.status,
                    progress = if (tasks.isEmpty()) 0 else tasks.map { it.progress }.average().toInt(),
                    startDate = project.startDate,
                    dueDate = project.dueDate,
                    epicCount = epics.size,
                    taskCount = tasks.size,
                    memberCount = memberCount,
                ),
            roadmapItems =
                epics.map { epic ->
                    val epicTasks = tasksByEpicId[epic.requiredId] ?: emptyList()
                    RoadmapItem(
                        epicId = epic.requiredId,
                        epicName = epic.name,
                        startDate = epic.startDate,
                        dueDate = epic.dueDate,
                        status = epic.status,
                        progress = if (epicTasks.isEmpty()) 0 else epicTasks.map { it.progress }.average().toInt(),
                        tasks = epicTasks.map { it.toRoadmapTaskBar(now) },
                    )
                },
            epicTaskGroups =
                epics.map { epic ->
                    val epicTasks = tasksByEpicId[epic.requiredId] ?: emptyList()
                    EpicTaskGroup(
                        epicId = epic.requiredId,
                        epicName = epic.name,
                        status = epic.status,
                        tasks = epicTasks.map { it.toEpicTaskRow(now) },
                    )
                },
        )
    }

    fun getAdminDashboard(): AdminDashboardResponse {
        val user = authorizationService.currentUser()
        authorizationService.requireAdmin(user)

        val now = LocalDate.now()

        // 프로젝트 카드
        val projects = projectRepository.findAll()
        val allEpics = epicRepository.findByProjectIdIn(projects.map { it.requiredId })
        val allTasks = taskRepository.findByEpicIn(allEpics)
        val epicsByProjectId = allEpics.groupBy { it.project.requiredId }
        val tasksByEpicId = allTasks.groupBy { it.epic.requiredId }

        val pmIds = projects.mapNotNull { it.pmId }.distinct()
        val pmNameById =
            if (pmIds.isEmpty()) {
                emptyMap()
            } else {
                userRepository.findAllById(pmIds).associate { it.requiredId to it.name }
            }

        val projectCards =
            projects.map { project ->
                val projectEpics = epicsByProjectId[project.requiredId] ?: emptyList()
                val projectTasks = projectEpics.flatMap { tasksByEpicId[it.requiredId] ?: emptyList() }
                AdminProjectCard(
                    projectId = project.requiredId,
                    projectName = project.name,
                    pmName = project.pmId?.let { pmNameById[it] },
                    status = project.status,
                    progress = if (projectTasks.isEmpty()) 0 else projectTasks.map { it.progress }.average().toInt(),
                    epicCount = projectEpics.size,
                    memberCount = projectTasks.mapNotNull { it.assignee?.requiredId }.distinct().size,
                    delayedTaskCount =
                        projectTasks.count { task ->
                            task.dueDate != null &&
                                task.status != TaskStatus.DONE &&
                                now.isAfter(task.dueDate)
                        },
                    startDate = project.startDate,
                    dueDate = project.dueDate,
                )
            }

        // 팀 요약
        val teams = teamRepository.findAll()
        val tasksByAssigneeId = allTasks.groupBy { it.assignee?.requiredId }

        val teamSummaries =
            teams.map { team ->
                val members = teamMemberRepository.findByTeam(team)
                val memberUserIds = members.map { it.user.requiredId }.toSet()
                val memberTasks = memberUserIds.flatMap { tasksByAssigneeId[it] ?: emptyList() }
                val doneCount = memberTasks.count { it.status == TaskStatus.DONE }

                TeamSummaryItem(
                    teamId = team.requiredId,
                    teamName = team.name,
                    leaderName = team.leaderId?.let { pmNameById[it] ?: userRepository.findByIdOrNull(it)?.name },
                    memberCount = members.size,
                    activeTaskCount = memberTasks.count { it.status != TaskStatus.DONE },
                    completionRate = if (memberTasks.isEmpty()) 0 else (doneCount * 100 / memberTasks.size),
                )
            }

        return AdminDashboardResponse(
            projects = projectCards,
            teamSummaries = teamSummaries,
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

    private fun Task.toRoadmapTaskBar(now: LocalDate): RoadmapTaskBar =
        RoadmapTaskBar(
            taskId = this.requiredId,
            taskName = this.name,
            assigneeName = this.assignee?.name,
            startDate = this.startDate,
            dueDate = this.dueDate,
            status = this.status,
            progress = this.progress,
            daysDelta = calculateDaysDelta(now),
        )

    private fun Task.toEpicTaskRow(now: LocalDate): EpicTaskRow =
        EpicTaskRow(
            taskId = this.requiredId,
            taskName = this.name,
            status = this.status,
            progress = this.progress,
            assigneeName = this.assignee?.name,
            startDate = this.startDate,
            dueDate = this.dueDate,
            daysDelta = calculateDaysDelta(now),
        )

    /**
     * daysDelta 계산:
     * - DONE: updatedAt(완료일) - dueDate (음수=조기완료, 양수=지연완료)
     * - 미완료 + 마감초과: now - dueDate (양수=지연중)
     * - 그 외: null
     */
    private fun Task.calculateDaysDelta(now: LocalDate): Int? {
        val due = this.dueDate ?: return null
        return when {
            this.status == TaskStatus.DONE ->
                ChronoUnit.DAYS.between(due, this.updatedAt.toLocalDate()).toInt()
            now.isAfter(due) ->
                ChronoUnit.DAYS.between(due, now).toInt()
            else -> null
        }
    }
}
