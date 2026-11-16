package com.studyos.cainter.data.repository

import com.studyos.cainter.data.local.TodoDao
import com.studyos.cainter.data.local.entity.TodoItem
import kotlinx.coroutines.flow.Flow

class TodoRepository(private val todoDao: TodoDao) {

    fun getAllTodos(): Flow<List<TodoItem>> = todoDao.getAllTodos()

    suspend fun addTodo(title: String, category: String = "GENERAL"): Long {
        return todoDao.insertTodo(TodoItem(title = title.trim(), category = category))
    }

    suspend fun setCompleted(id: Long, isCompleted: Boolean) {
        val completedTime = if (isCompleted) System.currentTimeMillis() else null
        todoDao.setCompleted(id, isCompleted, completedTime)
    }

    suspend fun deleteTodoById(id: Long) {
        todoDao.deleteById(id)
    }

    suspend fun clearCompleted() {
        todoDao.clearCompleted()
    }
}
