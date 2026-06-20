package com.example.data.api.tools

import com.example.data.local.runner.LocalCommandRunner
import com.example.domain.api.Tool
import com.example.domain.api.ToolResult

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
