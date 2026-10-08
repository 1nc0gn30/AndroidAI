package com.example.model

enum class HarnessProvider(
    val id: String,
    val displayName: String,
    val defaultModel: String,
    val tagline: String,
    val defaultEndpoint: String,
    val specialty: String,
    val badgeColorHex: Long
) {
    GOOGLE(
        id = "google",
        displayName = "Google Gemini",
        defaultModel = "gemini-2.5-flash",
        tagline = "Ultra-fast multimodal reasoning & native Android intelligence",
        defaultEndpoint = "https://generativelanguage.googleapis.com/v1beta/models/",
        specialty = "Multimodal & Speed",
        badgeColorHex = 0xFF4285F4
    ),
    OPENAI(
        id = "openai",
        displayName = "OpenAI",
        defaultModel = "gpt-4o",
        tagline = "Industry standard for structured outputs & tool calling",
        defaultEndpoint = "https://api.openai.com/v1/chat/completions",
        specialty = "Function Calling",
        badgeColorHex = 0xFF10A37F
    ),
    ANTHROPIC(
        id = "anthropic",
        displayName = "Anthropic Claude",
        defaultModel = "claude-3-5-sonnet-20241022",
        tagline = "Superior nuance, nuanced writing & precise code control",
        defaultEndpoint = "https://api.anthropic.com/v1/messages",
        specialty = "Nuance & Code",
        badgeColorHex = 0xFFD97706
    ),
    META(
        id = "meta",
        displayName = "Meta Llama",
        defaultModel = "llama-3.3-70b-instruct",
        tagline = "Leading open-weights foundation model for customizable workflows",
        defaultEndpoint = "https://api.groq.com/openai/v1/chat/completions",
        specialty = "Open Weights",
        badgeColorHex = 0xFF0866FF
    ),
    HERMES(
        id = "hermes",
        displayName = "Nous Hermes Agent",
        defaultModel = "hermes-3-llama-3.1-405b",
        tagline = "Pioneering agentic steering & multi-turn tool chain synthesis",
        defaultEndpoint = "https://api.together.xyz/v1/chat/completions",
        specialty = "Agentic Workflows",
        badgeColorHex = 0xFF8B5CF6
    ),
    GROK(
        id = "grok",
        displayName = "xAI Grok",
        defaultModel = "grok-2",
        tagline = "Unfiltered direct reasoning & real-time knowledge synthesis",
        defaultEndpoint = "https://api.x.ai/v1/chat/completions",
        specialty = "Live Knowledge",
        badgeColorHex = 0xFF111827
    ),
    MISTRAL(
        id = "mistral",
        displayName = "Mistral AI",
        defaultModel = "mistral-large-latest",
        tagline = "High-efficiency reasoning & compact latency across tasks",
        defaultEndpoint = "https://api.mistral.ai/v1/chat/completions",
        specialty = "Compact Efficiency",
        badgeColorHex = 0xFFFF7000
    ),
    COHERE(
        id = "cohere",
        displayName = "Cohere",
        defaultModel = "command-r-plus-08-2024",
        tagline = "Optimized for enterprise RAG, search grounding & multi-step actions",
        defaultEndpoint = "https://api.cohere.com/v2/chat",
        specialty = "RAG & Actions",
        badgeColorHex = 0xFF39594C
    ),
    DEEPSEEK(
        id = "deepseek",
        displayName = "DeepSeek",
        defaultModel = "deepseek-reasoner",
        tagline = "State-of-the-art chain-of-thought mathematical & logical deduction",
        defaultEndpoint = "https://api.deepseek.com/chat/completions",
        specialty = "Deep Reasoning (R1)",
        badgeColorHex = 0xFF0284C7
    ),
    PERPLEXITY(
        id = "perplexity",
        displayName = "Perplexity",
        defaultModel = "sonar-pro",
        tagline = "Live web citation, real-time search engine synthesis & fact verification",
        defaultEndpoint = "https://api.perplexity.ai/chat/completions",
        specialty = "Live Web Grounding",
        badgeColorHex = 0xFF0D9488
    )
}

data class HarnessConfig(
    val provider: HarnessProvider,
    val apiKey: String = "",
    val model: String = provider.defaultModel,
    val customEndpoint: String = "",
    val temperature: Float = 0.7f,
    val isEnabled: Boolean = true,
    val requestCount: Int = 0,
    val lastLatencyMs: Long = 0,
    val lastSuccess: Boolean = true
)
