package com.riccardobusetti.unibztimetable.ui.choosefaculty

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.riccardobusetti.unibztimetable.domain.entities.UserPrefs
import com.riccardobusetti.unibztimetable.domain.entities.choices.Degree
import com.riccardobusetti.unibztimetable.domain.entities.choices.Department
import com.riccardobusetti.unibztimetable.domain.entities.choices.FacultyChoice
import com.riccardobusetti.unibztimetable.domain.entities.choices.StudyPlan
import com.riccardobusetti.unibztimetable.domain.entities.onlyChooseFaculty
import com.riccardobusetti.unibztimetable.domain.entities.params.UserPrefsParams
import com.riccardobusetti.unibztimetable.domain.repositories.ChooseFacultyRepository
import com.riccardobusetti.unibztimetable.domain.usecases.DeleteLocalTimetableUseCase
import com.riccardobusetti.unibztimetable.domain.usecases.PutUserPrefsUseCase
import com.riccardobusetti.unibztimetable.ui.custom.AdvancedViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.*

class ChooseFacultyViewModel(
    private val context: Context,
    private val repository: ChooseFacultyRepository,
    private val putUserPrefsUseCase: PutUserPrefsUseCase,
    private val deleteLocalTimetableUseCase: DeleteLocalTimetableUseCase
) : AdvancedViewModel() {

    companion object {
        private const val TAG = "ChooseFacultyViewModel"
    }

    private val _choices = MutableLiveData<List<FacultyChoice>>()
    val choices: LiveData<List<FacultyChoice>>
        get() = _choices

    private val _showNextButton = MutableLiveData<Boolean>(false)
    val showNextButton: LiveData<Boolean>
        get() = _showNextButton

    private val choicesStack: Stack<Pair<UserPrefs.Pref, FacultyChoice>> = Stack()

    override fun start() {
        loadData()
    }

    private fun loadData() {
        hideNextButton()
        if (choicesStack.empty()) {
            loadDepartments()
        } else {
            val currentChoice = choicesStack.peek()
            when (currentChoice.first) {
                UserPrefs.Pref.DEPARTMENT_KEY -> loadDegrees((currentChoice.second as Department))
                UserPrefs.Pref.DEGREE_KEY -> loadStudyPlans((currentChoice.second as Degree))
                UserPrefs.Pref.STUDY_PLAN_KEY -> showNextButton()
            }
        }
    }

    private fun loadChoices(
        mutableLiveData: MutableLiveData<List<FacultyChoice>>,
        block: suspend () -> List<FacultyChoice>
    ) {
        viewModelScope.safeLaunch(TAG) {
            val result = withContext(Dispatchers.IO) { block() }
            // If we don't get data from the server it means that a specific department... has no
            // degrees...
            if (result.isEmpty())
                showNextButton()
            else
                mutableLiveData.value = result
        }
    }

    private fun loadDepartments() {
        loadChoices(_choices) {
            repository.getDepartments()
        }
    }

    private fun loadDegrees(department: Department) {
        loadChoices(_choices) {
            repository.getDegrees(department.id)
        }
    }

    private fun loadStudyPlans(degree: Degree) {
        loadChoices(_choices) {
            repository.getStudyPlans(degree.id)
        }
    }

    fun select(choice: FacultyChoice) {
        when (choice) {
            is Department -> selectDepartment(choice)
            is Degree -> selectDegree(choice)
            is StudyPlan -> selectStudyPlan(choice)
        }
        loadData()
    }

    private fun selectDepartment(department: Department) {
        choicesStack.push(UserPrefs.Pref.DEPARTMENT_KEY to department)
    }

    private fun selectDegree(degree: Degree) {
        choicesStack.push(UserPrefs.Pref.DEGREE_KEY to degree)
    }

    private fun selectStudyPlan(studyPlan: StudyPlan) {
        choicesStack.push(UserPrefs.Pref.STUDY_PLAN_KEY to studyPlan)
    }

    fun saveUserPrefs() {
        viewModelScope.safeLaunch(TAG) {
            withContext(Dispatchers.IO) {
                putUserPrefsUseCase.execute(UserPrefsParams(buildUserPrefs()))
                deleteLocalTimetableUseCase.execute(null)
            }
        }
    }

    private fun buildUserPrefs(): UserPrefs {
        val userPrefsMap = mutableMapOf<UserPrefs.Pref, String>()
        val choicesList = choicesStack.toList()

        UserPrefs.Pref.values().onlyChooseFaculty().forEach { pref ->
            val choice = choicesList.find { it.first == pref }
            userPrefsMap[pref] = getFacultyChoiceKey(choice?.second)
        }

        return UserPrefs(userPrefsMap)
    }

    private fun getFacultyChoiceKey(choice: FacultyChoice?) = when (choice) {
        is Department -> choice.key
        is Degree -> choice.key
        is StudyPlan -> choice.key
        else -> UserPrefs.NO_VALUE
    }

    fun goBack() {
        if (choicesStack.isNotEmpty()) {
            // If the item we pop is of type study plan it means we are at the end, thus we
            // pop two times in order to go back to loading the degree. This is done because
            // when we select the study plan a new list isn't fetched.
            if (choicesStack.pop().first == UserPrefs.Pref.STUDY_PLAN_KEY) {
                choicesStack.pop()
            }
        }
        loadData()
    }

    private fun showNextButton() {
        _showNextButton.value = true
    }

    private fun hideNextButton() {
        _showNextButton.value = false
    }
}