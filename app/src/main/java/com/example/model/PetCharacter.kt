package com.example.model

enum class PetSpecies(
    val id: String,
    val petName: String,
    val title: String,
    val description: String,
    val primaryColorHex: Long,
    val accentColorHex: Long,
    val iconEmoji: String
) {
    CYBER_CAT(
        id = "cyber_cat",
        petName = "Kiko",
        title = "Cybernetic Feline",
        description = "Playful and lightning-fast at cross-app multitasking.",
        primaryColorHex = 0xFF6366F1,
        accentColorHex = 0xFFF472B6,
        iconEmoji = "🐱"
    ),
    QUANTUM_PUP(
        id = "quantum_pup",
        petName = "Pixel",
        title = "Quantum Guard Pup",
        description = "Loyal phone guardian and proactive routine scheduler.",
        primaryColorHex = 0xFF0284C7,
        accentColorHex = 0xFFFBBF24,
        iconEmoji = "🐶"
    ),
    ROBO_BUNNY(
        id = "robo_bunny",
        petName = "Nova",
        title = "Hyper-Speed Bunny",
        description = "Analytic specialist with extra-sensory app inspection.",
        primaryColorHex = 0xFFEC4899,
        accentColorHex = 0xFF8B5CF6,
        iconEmoji = "🐰"
    ),
    POCKET_DRAKE(
        id = "pocket_drake",
        petName = "Ignis",
        title = "Mini Astral Dragon",
        description = "Powerhouse executor who roars through complex chains.",
        primaryColorHex = 0xFFEF4444,
        accentColorHex = 0xFFF97316,
        iconEmoji = "🐲"
    ),
    STAR_FOX(
        id = "star_fox",
        petName = "Aero",
        title = "Cosmic Navigator Fox",
        description = "Wise strategist that coordinates multi-agent synthesis.",
        primaryColorHex = 0xFF10B981,
        accentColorHex = 0xFF06B6D4,
        iconEmoji = "🦊"
    )
}

enum class PetMood(val label: String, val bubbleColorHex: Long) {
    IDLE("Ready for orders", 0xFF6366F1),
    HAPPY("Feeling great!", 0xFF10B981),
    THINKING("Consulting AI harness...", 0xFF8B5CF6),
    EXECUTING("Automating across apps...", 0xFFF59E0B),
    CELEBRATING("Task completed!", 0xFF06B6D4),
    SLEEPING("Recharging power cells...", 0xFF64748B)
}

data class PetState(
    val species: PetSpecies = PetSpecies.CYBER_CAT,
    val customName: String = "Kiko",
    val mood: PetMood = PetMood.IDLE,
    val currentSpeech: String = "Hello! I'm your AI Pet Companion. Tap me or assign a task across your apps!",
    val isOverlayActive: Boolean = false,
    val happinessLevel: Int = 95,
    val energyLevel: Int = 90,
    val completedTasksCount: Int = 0
)
