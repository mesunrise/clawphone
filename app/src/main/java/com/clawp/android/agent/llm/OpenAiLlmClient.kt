package com.clawp.android.agent.llm

import com.clawp.android.agent.AgentConfig
import com.clawp.android.agent.langchain.http.OkHttpClientBuilderAdapter
import dev.langchain4j.agent.tool.ToolSpecification
import dev.langchain4j.data.message.ChatMessage
import dev.langchain4j.model.chat.ChatModel
import dev.langchain4j.model.chat.StreamingChatModel
import dev.langchain4j.model.chat.request.ChatRequest
import dev.langchain4j.model.chat.response.ChatResponse
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler
import dev.langchain4j.model.openai.OpenAiChatModel
import dev.langchain4j.model.openai.OpenAiStreamingChatModel
import okhttp3.OkHttpClient
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicReference

/**
 * OpenAI 兼容的 LLM 客户端实现
 * 支持自定义 base URL，可用于 GPUGeek 等第三方服务
 */
class OpenAiLlmClient(
    private val config: AgentConfig,
    private val httpClientBuilder: OkHttpClientBuilderAdapter
) : LlmClient {

    private val chatModel: ChatModel by lazy { buildChatModel() }
    private val streamingChatModel: StreamingChatModel by lazy { buildStreamingChatModel() }

    private fun buildChatModel(): ChatModel {
        val builder = OpenAiChatModel.builder()
            .httpClientBuilder(httpClientBuilder)
            .apiKey(config.apiKey)
            .modelName(config.modelName)
            .temperature(config.temperature)
        if (config.baseUrl.isNotEmpty()) {
            builder.baseUrl(config.baseUrl)
        }
        return builder.build()
    }

    private fun buildStreamingChatModel(): StreamingChatModel {
        val builder = OpenAiStreamingChatModel.builder()
            .httpClientBuilder(httpClientBuilder)
            .apiKey(config.apiKey)
            .modelName(config.modelName)
            .temperature(config.temperature)
        if (config.baseUrl.isNotEmpty()) {
            builder.baseUrl(config.baseUrl)
        }
        return builder.build()
    }

    override fun chat(messages: List<ChatMessage>, toolSpecs: List<ToolSpecification>): LlmResponse {
        val request = ChatRequest.builder()
            .messages(messages)
            .toolSpecifications(toolSpecs)
            .build()
        val response = chatModel.chat(request)
        return response.toLlmResponse()
    }

    override fun chatStreaming(
        messages: List<ChatMessage>,
        toolSpecs: List<ToolSpecification>,
        listener: StreamingListener
    ): LlmResponse {
        val request = ChatRequest.builder()
            .messages(messages)
            .toolSpecifications(toolSpecs)
            .build()

        val latch = CountDownLatch(1)
        val resultRef = AtomicReference<LlmResponse>()
        val errorRef = AtomicReference<Throwable>()

        streamingChatModel.chat(request, object : StreamingChatResponseHandler {
            override fun onPartialResponse(token: String) {
                listener.onPartialText(token)
            }

            override fun onCompleteResponse(response: ChatResponse) {
                val llmResponse = response.toLlmResponse()
                resultRef.set(llmResponse)
                listener.onComplete(llmResponse)
                latch.countDown()
            }

            override fun onError(error: Throwable) {
                errorRef.set(error)
                listener.onError(error)
                latch.countDown()
            }
        })

        // 等待最多 70 秒，避免无限等待
        val completed = latch.await(70, java.util.concurrent.TimeUnit.SECONDS)
        if (!completed) {
            throw java.util.concurrent.TimeoutException("LLM streaming response timeout after 70 seconds")
        }

        errorRef.get()?.let { throw it }
        return resultRef.get()
    }
}
