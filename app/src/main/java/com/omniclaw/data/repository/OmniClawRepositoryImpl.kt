package com.omniclaw.data.repository
import com.omniclaw.core.logging.FileLogger

import com.omniclaw.data.local.dao.OmniClawDao
import com.omniclaw.data.local.entity.*
import com.omniclaw.domain.models.*
import com.omniclaw.domain.repository.OmniClawRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class OmniClawRepositoryImpl(
    private val dao: OmniClawDao
) : OmniClawRepository {
    private val TAG = "OmniClawRepository"

    override fun getAllProjects(): Flow<List<Project>> =
        dao.getAllProjects().map { list -> list.map { it.toProject() } }

    override suspend fun insertProject(project: Project) {
        dao.insertProject(project.toEntity())
    }

    override fun getAllSessions(): Flow<List<ChatSession>> =
        dao.getAllSessions().map { list -> list.map { it.toSession() } }

    override fun getSessionsForProject(projectId: String): Flow<List<ChatSession>> =
        dao.getSessionsForProject(projectId).map { list -> list.map { it.toSession() } }

    override suspend fun insertSession(session: ChatSession) {
        FileLogger.d(TAG, "insertSession", "id=${session.id} title=${session.title}")
        dao.insertSession(session.toEntity())
    }

    override fun getMessagesForSession(sessionId: String): Flow<List<Message>> =
        dao.getMessagesForSession(sessionId).map { list -> list.map { it.toMessage() } }

    override suspend fun insertMessage(message: Message) {
        FileLogger.d(TAG, "insertMessage", "sessionId=${message.sessionId} role=${message.role}")
        dao.insertMessage(message.toEntity())
    }

    override suspend fun deleteEmptySessions() {
        dao.deleteEmptySessions()
    }

    override suspend fun deleteSession(sessionId: String) {
        dao.deleteMessagesForSession(sessionId)
        dao.deleteSession(sessionId)
    }

    override suspend fun updateSessionTitle(sessionId: String, title: String) {
        dao.updateSessionTitle(sessionId, title, System.currentTimeMillis())
    }

    override fun getAllAgents(): Flow<List<Agent>> =
        dao.getAllAgents().map { list -> list.map { it.toAgent() } }

    override suspend fun insertAgent(agent: Agent) {
        FileLogger.i(TAG, "insertAgent", "id=${agent.id} name=${agent.name} runCmd=${agent.runCommand}")
        dao.insertAgent(agent.toEntity())
    }

    override fun getAllTermuxLogs(): Flow<List<TermuxLog>> =
        dao.getAllTermuxLogs().map { list -> list.map { it.toTermuxLog() } }

    override suspend fun insertTermuxLog(log: TermuxLog) {
        dao.insertTermuxLog(log.toEntity())
    }

    override fun getEnabledSkills(): Flow<List<Skill>> =
        dao.getEnabledSkills().map { list -> list.map { it.toSkill() } }

    override fun getAllSkills(): Flow<List<Skill>> =
        dao.getAllSkills().map { list -> list.map { it.toSkill() } }

    override suspend fun insertSkill(skill: Skill) {
        FileLogger.i(TAG, "insertSkill", "id=${skill.id} name=${skill.name}")
        dao.insertSkill(skill.toEntity())
    }

    override suspend fun setSkillEnabled(skillId: String, enabled: Boolean) {
        dao.setSkillEnabled(skillId, enabled)
    }

    override suspend fun updateSkillContent(skillId: String, content: String) {
        dao.updateSkillContent(skillId, content)
    }

    override suspend fun deleteSkill(skill: Skill) {
        dao.deleteSkill(skill.toEntity())
    }
}
