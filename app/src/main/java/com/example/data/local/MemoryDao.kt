package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.MemoryItem
import kotlinx.coroutines.flow.Flow

@Dao
interface MemoryDao {
    @Query("SELECT COUNT(*) FROM memories WHERE category = :category AND title = :title")
    suspend fun getCountByCategoryAndTitle(category: String, title: String): Int

    @Query("SELECT * FROM memories ORDER BY timestamp DESC")
    fun getAllMemories(): Flow<List<MemoryItem>>

    @Query("SELECT * FROM memories WHERE category = :category ORDER BY timestamp DESC")
    fun getMemoriesByCategory(category: String): Flow<List<MemoryItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMemory(memory: MemoryItem): Long

    @Delete
    suspend fun deleteMemory(memory: MemoryItem)

    @Query("DELETE FROM memories WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM memories")
    suspend fun clearAll()
}
