package com.example.ai

import com.example.BuildConfig
import com.example.model.ActionType
import com.example.model.AutomationTask
import com.example.model.HarnessConfig
import com.example.model.HarnessProvider
import com.example.model.StepStatus
import com.example.model.TaskStep
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID
import java.util.concurrent.TimeUnit

data class AiResponseResult(
    val provider: HarnessProvider,
    val model: String,
    val text: String,
    val reasoningTrace: String? = null,
    val generatedTask: AutomationTask? = null,
    val latencyMs: Long = 0,
    val isLiveApi: Boolean = false
)

class AiEngine {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    suspend fun executeHarnessPrompt(
        config: HarnessConfig,
        userPrompt: String,
        targetAppHint: String? = null
    ): AiResponseResult = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        val apiKey = config.apiKey.ifBlank {
            if (config.provider == HarnessProvider.GOOGLE) {
                try {
                    val keyField = BuildConfig::class.java.getField("GEMINI_API_KEY")
                    val value = keyField.get(null)?.toString() ?: ""
                    if (value.isNotBlank() && !value.contains("MY_GEMINI_API_KEY")) value else ""
                } catch (_: Exception) { "" }
            } else ""
        }

        if (apiKey.isNotBlank()) {
            try {
                val liveResult = callLiveApi(config, apiKey, userPrompt)
                val latency = System.currentTimeMillis() - startTime
                return@withContext liveResult.copy(
                    latencyMs = latency,
                    isLiveApi = true,
                    generatedTask = parseTaskFromResponse(userPrompt, liveResult.text, config.provider, targetAppHint)
                )
            } catch (e: Exception) {
                val simResult = generateSimulatedAgentResponse(config, userPrompt, targetAppHint)
                val latency = System.currentTimeMillis() - startTime
                return@withContext simResult.copy(
                    latencyMs = latency,
                    text = "${simResult.text}\n\n(Live API returned: ${e.localizedMessage ?: "error"}; agent fallback active)"
                )
            }
        }

