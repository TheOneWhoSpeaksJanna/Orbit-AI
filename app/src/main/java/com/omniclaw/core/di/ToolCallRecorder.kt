package com.omniclaw.core.di

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ToolCallRecord(
    val sessionId: String,
    val command: String,
    val output: String,
    val exitCode: Int,
    val timestamp: Long = System.currentTimeMillis()
)

class ToolCallRecorder {
    private val _records = MutableStateFlow<List<ToolCallRecord>>(emptyList())
    val records: StateFlow<List<ToolCallRecord>> = _records.asStateFlow()

    // Cap the number of stored records to prevent unbounded memory growth.
    // Without this, long-running sessions accumulate thousands of records
    // that are never cleared (clearSession/clearAll are never called from
    // the UI), causing gradual memory pressure.
    private var maxRecords: Int = DEFAULT_MAX_RECORDS

    fun record(record: ToolCallRecord) {
        val current = _records.value
        _records.value = if (current.size >= maxRecords) {
            current.takeLast(maxRecords - 1) + record
        } else {
            current + record
        }
    }

    fun getRecordsForSession(sessionId: String): List<ToolCallRecord> =
        _records.value.filter { it.sessionId == sessionId }

    fun clearSession(sessionId: String) {
        _records.value = _records.value.filter { it.sessionId != sessionId }
    }

    fun clearAll() {
        _records.value = emptyList()
    }

    companion object {
        private const val DEFAULT_MAX_RECORDS = 500
    }
}
