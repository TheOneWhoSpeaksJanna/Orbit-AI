package com.example.core.di

import android.content.Context
import androidx.room.Room
import com.example.data.api.tools.ExecuteCommandTool
import com.example.data.api.tools.SudoCommandTool
import com.example.data.api.tools.ToolRegistry
import com.example.data.local.OrbitDatabase
import com.example.data.local.prefs.PreferencesManager
import com.example.data.local.runner.LocalCommandRunner
import com.example.data.repository.OrbitRepositoryImpl
import com.example.domain.api.AiProvider
import com.example.domain.repository.OrbitRepository

interface AppContainer {
    val repository: OrbitRepository
    val prefsManager: PreferencesManager
    val aiProvider: AiProvider
    val toolRegistry: ToolRegistry
    val localCommandRunner: LocalCommandRunner
    val runtimeManager: com.example.data.local.runtime.OrbitRuntimeManager
    val packageInstaller: com.example.data.local.runtime.PackageInstaller
}

class DefaultAppContainer(private val context: Context) : AppContainer {
    private val database: OrbitDatabase by lazy {
        Room.databaseBuilder(context, OrbitDatabase::class.java, "orbit_database")
            .build()
    }

    override val repository: OrbitRepository by lazy {
        OrbitRepositoryImpl(database.dao())
    }

    override val prefsManager: PreferencesManager by lazy {
        PreferencesManager(context)
    }

    override val aiProvider: AiProvider by lazy {
        com.example.data.api.providers.AiProviderSelector()
    }
    
    override val runtimeManager: com.example.data.local.runtime.OrbitRuntimeManager by lazy {
        com.example.data.local.runtime.OrbitRuntimeManager(context)
    }

    override val packageInstaller: com.example.data.local.runtime.PackageInstaller by lazy {
        com.example.data.local.runtime.PackageInstaller(
            runtimeManager,
            okhttp3.OkHttpClient.Builder().build()
        )
    }

    override val localCommandRunner: LocalCommandRunner by lazy {
        LocalCommandRunner(runtimeManager)
    }

    override val toolRegistry: ToolRegistry by lazy {
        ToolRegistry(
            listOf(
                ExecuteCommandTool(localCommandRunner),
                SudoCommandTool(localCommandRunner)
            )
        )
    }
}
