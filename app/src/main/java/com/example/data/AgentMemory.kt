package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "agent_memories")
data class AgentMemory(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val encounterId: Long,
    val timestamp: Long,
    val memoryTag: String,
    val lessonLearned: String
)

@Dao
interface AgentMemoryDao {
    @Query("SELECT * FROM agent_memories ORDER BY timestamp DESC")
    fun getAllMemories(): Flow<List<AgentMemory>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMemory(memory: AgentMemory)

    @Query("SELECT * FROM agent_memories ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentMemories(limit: Int): List<AgentMemory>
}
