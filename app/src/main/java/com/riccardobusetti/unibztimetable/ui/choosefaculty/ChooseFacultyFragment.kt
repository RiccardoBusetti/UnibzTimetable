package com.riccardobusetti.unibztimetable.ui.choosefaculty

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.riccardobusetti.unibztimetable.R
import com.riccardobusetti.unibztimetable.domain.entities.app.AppSection
import com.riccardobusetti.unibztimetable.domain.entities.choices.FacultyChoice
import com.riccardobusetti.unibztimetable.domain.repositories.ChooseFacultyRepository
import com.riccardobusetti.unibztimetable.domain.repositories.TimetableRepository
import com.riccardobusetti.unibztimetable.domain.repositories.UserPrefsRepository
import com.riccardobusetti.unibztimetable.domain.strategies.LocalTimetableStrategy
import com.riccardobusetti.unibztimetable.domain.strategies.RemoteTimetableStrategy
import com.riccardobusetti.unibztimetable.domain.strategies.SharedPreferencesUserPrefsStrategy
import com.riccardobusetti.unibztimetable.domain.usecases.DeleteLocalTimetableUseCase
import com.riccardobusetti.unibztimetable.domain.usecases.PutUserPrefsUseCase
import com.riccardobusetti.unibztimetable.ui.custom.BackableFragment
import com.riccardobusetti.unibztimetable.ui.custom.BaseFragment
import com.riccardobusetti.unibztimetable.ui.items.FacultyItem
import com.riccardobusetti.unibztimetable.ui.setup.SetupActivity
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.GroupieViewHolder
import kotlinx.android.synthetic.main.fragment_choose_faculty.*

class ChooseFacultyFragment : BaseFragment<ChooseFacultyViewModel>(), BackableFragment {

    override val appSection: AppSection
        get() = AppSection.CHOOSE_FACULTY

    private val groupAdapter = GroupAdapter<GroupieViewHolder>()

    private lateinit var recyclerView: RecyclerView
    private lateinit var nextButton: Button

    override fun initViewModel(): ChooseFacultyViewModel {
        val userPrefsRepository =
            UserPrefsRepository(SharedPreferencesUserPrefsStrategy(requireContext()))
        val timetableRepository = TimetableRepository(
            LocalTimetableStrategy(requireContext()),
            RemoteTimetableStrategy()
        )

        return ViewModelProviders.of(
            this,
            ChooseFacultyViewModelFactory(
                requireContext(),
                ChooseFacultyRepository(),
                PutUserPrefsUseCase(userPrefsRepository),
                DeleteLocalTimetableUseCase(timetableRepository)
            )
        ).get(ChooseFacultyViewModel::class.java)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_choose_faculty, container, false)
    }

    override fun setupUI() {
        recyclerView = fragment_choose_faculty_recyclerview
        recyclerView.apply {
            layoutManager = LinearLayoutManager(activity)
            adapter = groupAdapter
        }

        nextButton = fragment_choose_faculty_next_button
        nextButton.setOnClickListener {
            model?.saveUserPrefs()
            (requireActivity() as SetupActivity).finishSetup()
        }
    }

    override fun attachObservers() {
        model?.let {
            it.choices.observe(this@ChooseFacultyFragment, Observer { choices ->
                showChoices(choices)
            })

            it.showNextButton.observe(this@ChooseFacultyFragment, Observer { show ->
                if (show) {
                    showNextButton()
                } else {
                    hideNextButton()
                }
            })
        }
    }

    override fun onBackPressed() {
        model?.goBack()
    }

    private fun showChoices(choices: List<FacultyChoice>) {
        groupAdapter.clear()
        choices.forEach { choice ->
            groupAdapter.add(FacultyItem(choice) { clickedChoice -> handleChoiceClick(clickedChoice) })
        }
    }

    private fun handleChoiceClick(choice: FacultyChoice) {
        model?.select(choice)
    }

    private fun showNextButton() {
        nextButton.visibility = View.VISIBLE
    }

    private fun hideNextButton() {
        nextButton.visibility = View.GONE
    }
}