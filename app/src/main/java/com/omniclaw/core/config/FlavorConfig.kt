package com.omniclaw.core.config

import com.omniclaw.BuildConfig

object FlavorConfig {

    val presetAgentId: String get() = BuildConfig.FLAVOR_PRESET_AGENT_ID
    val presetAgentName: String get() = BuildConfig.FLAVOR_PRESET_AGENT_NAME
    val appLabel: String get() = BuildConfig.FLAVOR_APP_LABEL
    val isNormal: Boolean get() = presetAgentId.isEmpty()
}
