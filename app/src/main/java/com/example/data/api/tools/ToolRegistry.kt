package com.example.data.api.tools

import com.example.domain.api.ParsedToolCall
import com.example.domain.api.Tool
import com.example.domain.api.ToolResult

class ToolRegistry(
    private val tools: List<Tool> = emptyList()
) {
    private val toolCallPattern = "\\[(RUN|SUDO): (.+?)]".toRegex()

    fun parseToolCalls(text: String): List<ParsedToolCall> =
        toolCallPattern.findAll(text).map { match ->
            ParsedToolCall(
                toolName = match.groupValues[1],
                params = match.groupValues[2],
                rawMatch = match.value
            )
        }.toList()

    fun containsToolCall(text: String): Boolean = toolCallPattern.containsMatchIn(text)

    suspend fun execute(parsed: ParsedToolCall): ToolResult {
        val tool = tools.find { it.name.equals(parsed.toolName, ignoreCase = true) }
            ?: return ToolResult(
                output = "Error: No tool registered for '${parsed.toolName}'",
                exitCode = -1,
                command = parsed.params
            )
        return tool.execute(parsed.params)
    }
}
