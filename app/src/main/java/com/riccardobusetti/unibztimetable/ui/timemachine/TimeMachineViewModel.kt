package com.riccardobusetti.unibztimetable.ui.timemachine

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.riccardobusetti.unibztimetable.domain.entities.UserPrefs
import com.riccardobusetti.unibztimetable.domain.usecases.GetIntervalDateTimetableUseCase
import com.riccardobusetti.unibztimetable.domain.usecases.GetUserPrefsUseCase
import com.riccardobusetti.unibztimetable.utils.DateUtils
import com.riccardobusetti.unibztimetable.utils.custom.TimetableViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.*

class TimeMachineViewModel(
    private val getIntervalDateTimetableUseCase: GetIntervalDateTimetableUseCase,
    private val getUserPrefsUseCase: GetUserPrefsUseCase
) : TimetableViewModel() {

    /**
     * Enum representing the state of the bottomsheet which pops up when the user
     * needs to check for an interval of dates in order to perform a time travel.
     */
    enum class BottomSheetState {
        OPENED,
        CLOSED
    }

    companion object {
        private const val TAG = "TimeMachineViewModel"
    }

    val selectedDateInterval = MutableLiveData<Pair<String, String>>().apply {
        this.value =
            DateUtils.getCurrentDateFormatted() to DateUtils.getCurrentDatePlusDaysFormatted(7)
    }

    val bottomSheetState =
        MutableLiveData<BottomSheetState>().apply { this.value = BottomSheetState.CLOSED }

    fun loadTimetable(
        fromDate: String,
        toDate: String,
        page: String = DEFAULT_PAGE
    ) {
        viewModelScope.launchWithSupervisor {
            hideError()
            showLoading()

            val userPrefs = getUserPrefs()
            val newTimetable = try {
                loadTimetable(userPrefs, fromDate, toDate, page)
            } catch (e: Exception) {
                handleTimetableException(TAG, e)
            }

            hideLoading()
            showTimetable(newTimetable)
        }
    }

    private suspend fun getUserPrefs() = withContext(Dispatchers.IO) {
        getUserPrefsUseCase.getUserPrefs()
    }

    private suspend fun loadTimetable(
        userPrefs: UserPrefs, fromDate: String,
        toDate: String,
        page: String
    ) = withContext(Dispatchers.IO) {
        getIntervalDateTimetableUseCase.getTimetableInInterval(
            userPrefs.prefs[UserPrefs.Pref.DEPARTMENT_ID] ?: "",
            userPrefs.prefs[UserPrefs.Pref.DEGREE_ID] ?: "",
            userPrefs.prefs[UserPrefs.Pref.STUDY_PLAN_ID] ?: "",
            fromDate,
            toDate,
            page
        )
    }

    fun getCurrentFromDate(): Calendar? {
        val fromDate = DateUtils.formatStringToDate(selectedDateInterval.value!!.first)

        return if (fromDate != null) {
            DateUtils.getCalendarFromDate(fromDate)
        } else {
            null
        }
    }

    fun updateFromDate(newDate: String) {
        selectedDateInterval.value = newDate to selectedDateInterval.value!!.second
    }

    fun getCurrentToDate(): Calendar? {
        val fromDate = DateUtils.formatStringToDate(selectedDateInterval.value!!.second)

        return if (fromDate != null) {
            DateUtils.getCalendarFromDate(fromDate)
        } else {
            null
        }
    }

    fun updateToDate(newDate: String) {
        selectedDateInterval.value = selectedDateInterval.value!!.first to newDate
    }
}