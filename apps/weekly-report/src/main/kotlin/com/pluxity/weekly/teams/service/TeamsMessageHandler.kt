package com.pluxity.weekly.teams.service

import com.pluxity.common.core.exception.CustomException
import com.pluxity.weekly.chat.service.ChatService
import com.pluxity.weekly.epic.dto.EpicRequest
import com.pluxity.weekly.epic.service.EpicService
import com.pluxity.weekly.global.auth.AuthorizationService
import com.pluxity.weekly.project.dto.ProjectRequest
import com.pluxity.weekly.project.service.ProjectService
import com.pluxity.weekly.teams.converter.AdaptiveCardConverter
import com.pluxity.weekly.teams.dto.Activity
import com.pluxity.weekly.teams.entity.TeamsConversation
import com.pluxity.weekly.teams.repository.TeamsConversationRepository
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service
import java.time.LocalDate

private val log = KotlinLogging.logger {}

@Service
class TeamsMessageHandler(
    private val chatService: ChatService,
    private val cardConverter: AdaptiveCardConverter,
    private val replyClient: TeamsBotReplyClient,
    private val projectService: ProjectService,
    private val epicService: EpicService,
    private val authorizationService: AuthorizationService,
    private val teamsConversationRepository: TeamsConversationRepository,
) {
    fun handleActivity(activity: Activity) {
        when (activity.type) {
            "message" -> handleMessage(activity)
            "installationUpdate" -> handleInstallationUpdate(activity)
            else -> log.debug { "Unhandled activity type: ${activity.type}" }
        }
    }

    private fun handleMessage(activity: Activity) {
        log.info { "Teams 메시지 수신 - from: ${activity.from?.name}, activity: $activity" }

        if (activity.value != null) {
            handleFormSubmit(activity)
            return
        }

        val text = activity.text?.trim()
        if (text.isNullOrBlank()) {
            replyClient.reply(activity, cardConverter.textMessage("메시지를 입력해주세요."))
            return
        }

        val response =
            try {
                val responses = chatService.chat(text)
                if (responses.isEmpty()) {
                    cardConverter.textMessage("처리할 수 있는 내용이 없습니다.")
                } else {
                    cardConverter.toTeamsResponse(responses.first())
                }
            } catch (e: CustomException) {
                log.warn { "Chat 처리 실패: ${e.message}" }
                cardConverter.textMessage(e.message ?: "요청을 처리할 수 없습니다.")
            } catch (e: Exception) {
                log.error(e) { "Chat 처리 중 예외 발생" }
                cardConverter.textMessage("처리 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요.")
            }

        replyClient.reply(activity, response)
    }

    @Suppress("UNCHECKED_CAST")
    private fun handleFormSubmit(activity: Activity) {
        val formData = activity.value as? Map<String, Any>
        if (formData == null) {
            replyClient.reply(activity, cardConverter.textMessage("폼 데이터를 읽을 수 없습니다."))
            return
        }

        val action = formData["action"] as? String
        val target = formData["target"] as? String
        log.info { "폼 submit 수신 - action: $action, target: $target, data: $formData" }

        val response =
            try {
                when {
                    action == "create" && target == "project" -> {
                        val id =
                            projectService.create(
                                ProjectRequest(
                                    name = formData["name"] as? String ?: "",
                                    description = formData["description"] as? String,
                                    startDate = (formData["startDate"] as? String)?.takeIf { it.isNotBlank() }?.let { LocalDate.parse(it) },
                                    dueDate = (formData["dueDate"] as? String)?.takeIf { it.isNotBlank() }?.let { LocalDate.parse(it) },
                                    pmId = (formData["pmId"] as? String)?.toLongOrNull(),
                                ),
                            )
                        cardConverter.textMessage("프로젝트 생성이 완료되었습니다. (ID: $id)")
                    }
                    action == "create" && target == "epic" -> {
                        val projectId = (formData["projectId"] as? String)?.toLongOrNull()
                        if (projectId == null) {
                            cardConverter.textMessage("프로젝트를 선택해주세요.")
                        } else {
                            val userIds =
                                (formData["userIds"] as? String)
                                    ?.split(",")
                                    ?.mapNotNull { it.trim().toLongOrNull() }
                                    ?.ifEmpty { null }
                            val id =
                                epicService.create(
                                    EpicRequest(
                                        projectId = projectId,
                                        name = formData["name"] as? String ?: "",
                                        description = formData["description"] as? String,
                                        startDate =
                                            (formData["startDate"] as? String)
                                                ?.takeIf { it.isNotBlank() }
                                                ?.let { LocalDate.parse(it) },
                                        dueDate =
                                            (formData["dueDate"] as? String)
                                                ?.takeIf { it.isNotBlank() }
                                                ?.let { LocalDate.parse(it) },
                                        userIds = userIds,
                                    ),
                                )
                            cardConverter.textMessage("에픽 생성이 완료되었습니다. (ID: $id)")
                        }
                    }
                    else -> cardConverter.textMessage("지원하지 않는 폼 요청입니다. (action: $action, target: $target)")
                }
            } catch (e: CustomException) {
                log.warn { "폼 처리 실패: ${e.message}" }
                cardConverter.textMessage(e.message ?: "요청을 처리할 수 없습니다.")
            } catch (e: Exception) {
                log.error(e) { "폼 처리 중 예외 발생" }
                cardConverter.textMessage("처리 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요.")
            }

        replyClient.reply(activity, response)
    }

    /**
     *  Teams App 설치시 받는 메시지
     */
    private fun handleInstallationUpdate(activity: Activity) {
        val action = activity.action ?: "unknown"
        log.info { "Installation update - action: $action, user: ${activity.from?.name}" }

        val serviceUrl = activity.serviceUrl
        val conversationId = activity.conversation?.id
        val aadObjectId = activity.from?.aadObjectId
        if (serviceUrl.isNullOrBlank() || conversationId.isNullOrBlank()) {
            log.warn { "serviceUrl 또는 conversationId 누락 - conversationReference 저장 불가" }
        } else {
            val currentUser = authorizationService.currentUser()
            val existing = teamsConversationRepository.findByUserId(currentUser.requiredId)
            if (existing != null) {
                existing.update(serviceUrl, conversationId)
            } else if (!aadObjectId.isNullOrBlank()) {
                teamsConversationRepository.save(
                    TeamsConversation(
                        aadObjectId = aadObjectId,
                        userId = currentUser.requiredId,
                        serviceUrl = serviceUrl,
                        conversationId = conversationId,
                    ),
                )
            } else {
                log.warn { "aadObjectId 누락 - TeamsConversation 저장 불가" }
            }
        }

        if (action == "add") {
            replyClient.reply(
                activity,
                cardConverter.textMessage("안녕하세요! Weekly Report 봇입니다. 자연어로 태스크를 관리해보세요."),
            )
        }
    }
}
