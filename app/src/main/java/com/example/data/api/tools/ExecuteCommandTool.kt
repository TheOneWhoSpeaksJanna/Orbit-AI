package com.example.data.api.tools

import com.example.data.local.runner.LocalCommandRunner
import com.example.domain.api.Tool
import com.example.domain.api.ToolResult

class ExecuteCommandTool(
    private val runner: LocalCommandRunner
) : Tool {
    override val name = "RUN"
    override val description = "Execute a shell command and return its output"
    override suspend fun execute(params: String): ToolResult {
        val result = runner.executeCommand(params)
        return ToolResult(
            output = result.output,
            exitCode = result.exitCode,
            command = params
        )
    }
}
