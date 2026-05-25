package com.clawp.android.agent.llm

import com.clawp.android.agent.AgentConfig
import com.clawp.android.agent.langchain.http.OkHttpClientBuilderAdapter
import com.clawp.android.utils.XLog
import dev.langchain4j.data.message.ChatMessage
import dev.langchain4j.model.chat.ChatLanguageModel
import dev.langchain4j.model.chat.StreamingChatLanguageModel
import dev.langchain4j.model.openai.OpenAiChatModel
import dev.langchain4j.model.openai.OpenAiStreamingChatModel
import dev.langchain4j.model.output.Response
import dev.langchain4j.model.output.TokenUsage
import okhttp3.OkHttpClient
import java.time.Duration

/**
 * OpenAI 兼容的 LLM 客户端实现
 * 支持自定义 base URL，可用于 GPUGeek 等第三方服务
 */
class OpenAiLlmClient(
    private val config: AgentConfig,
    private val httpClient: OkHttpClient
) : LlmClient {

    companion object {
        private const val TAG = "OpenAiLlmClient"
    }

    private val httpClientBuilder = OkHttpClientBuilderAdapter(httpClient)

    override fun chat(messages: List<ChatMessage>, toolSpecs: List<dev.langchain4j.model.tool.ToolSpecification>): LlmResponse {
        val model = buildChatModel()
        val response: Response<dev.langchain4j.data.message.AiMessage> = if (toolSpecs.isEmpty()) {
            model.generate(messages)
        } else {
            model.generate(messages, toolSpecs)
        }

        val aiMessage = response.content()
        val usage = response.tokenUsage()

        return LlmResponse(
            content = aiMessage.text() ?: "",
            toolCalls = aiMessage.toolExecutionRequests()?.map { req ->
                ToolCall(
                    id = req.id(),
                    name = req.name(),
                    arguments = req.arguments()
                )
            } ?: emptyList(),
            stopReason = if (aiMessage.toolExecutionRequests()?.isNotEmpty() == true) "tool_use" else "end_turn",
            inputTokens = usage?.inputTokenCount() ?: 0,
            outputTokens = usage?.outputTokenCount() ?: 0
        )
    }

    override fun chatStreaming(
        messages: List<ChatMessage>,
        toolSpecs: List<dev.langchain4j.model.tool.ToolSpecification>,
        listener: StreamingListener
    ): LlmResponse {
        val model = buildStreamingChatModel()
        val contentBuilder = StringBuilder()
        val toolCallsBuilder = mutableListOf<ToolCall>()
        var inputTokens = 0
        var outputTokens = 0
        var stopReason = "end_turn"

        val handler = object : dev.langchain4j.model.output.StreamingResponseHandler<dev.langchain4j.data.message.AiMessage> {
            override fun onNext(token: String) {
                contentBuilder.append(token)
                listener.onContentDelta(token)
            }

            override fun onComplete(response: Response<dev.langchain4j.data.message.AiMessage>) {
                val aiMessage = response.content()
                val usage = response.tokenUsage()

                inputTokens = usage?.inputTokenCount() ?: 0
                outputTokens = usage?.outputTokenCount() ?: 0

                aiMessage.toolExecutionRequests()?.forEach { req ->
                    toolCallsBuilder.add(
                        ToolCall(
                            id = req.id(),
                            name = req.name(),
                            arguments = req.arguments()
                        )
                    )
                }

                if (toolCallsBuilder.isNotEmpty()) {
                    stopReason = "tool_use"
                }
            }

            override fun onError(error: Throwable) {
                XLog.e(TAG, "Streaming error", error)
                throw error
            }
        }

        if (toolSpecs.isEmpty()) {
            model.generate(messages, handler)
        } else {
            model.generate(messages, toolSpecs, handler)
        }

        return LlmResponse(
            content = contentBuilder.toString(),
            toolCalls = toolCallsBuilder,
            stopReason = stopReason,
            inputTokens = inputTokens,
            outputTokens = outputTokens
        )
    }

    private fun buildChatModel(): ChatLanguageModel {
        val builder = OpenAiChatModel.builder()
            .httpClientBuilder(httpClientBuilder)
            .apiKey(config.apiKey)
            .modelName(config.modelName.ifEmpty { "gpt-4" })
            .temperature(config.temperature)
            .timeout(Duration.ofSeconds(120))

        if (config.baseUrl.isNotEmpty()) {
            builder.baseUrl(config.baseUrl)
        }

        return builder.build()
    }

    private fun buildStreamingChatModel(): StreamingChatLanguageModel {
        val builder = OpenAiStreamingChatModel.builder()
            .httpClientBuilder(httpClientBuilder)
            .apiKey(config.apiKey)
            .modelName(config.modelName.ifEmpty { "gpt-4" })
            .temperature(config.temperature)
            .timeout(Duration.ofSeconds(120))

        if (config.baseUrl.isNotEmpty()) {
            builder.baseUrl(config.baseUrl)
        }

        return builder.build()
    }
}
