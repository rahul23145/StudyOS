package com.studyos.cainter.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.studyos.cainter.data.local.entity.TodoItem
import kotlinx.coroutines.flow.Flow

@Dao
interface TodoDao {

    @Query("SELECT * FROM todo_items ORDER BY isCompleted ASC, id DESC")
    fun getAllTodos(): Flow<List<TodoItem>>

    @Query("SELECT * FROM todo_items ORDER BY isCompleted ASC, id DESC")
    suspend fun getAllTodosSync(): List<TodoItem>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTodo(todo: TodoItem): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(todos: List<TodoItem>)

    @Update
    suspend fun updateTodo(todo: TodoItem)

    @Query("UPDATE todo_items SET isCompleted = :isCompleted, completedAt = :completedAt WHERE id = :id")
    suspend fun setCompleted(id: Long, isCompleted: Boolean, completedAt: Long?)

    @Delete
    suspend fun deleteTodo(todo: TodoItem)

    @Query("DELETE FROM todo_items WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM todo_items WHERE isCompleted = 1")
    suspend fun clearCompleted()
}
