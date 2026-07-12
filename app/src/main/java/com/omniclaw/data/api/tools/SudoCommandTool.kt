package com.omniclaw.data.api.tools

import com.omniclaw.data.local.runner.LocalCommandRunner
import com.omniclaw.domain.api.Tool
import com.omniclaw.domain.api.ToolResult

class SudoCommandTool(
    private val runner: LocalCommandRunner
) : Tool {
    override val name = "SUDO"
    override val description = "Execute a privileged shell command via Shizuku and return its output"
    override suspend fun execute(params: String): ToolResult {
        val result = runner.executePrivilegedCommand(params)
        return ToolResult(
            output = result.output,
            exitCode = result.exitCode,
            command = params
        )
    }
}
