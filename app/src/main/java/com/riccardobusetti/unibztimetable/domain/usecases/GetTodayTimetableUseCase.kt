package com.riccardobusetti.unibztimetable.domain.usecases

import com.riccardobusetti.unibztimetable.data.remote.WebSiteUrl
import com.riccardobusetti.unibztimetable.domain.entities.AppSection
import com.riccardobusetti.unibztimetable.domain.entities.Course
import com.riccardobusetti.unibztimetable.domain.repositories.TimetableRepository
import kotlinx.coroutines.flow.Flow

/**
 * Use case which will manage the today timetable that is responsible of
 * showing to the user the timetable of the current day.
 *
 * @author Riccardo Busetti
 */
class GetTodayTimetableUseCase(
    private val timetableRepository: TimetableRepository
) : UseCase {

    /**
     * Gets the timetable of today.
     */
    fun getTodayTimetable(
        department: String,
        degree: String,
        studyPlan: String,
        page: String,
        isInternetAvailable: Boolean
    ): Flow<List<Course>> {
        val websiteUrl = WebSiteUrl.Builder()
            .useDeviceLanguage()
            .withDepartment(department)
            .withDegree(degree)
            .withStudyPlan(studyPlan)
            .onlyToday()
            .atPage(page)
            .build()

        return timetableRepository.getTimetable(AppSection.TODAY, websiteUrl, isInternetAvailable)
    }

    /**
     * Gets the local timetable for today.
     */
    fun getLocalTodayTimetable() = timetableRepository.getLocalTimetable(AppSection.TODAY)
}