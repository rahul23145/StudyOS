package com.studyos.cainter.ui.timetable

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.studyos.cainter.data.local.entity.ScheduleItem
import com.studyos.cainter.data.repository.ScheduleRepository
import com.studyos.cainter.util.DateUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch

class TimetableViewModel(
    private val repository: ScheduleRepository
) : ViewModel() {

    private val _selectedDay = MutableStateFlow(DateUtils.getTodayDayOfWeek())
    val selectedDay: StateFlow<Int> = _selectedDay.asStateFlow()

    private val _scheduleItems = MutableStateFlow<List<ScheduleItem>>(emptyList())
    val scheduleItems: StateFlow<List<ScheduleItem>> = _scheduleItems.asStateFlow()

    init {
        viewModelScope.launch {
            _selectedDay.flatMapLatest { day ->
                repository.getScheduleForDay(day)
            }.collect { items ->
                _scheduleItems.value = items
            }
        }
    }

    fun selectDay(day: Int) {
        _selectedDay.value = day
    }

    fun saveItem(item: ScheduleItem) {
        viewModelScope.launch {
            if (item.id == 0L) {
                repository.insertItem(item)
            } else {
                repository.updateItem(item)
            }
        }
    }

    fun deleteItem(item: ScheduleItem) {
        viewModelScope.launch {
            repository.deleteItem(item)
        }
    }

    fun resetToDefaults() {
        viewModelScope.launch {
            repository.resetToDefaultSchedule()
        }
    }

    class Factory(private val repository: ScheduleRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return TimetableViewModel(repository) as T
        }
    }
}
