package com.clawp.android.agent.llm

import com.clawp.android.agent.AgentConfig
import com.clawp.android.agent.DefaultAgentService
import com.clawp.android.agent.langchain.http.OkHttpClientBuilderAdapter

object LlmClientFactory {

    fun create(config: AgentConfig): LlmClient {
        val httpClientBuilder = OkHttpClientBuilderAdapter().apply {
            if (DefaultAgentService.FILE_LOGGING_ENABLED && DefaultAgentService.FILE_LOGGING_CACHE_DIR != null) {
                setFileLoggingEnabled(true, DefaultAgentService.FILE_LOGGING_CACHE_DIR)
            }
        }
        return OpenAiLlmClient(config, httpClientBuilder)
    }
}
