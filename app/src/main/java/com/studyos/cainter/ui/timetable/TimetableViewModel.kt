package com.studyos.cainter.ui.timetable

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.studyos.cainter.data.local.entity.ScheduleItem
import com.studyos.cainter.data.local.entity.ScheduleType
import com.studyos.cainter.data.repository.ScheduleRepository
import com.studyos.cainter.util.DateUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TimetableViewModel(
    private val repository: ScheduleRepository
) : ViewModel() {

    private val _selectedDay = MutableStateFlow(DateUtils.getTodayDayOfWeek())
    val selectedDay: StateFlow<Int> = _selectedDay.asStateFlow()

    private val _scheduleItems = MutableStateFlow<List<ScheduleItem>>(emptyList())
    val scheduleItems: StateFlow<List<ScheduleItem>> = _scheduleItems.asStateFlow()

    val activeScheduleType: StateFlow<ScheduleType?> = repository.getActiveScheduleType()
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    init {
        viewModelScope.launch {
            repository.removeDuplicates()
            val activeId = repository.getActiveScheduleTypeSync()?.id ?: 1L
            val day1Items = repository.getScheduleForDaySync(1, activeId)
            val hasLiveAudit = day1Items.any { it.title.contains("Live Audit", ignoreCase = true) }
            if (day1Items.isNotEmpty() && !hasLiveAudit) {
                repository.resetScheduleForType(activeId)
            }
        }
        viewModelScope.launch {
            combine(_selectedDay, repository.getActiveScheduleType()) { day, activeType ->
                val typeId = activeType?.id ?: repository.getActiveScheduleTypeSync()?.id ?: 1L
                Pair(day, typeId)
            }.flatMapLatest { (day, typeId) ->
                repository.getScheduleForDay(day, typeId)
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
            val currentTypeId = activeScheduleType.value?.id
                ?: repository.getActiveScheduleTypeSync()?.id
                ?: 1L
            val itemToSave = if (item.scheduleTypeId == 0L) {
                item.copy(scheduleTypeId = currentTypeId)
            } else {
                item
            }
            if (itemToSave.id == 0L) {
                repository.insertItem(itemToSave)
            } else {
                repository.updateItem(itemToSave)
            }
        }
    }

    fun deleteItem(item: ScheduleItem) {
        viewModelScope.launch {
            repository.deleteItemById(item.id)
        }
    }

    fun resetToDefaults() {
        viewModelScope.launch {
            val currentTypeId = activeScheduleType.value?.id
                ?: repository.getActiveScheduleTypeSync()?.id
                ?: 1L
            repository.resetScheduleForType(currentTypeId)
        }
    }

    class Factory(private val repository: ScheduleRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return TimetableViewModel(repository) as T
        }
    }
}