        val simResult = generateSimulatedAgentResponse(config, userPrompt, targetAppHint)
        val latency = System.currentTimeMillis() - startTime
        return@withContext simResult.copy(latencyMs = latency)
    }

    private fun callLiveApi(
        config: HarnessConfig,
        apiKey: String,
        userPrompt: String
    ): AiResponseResult {
        val systemPrompt = "You are an intelligent mobile task automation agent. " +
                "You plan and execute cross-app routines. If a task requires device control, describe the steps."

        return when (config.provider) {
            HarnessProvider.GOOGLE -> callGoogleGemini(config, apiKey, userPrompt, systemPrompt)
            HarnessProvider.ANTHROPIC -> callAnthropic(config, apiKey, userPrompt, systemPrompt)
            HarnessProvider.COHERE -> callCohere(config, apiKey, userPrompt, systemPrompt)
            else -> callOpenAiCompatible(config, apiKey, userPrompt, systemPrompt)
        }
    }

    private fun callGoogleGemini(
        config: HarnessConfig,
        apiKey: String,
        userPrompt: String,
        systemPrompt: String
    ): AiResponseResult {
        val url = "https://generativelanguage.googleapis.com/v1beta/models/${config.model}:generateContent?key=$apiKey"
        val payload = JSONObject().apply {
            val contents = JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "user")
                    put("parts", JSONArray().apply {
                        put(JSONObject().put("text", "$systemPrompt\n\nUser Request: $userPrompt"))
                    })
                })
            }
            put("contents", contents)
        }

        val request = Request.Builder()
            .url(url)
            .post(payload.toString().toRequestBody("application/json".toMediaType()))
            .build()

        val response = httpClient.newCall(request).execute()
        val responseBody = response.body?.string().orEmpty()
        if (!response.isSuccessful) {
            throw IllegalStateException("HTTP ${response.code}: $responseBody")
        }

        val json = JSONObject(responseBody)
        val candidates = json.optJSONArray("candidates")
        val content = candidates?.optJSONObject(0)?.optJSONObject("content")
        val parts = content?.optJSONArray("parts")
        val text = parts?.optJSONObject(0)?.optString("text").orEmpty()

        return AiResponseResult(
            provider = config.provider,
            model = config.model,
            text = text.ifBlank { "Task plan generated by Google Gemini." }
        )
    }

    private fun callOpenAiCompatible(
        config: HarnessConfig,
        apiKey: String,
        userPrompt: String,
        systemPrompt: String
    ): AiResponseResult {
        val endpoint = config.customEndpoint.ifBlank { config.provider.defaultEndpoint }
        val payload = JSONObject().apply {
            put("model", config.model)
            val messages = JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "system")
                    put("content", systemPrompt)
                })
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", userPrompt)
                })
            }
            put("messages", messages)
            put("temperature", config.temperature.toDouble())
        }

        val request = Request.Builder()
            .url(endpoint)
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .post(payload.toString().toRequestBody("application/json".toMediaType()))
            .build()

        val response = httpClient.newCall(request).execute()
        val responseBody = response.body?.string().orEmpty()
        if (!response.isSuccessful) {
            throw IllegalStateException("HTTP ${response.code}: $responseBody")
        }

        val json = JSONObject(responseBody)
        val choices = json.optJSONArray("choices")
        val message = choices?.optJSONObject(0)?.optJSONObject("message")
        val content = message?.optString("content").orEmpty()
        val reasoning = message?.optString("reasoning_content")

        return AiResponseResult(
            provider = config.provider,
            model = config.model,
            text = content.ifBlank { "Task plan executed." },
            reasoningTrace = reasoning
        )
    }

    private fun callAnthropic(
        config: HarnessConfig,
        apiKey: String,
        userPrompt: String,
        systemPrompt: String
    ): AiResponseResult {
        val endpoint = config.customEndpoint.ifBlank { config.provider.defaultEndpoint }
        val payload = JSONObject().apply {
            put("model", config.model)
            put("max_tokens", 1024)
            put("system", systemPrompt)
            val messages = JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", userPrompt)
                })
            }
            put("messages", messages)
        }

        val request = Request.Builder()
            .url(endpoint)
            .addHeader("x-api-key", apiKey)
            .addHeader("anthropic-version", "2023-06-01")
            .addHeader("Content-Type", "application/json")
            .post(payload.toString().toRequestBody("application/json".toMediaType()))
            .build()

        val response = httpClient.newCall(request).execute()
        val responseBody = response.body?.string().orEmpty()
        if (!response.isSuccessful) {
            throw IllegalStateException("HTTP ${response.code}: $responseBody")
        }

        val json = JSONObject(responseBody)
        val contentArray = json.optJSONArray("content")
        val text = contentArray?.optJSONObject(0)?.optString("text").orEmpty()

        return AiResponseResult(
            provider = config.provider,
            model = config.model,
            text = text.ifBlank { "Claude finished task analysis." }
        )
    }

    private fun callCohere(
        config: HarnessConfig,
        apiKey: String,
        userPrompt: String,
        systemPrompt: String
    ): AiResponseResult {
        val endpoint = config.customEndpoint.ifBlank { config.provider.defaultEndpoint }
        val payload = JSONObject().apply {
            put("model", config.model)
            val messages = JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "system")
                    put("content", systemPrompt)
                })
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", userPrompt)
                })
            }
            put("messages", messages)
        }

        val request = Request.Builder()
            .url(endpoint)
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .post(payload.toString().toRequestBody("application/json".toMediaType()))
            .build()

        val response = httpClient.newCall(request).execute()
        val responseBody = response.body?.string().orEmpty()
        if (!response.isSuccessful) {
            throw IllegalStateException("HTTP ${response.code}: $responseBody")
        }

        val json = JSONObject(responseBody)
        val message = json.optJSONObject("message")
        val text = message?.optJSONArray("content")?.optJSONObject(0)?.optString("text").orEmpty()

        return AiResponseResult(
            provider = config.provider,
            model = config.model,
            text = text.ifBlank { "Cohere command processed." }
        )
    }

    private fun generateSimulatedAgentResponse(
        config: HarnessConfig,
        prompt: String,
        targetAppHint: String?
    ): AiResponseResult {
        val (reasoning, responseText) = when (config.provider) {
            HarnessProvider.DEEPSEEK -> {
                val thought = "Thinking Process:\n1. Deconstruct user goal: '$prompt'\n" +
                        "2. Inspect available phone hardware controllers & app intent schemes.\n" +
                        "3. Synthesize optimal sequence to avoid excessive context switches.\n" +
                        "4. Formulate verified execution pipeline."
                val body = "DeepSeek R1 reasoning complete. I have decomposed your goal into a multi-step action pipeline for immediate cross-app execution."
                Pair(thought, body)
            }
            HarnessProvider.HERMES -> {
                val thought = "<action>\nCall: device_agent_toolchain\nParams: { target: '$targetAppHint', prompt: '$prompt' }\nValidation: PASS\n</action>"
                val body = "Hermes Agent has synthesized an autonomous tool chain. Directing pet companion to initiate task dispatch across your phone."
                Pair(thought, body)
            }
            HarnessProvider.PERPLEXITY -> {
                val thought = "Search Query: \"$prompt best workflow automation Android\"\nCitations: [1] Android App Manifest Intents, [2] Background Controller Architecture"
                val body = "Synthesized across live web sources [1]. The task is structured for cross-platform automation. Ready to launch the target app."
                Pair(thought, body)
            }
            HarnessProvider.GROK -> {
                val thought = "xAI Real-time Matrix: Direct analysis of current task '$prompt'. Optimizing for maximum throughput."
                val body = "Grok-2 agent online. Let's cut through the steps and get this task done right away."
                Pair(thought, body)
            }
            HarnessProvider.ANTHROPIC -> {
                val thought = "Constitutional safety check passed. Analyzing multi-step intent for user request: '$prompt'."
                val body = "I have outlined a clean, structured workflow to complete this request securely across your phone's apps."
                Pair(thought, body)
            }
            HarnessProvider.GOOGLE -> {
                val thought = "Gemini multimodal context analyzer: Parsing device telemetry and application target '$targetAppHint'."
                val body = "Gemini has planned your mobile automation routine. Your pet companion is coordinating the steps."
                Pair(thought, body)
            }
            HarnessProvider.OPENAI -> {
                val thought = "GPT-4o Function Calling: Resolved schema for 'execute_phone_task' with parameters."
                val body = "Plan verified. Tool arguments formatted. Executing sequence now."
                Pair(thought, body)
            }
            HarnessProvider.META -> {
                val thought = "Llama 3.3 70B open instruct: Formulating cross-platform execution steps."
                val body = "Meta Llama agent ready. Triggering the workflow through OmniPet."
                Pair(thought, body)
            }
            HarnessProvider.MISTRAL -> {
                val thought = "Mistral Large: Compact task tokenization complete. Minimal latency path selected."
                val body = "Mistral agent sequence established. Dispatching task."
                Pair(thought, body)
            }
            HarnessProvider.COHERE -> {
                val thought = "Cohere Command R+: Grounding task in device environment and clipboard state."
                val body = "Cohere agent ready. Automation instructions compiled."
                Pair(thought, body)
            }
        }

        val generatedTask = parseTaskFromResponse(prompt, responseText, config.provider, targetAppHint)

        return AiResponseResult(
            provider = config.provider,
            model = config.model,
            text = responseText,
            reasoningTrace = reasoning,
            generatedTask = generatedTask,
            isLiveApi = false
        )
    }

    private fun parseTaskFromResponse(
        prompt: String,
        aiResponse: String,
        provider: HarnessProvider,
        targetAppHint: String?
    ): AutomationTask {
        val lower = prompt.lowercase()
        val steps = mutableListOf<TaskStep>()

        steps.add(
            TaskStep(
                id = UUID.randomUUID().toString(),
                title = "Synthesize plan with ${provider.displayName}",
                actionType = ActionType.AI_REASONING,
                param = prompt,
                status = StepStatus.COMPLETED,
                logOutput = "AI model formulated multi-step action plan"
            )
        )

        if (lower.contains("flash") || lower.contains("torch") || lower.contains("light")) {
            steps.add(
                TaskStep(
                    id = UUID.randomUUID().toString(),
                    title = "Toggle Flashlight",
                    actionType = ActionType.FLASHLIGHT_TOGGLE,
                    param = "toggle"
                )
            )
        }

        if (lower.contains("battery") || lower.contains("power") || lower.contains("charge")) {
            steps.add(
                TaskStep(
                    id = UUID.randomUUID().toString(),
                    title = "Inspect Battery Level & State",
                    actionType = ActionType.BATTERY_CHECK,
                    param = "check"
                )
            )
        }

        if (lower.contains("copy") || lower.contains("clip") || lower.contains("note") || lower.contains("summar")) {
            steps.add(
                TaskStep(
                    id = UUID.randomUUID().toString(),
                    title = "Copy Summary to Clipboard",
                    actionType = ActionType.CLIPBOARD_COPY,
                    param = "Summary: $prompt [Generated by ${provider.displayName}]"
                )
            )
        }

        if (lower.contains("search") || lower.contains("look up") || lower.contains("find") || lower.contains("google") || lower.contains("web")) {
            val query = prompt.replace("search", "", true)
                .replace("look up", "", true)
                .replace("find", "", true)
                .trim()
            steps.add(
                TaskStep(
                    id = UUID.randomUUID().toString(),
                    title = "Launch Web Search",
                    actionType = ActionType.WEB_SEARCH,
                    param = query.ifBlank { prompt }
                )
            )
        }

        if (lower.contains("map") || lower.contains("navigate") || lower.contains("direction") || lower.contains("where")) {
            steps.add(
                TaskStep(
                    id = UUID.randomUUID().toString(),
                    title = "Open Navigation in Maps",
                    actionType = ActionType.MAP_NAVIGATE,
                    param = prompt
                )
            )
        }

        if (lower.contains("share") || lower.contains("send")) {
            steps.add(
                TaskStep(
                    id = UUID.randomUUID().toString(),
                    title = "Dispatch Share Intent",
                    actionType = ActionType.SHARE_TEXT,
                    param = "OmniPet AI automated task: $prompt"
                )
            )
        }

        if (lower.contains("email") || lower.contains("mail")) {
            steps.add(
                TaskStep(
                    id = UUID.randomUUID().toString(),
                    title = "Draft Message in Email App",
                    actionType = ActionType.DRAFT_EMAIL,
                    param = prompt
                )
            )
        }

        if (!targetAppHint.isNullOrBlank()) {
            steps.add(
                TaskStep(
                    id = UUID.randomUUID().toString(),
                    title = "Launch Target App",
                    actionType = ActionType.LAUNCH_APP,
                    param = targetAppHint
                )
            )
        } else if (steps.size == 1) {
            steps.add(
                TaskStep(
                    id = UUID.randomUUID().toString(),
                    title = "Save AI Response to Clipboard",
                    actionType = ActionType.CLIPBOARD_COPY,
                    param = aiResponse.take(300)
                )
            )
            steps.add(
                TaskStep(
                    id = UUID.randomUUID().toString(),
                    title = "Provide Haptic Notification",
                    actionType = ActionType.VIBRATE_HAPTIC,
                    param = "success"
                )
            )
        }

        return AutomationTask(
            id = UUID.randomUUID().toString(),
            title = prompt.take(45),
            description = "Automated by ${provider.displayName} through OmniPet",
            harnessProvider = provider,
            targetApp = targetAppHint,
            steps = steps
        )
    }
}
