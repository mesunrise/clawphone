package com.clawp.android.agent.llm

import dev.langchain4j.model.chat.response.ChatResponse

fun ChatResponse.toLlmResponse(): LlmResponse {
    val aiMessage = this.aiMessage()
    return LlmResponse(
        text = aiMessage.text(),
        toolExecutionRequests = aiMessage.toolExecutionRequests() ?: emptyList(),
        tokenUsage = this.metadata()?.tokenUsage()
    )
}
