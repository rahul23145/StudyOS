package com.studyos.cainter.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.studyos.cainter.data.local.entity.ScheduleType
import kotlinx.coroutines.flow.Flow

@Dao
interface ScheduleTypeDao {
    @Query("SELECT * FROM schedule_types ORDER BY id ASC")
    fun getAllScheduleTypes(): Flow<List<ScheduleType>>

    @Query("SELECT * FROM schedule_types WHERE isActive = 1 LIMIT 1")
    fun getActiveScheduleType(): Flow<ScheduleType?>

    @Query("SELECT * FROM schedule_types WHERE isActive = 1 LIMIT 1")
    suspend fun getActiveScheduleTypeSync(): ScheduleType?

    @Query("SELECT COUNT(*) FROM schedule_types")
    suspend fun getScheduleTypeCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(scheduleType: ScheduleType): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(types: List<ScheduleType>)

    @Update
    suspend fun update(scheduleType: ScheduleType)

    @Delete
    suspend fun delete(scheduleType: ScheduleType)

    @Query("UPDATE schedule_types SET isActive = 0")
    suspend fun clearActiveStatus()

    @Query("UPDATE schedule_types SET isActive = 1 WHERE id = :typeId")
    suspend fun setActiveStatus(typeId: Long)
}
