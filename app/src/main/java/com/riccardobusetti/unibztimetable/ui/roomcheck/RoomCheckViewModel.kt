package com.riccardobusetti.unibztimetable.ui.roomcheck

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.riccardobusetti.unibztimetable.domain.entities.DisplayableCourseGroup
import com.riccardobusetti.unibztimetable.domain.entities.app.AppSection
import com.riccardobusetti.unibztimetable.domain.entities.params.TimetableParams
import com.riccardobusetti.unibztimetable.domain.usecases.CheckRoomAvailabilityUseCase
import com.riccardobusetti.unibztimetable.ui.custom.BaseViewModel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class RoomCheckViewModel(
    private val checkRoomAvailabilityUseCase: CheckRoomAvailabilityUseCase
) : BaseViewModel() {

    private val _nextRoomTimetable = MutableLiveData<DisplayableCourseGroup>()
    val nextRoomTimetable: LiveData<DisplayableCourseGroup>
        get() = _nextRoomTimetable

    override fun start() {

    }

    fun checkRoomAvailability(room: String) {
        viewModelScope.launch {
            checkRoomAvailabilityUseCase.execute(
                TimetableParams(
                    searchKeyword = room
                )
            )
                .onEach {
                    _nextRoomTimetable.value =
                        DisplayableCourseGroup.build(it, AppSection.ROOM_CHECK).first()
                }
                .collect()
        }
    }
}