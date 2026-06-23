package com.omniclaw.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.omniclaw.data.local.dao.OmniClawDao
import com.omniclaw.data.local.entity.*

@Database(
    entities = [
        ProjectEntity::class,
        SessionEntity::class,
        MessageEntity::class,
        AgentEntity::class,
        TermuxLogEntity::class
    ],
    version = DATABASE_VERSION,
    exportSchema = false
)
abstract class OmniClawDatabase : RoomDatabase() {
    abstract fun dao(): OmniClawDao

    companion object {
        private const val DATABASE_VERSION = 2
    }
}
