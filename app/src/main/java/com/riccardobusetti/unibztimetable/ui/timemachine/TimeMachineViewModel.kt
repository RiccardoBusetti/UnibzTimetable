package com.riccardobusetti.unibztimetable.ui.timemachine

import android.content.Context
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.riccardobusetti.unibztimetable.domain.entities.Day
import com.riccardobusetti.unibztimetable.domain.entities.UserPrefs
import com.riccardobusetti.unibztimetable.domain.usecases.GetIntervalDateTimetableUseCase
import com.riccardobusetti.unibztimetable.domain.usecases.GetUserPrefsUseCase
import com.riccardobusetti.unibztimetable.utils.DateUtils
import com.riccardobusetti.unibztimetable.utils.custom.TimetableViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import java.util.*

class TimeMachineViewModel(
    private val context: Context,
    private val getIntervalDateTimetableUseCase: GetIntervalDateTimetableUseCase,
    private val getUserPrefsUseCase: GetUserPrefsUseCase
) : TimetableViewModel<List<Day>>() {

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
            DateUtils.getCurrentDateFormatted() to DateUtils.getCurrentDatePlusYearsFormatted(1)
    }

    val bottomSheetState =
        MutableLiveData<BottomSheetState>().apply { this.value = BottomSheetState.CLOSED }

    fun loadTimetable(
        fromDate: String,
        toDate: String,
        page: String = DEFAULT_PAGE
    ) {
        viewModelScope.launchWithSupervisor {
            if (isCurrentPageFirstPage()) loadingState.value = true

            val userPrefs = withContext(Dispatchers.IO) {
                getUserPrefsUseCase.getUserPrefs()
            }

            val work = async(Dispatchers.IO) {
                getIntervalDateTimetableUseCase.getTimetable(
                    userPrefs.prefs[UserPrefs.Pref.DEPARTMENT_ID] ?: "",
                    userPrefs.prefs[UserPrefs.Pref.DEGREE_ID] ?: "",
                    userPrefs.prefs[UserPrefs.Pref.STUDY_PLAN_ID] ?: "",
                    fromDate,
                    toDate,
                    page
                )
            }

            val newTimetable = try {
                work.await()
            } catch (e: Exception) {
                Log.d(TAG, "This error occurred while parsing the timetable -> $e")

                error.value = TimetableError.ERROR_WHILE_GETTING_TIMETABLE

                null
            }

            loadingState.value = false
            newTimetable?.let {
                if (newTimetable.isEmpty() && isCurrentPageFirstPage())
                    error.value = TimetableError.EMPTY_TIMETABLE
                else
                    error.value = null
                    timetable.value = newTimetable
            }
        }
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