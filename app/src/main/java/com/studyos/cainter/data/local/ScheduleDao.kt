package com.studyos.cainter.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.studyos.cainter.data.local.entity.ScheduleItem
import kotlinx.coroutines.flow.Flow

@Dao
interface ScheduleDao {
    @Query("SELECT * FROM schedule_items WHERE dayOfWeek = :dayOfWeek ORDER BY startTime ASC")
    fun getScheduleForDay(dayOfWeek: Int): Flow<List<ScheduleItem>>

    @Query("SELECT * FROM schedule_items WHERE dayOfWeek = :dayOfWeek AND scheduleTypeId = :scheduleTypeId ORDER BY startTime ASC")
    fun getScheduleForDayAndType(dayOfWeek: Int, scheduleTypeId: Long): Flow<List<ScheduleItem>>

    @Query("SELECT * FROM schedule_items WHERE dayOfWeek = :dayOfWeek AND scheduleTypeId = :scheduleTypeId ORDER BY startTime ASC")
    suspend fun getScheduleForDayAndTypeSync(dayOfWeek: Int, scheduleTypeId: Long): List<ScheduleItem>

    @Query("SELECT * FROM schedule_items ORDER BY dayOfWeek ASC, startTime ASC")
    fun getAllScheduleItems(): Flow<List<ScheduleItem>>

    @Query("SELECT * FROM schedule_items WHERE scheduleTypeId = :scheduleTypeId ORDER BY dayOfWeek ASC, startTime ASC")
    fun getAllScheduleItemsForType(scheduleTypeId: Long): Flow<List<ScheduleItem>>

    @Query("SELECT * FROM schedule_items WHERE id = :id LIMIT 1")
    suspend fun getScheduleItemById(id: Long): ScheduleItem?

    @Query("SELECT COUNT(*) FROM schedule_items")
    suspend fun getScheduleCount(): Int

    @Query("SELECT COUNT(*) FROM schedule_items WHERE scheduleTypeId = :scheduleTypeId")
    suspend fun getScheduleCountForType(scheduleTypeId: Long): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: ScheduleItem): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<ScheduleItem>)

    @Update
    suspend fun updateItem(item: ScheduleItem)

    @Delete
    suspend fun deleteItem(item: ScheduleItem)

    @Query("DELETE FROM schedule_items")
    suspend fun clearAll()

    @Query("DELETE FROM schedule_items WHERE scheduleTypeId = :scheduleTypeId")
    suspend fun clearForType(scheduleTypeId: Long)
}
