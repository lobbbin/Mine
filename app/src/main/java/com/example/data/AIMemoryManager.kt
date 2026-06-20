package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Handles persistent storage of gameplay context and state, 
 * preventing logic regressions during high-accuracy simulation cycles.
 */
class AIMemoryManager(
    private val agentMemoryDao: AgentMemoryDao,
    private val worldStateDao: WorldStateDao,
    private val settingsDataStore: SettingsDataStore
) {
    suspend fun logSimulationEvent(encounterId: Long, tag: String, lesson: String) {
        val memory = AgentMemory(
            encounterId = encounterId,
            timestamp = System.currentTimeMillis(),
            memoryTag = tag,
            lessonLearned = lesson
        )
        agentMemoryDao.insertMemory(memory)
    }

    suspend fun getRecentContext(limit: Int = 10): String {
        val memories = agentMemoryDao.getRecentMemories(limit)
        if (memories.isEmpty()) return "No previous simulation context available."
        
        return memories.joinToString("\n") { mem ->
            "- [${mem.memoryTag}] Encounter ${mem.encounterId}: ${mem.lessonLearned}"
        }
    }
    
    val allMemoriesFlow: Flow<List<AgentMemory>> = agentMemoryDao.getAllMemories()
}
