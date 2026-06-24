package com.omniclaw.domain.models

enum class AgentPermissionLevel(val label: String) {
    NORMAL("Normal"),
    RULES("Rules"),
    MID_RULES("Mid Rules"),
    FULL_ACCESS("Full Access");

    val allowsAutoExecute: Boolean get() = this == RULES || this == FULL_ACCESS || this == MID_RULES
    val requiresConfirmationForSensitive: Boolean get() = this == NORMAL || this == MID_RULES
    val allowsAllWithoutAsk: Boolean get() = this == FULL_ACCESS

    companion object {
        fun fromValue(value: String): AgentPermissionLevel =
            entries.firstOrNull { it.name == value } ?: NORMAL
    }
}
