package com.omniclaw.data.api.tools

import com.omniclaw.data.local.runner.LocalCommandRunner
import com.omniclaw.domain.api.Tool
import com.omniclaw.domain.api.ToolResult

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
