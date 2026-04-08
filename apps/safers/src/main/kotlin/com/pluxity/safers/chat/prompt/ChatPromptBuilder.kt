package com.pluxity.safers.chat.prompt

import com.pluxity.safers.cctv.dto.CctvResponse
import com.pluxity.safers.chat.dto.ScreenMeta
import com.pluxity.safers.llm.dto.Message
import com.pluxity.safers.site.entity.Site
import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
class ChatPromptBuilder {
    private val intentPrompt: String by lazy {
        ClassPathResource("prompts/chat-intent-system.txt").getContentAsString(Charsets.UTF_8)
    }

    private val layoutPrompt: String by lazy {
        ClassPathResource("prompts/chat-system.txt").getContentAsString(Charsets.UTF_8)
    }

    /**
     * 1차 호출용: 의도 파악 시스템 프롬프트 (히스토리 + 캐시 메타데이터 포함)
     */
    fun buildIntentPrompt(
        sites: List<Site>,
        history: List<Message>,
        screenMetaList: List<ScreenMeta> = emptyList(),
    ): String =
        buildString {
            val now = LocalDateTime.now()

            append(intentPrompt.replace("{{now}}", now.toString()))
            appendLine()
            appendLine()
            appendLine("## SITES (${sites.size}개 현장)")
            sites.forEach { site ->
                appendLine("- id=${site.requiredId}, ${site.name}")
            }

            // 캐시된 화면 목록 (recall/modify에서 ref 매칭용)
            if (screenMetaList.isNotEmpty()) {
                appendLine()
                appendLine("## 캐시된 화면 목록 (recall/modify 시 ref로 참조)")
                screenMetaList.forEach { meta ->
                    val siteIdsCsv = meta.siteIds.joinToString(",")
                    val targetsCsv = meta.targets.joinToString(",")
                    val actionIdsCsv = meta.actionIds.joinToString(",")
                    appendLine(
                        "- ref=${meta.ref}, summary=${meta.summary}, siteIds=[$siteIdsCsv], targets=[$targetsCsv], actionIds=[$actionIdsCsv]",
                    )
                }
            }

            // 히스토리를 시스템 프롬프트 내 별도 블록으로 포함
            val historyEntries = history.filter { it.role == "system" && it.content.startsWith("---") }
            if (historyEntries.isNotEmpty()) {
                appendLine()
                appendLine("## 대화 히스토리 (참조용, 절대 이 형식으로 응답하지 마세요)")
                historyEntries.forEach { appendLine(it.content) }
            }
        }

    /**
     * 2차 호출용: UI 배치 시스템 프롬프트
     */
    fun buildLayoutPrompt(
        sites: List<Site>,
        cctvs: List<CctvResponse>,
    ): String =
        buildString {
            append(layoutPrompt)
            appendLine()
            appendLine()
            appendLine("## SITES")
            sites.forEach { site ->
                appendLine("- id=${site.requiredId}, ${site.name}")
            }
            appendLine()
            appendCameras(cctvs)
        }

    /**
     * 2차 호출용: 조회된 데이터 요약 메시지
     */
    fun buildDataSummary(
        userQuery: String,
        dataModel: Map<String, Any>,
    ): String =
        buildString {
            appendLine("사용자 질문: $userQuery")
            appendLine()
            appendLine("조회된 데이터:")
            dataModel.forEach { (key, value) ->
                when (value) {
                    is Collection<*> -> appendLine("- $key: ${value.size}건")
                    is Map<*, *> -> {
                        val content = value["content"]
                        if (content is Collection<*>) {
                            appendLine("- $key: ${content.size}건 (총 ${value["totalElements"] ?: "?"}건)")
                        } else {
                            appendLine("- $key: 데이터 있음")
                        }
                    }
                    else -> appendLine("- $key: 데이터 있음")
                }
            }
            appendLine()
            appendLine("위 데이터를 기반으로 적절한 UI를 배치해주세요.")
        }

    private fun StringBuilder.appendCameras(cctvs: List<CctvResponse>) {
        if (cctvs.isEmpty()) return
        appendLine("## AVAILABLE_CAMERAS")
        cctvs.groupBy { it.site.name }.forEach { (siteName, cameras) ->
            appendLine("$siteName:")
            cameras.forEach { appendLine("  - ${it.name}") }
        }
    }
}
