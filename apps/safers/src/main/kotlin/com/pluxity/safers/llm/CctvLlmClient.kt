package com.pluxity.safers.llm

import com.pluxity.safers.llm.dto.CctvFilterCriteria
import com.pluxity.safers.llm.dto.Message
import com.pluxity.safers.llm.dto.SiteInfo
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Component

private val log = KotlinLogging.logger {}

@Component
class CctvLlmClient(
    private val llmClient: LlmClient,
) {
    private val objectMapper = LlmClient.objectMapper

    private val promptTemplate: String by lazy {
        ClassPathResource("prompts/cctv-filter-system.txt").getContentAsString(Charsets.UTF_8)
    }

    fun parseCctvFilter(
        query: String,
        sites: List<SiteInfo>,
    ): CctvFilterCriteria? {
        val systemPrompt =
            promptTemplate.replace(
                "{{sites}}",
                sites.joinToString("\n") { site ->
                    val addr = site.address?.let { "($it)" } ?: ""
                    val desc = site.description?.let { " - $it" } ?: ""
                    "- ${site.id}: ${site.name}$addr$desc"
                },
            )

        val messages =
            listOf(
                Message(role = "system", content = systemPrompt),
                Message(role = "user", content = query),
            )

        return try {
            val start = System.currentTimeMillis()
            val content = llmClient.call(messages)
            val elapsed = System.currentTimeMillis() - start

            content?.let { parseResponse(it) }?.also {
                log.info { "LLM CCTV 필터 파싱 완료 - ${elapsed}ms, query: $query, criteria: $it" }
            }
        } catch (e: Exception) {
            log.error(e) { "LLM CCTV 필터 파싱 실패 - query: $query" }
            null
        }
    }

    private fun parseResponse(content: String): CctvFilterCriteria? {
        val json = LlmClient.extractJson(content)

        return try {
            val node = objectMapper.readTree(json)

            CctvFilterCriteria(
                name = node.get("name")?.takeIf { !it.isNull }?.asString(),
                siteIds =
                    node
                        .get("siteIds")
                        ?.takeIf { !it.isNull && it.isArray }
                        ?.mapNotNull { it.asLong() },
            )
        } catch (e: Exception) {
            log.error(e) { "LLM CCTV 응답 JSON 파싱 실패 - content: $json" }
            null
        }
    }
}
