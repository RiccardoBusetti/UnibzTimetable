package com.riccardobusetti.unibztimetable.ui.timemachine

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.datetime.datePicker
import com.ethanhua.skeleton.Skeleton
import com.ethanhua.skeleton.SkeletonScreen
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.riccardobusetti.unibztimetable.R
import com.riccardobusetti.unibztimetable.domain.repositories.TimetableRepository
import com.riccardobusetti.unibztimetable.domain.repositories.UserPrefsRepository
import com.riccardobusetti.unibztimetable.domain.strategies.RemoteTimetableStrategy
import com.riccardobusetti.unibztimetable.domain.strategies.SharedPreferencesUserPrefsStrategy
import com.riccardobusetti.unibztimetable.domain.usecases.GetIntervalDateTimetableUseCase
import com.riccardobusetti.unibztimetable.domain.usecases.GetUserPrefsUseCase
import com.riccardobusetti.unibztimetable.ui.items.CourseItem
import com.riccardobusetti.unibztimetable.ui.items.DayItem
import com.riccardobusetti.unibztimetable.utils.DateUtils
import com.riccardobusetti.unibztimetable.utils.custom.AdvancedFragment
import com.riccardobusetti.unibztimetable.utils.custom.views.StatusView
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.GroupieViewHolder
import com.xwray.groupie.Section
import kotlinx.android.synthetic.main.bottom_sheet_date_interval.view.*
import kotlinx.android.synthetic.main.fragment_time_machine.*
import java.util.*


class TimeMachineFragment : AdvancedFragment<TimeMachineViewModel>() {

    private val groupAdapter = GroupAdapter<GroupieViewHolder>()

    private lateinit var statusView: StatusView
    private lateinit var bottomSheetView: View
    private lateinit var fromDateText: TextView
    private lateinit var toDateText: TextView
    private lateinit var timeTravelButton: Button
    private lateinit var bottomSheetDialog: BottomSheetDialog
    private lateinit var recyclerView: RecyclerView
    private lateinit var floatingActionButton: FloatingActionButton
    private lateinit var skeleton: SkeletonScreen

    override fun initModel(): TimeMachineViewModel {
        val timetableRepository = TimetableRepository(RemoteTimetableStrategy())

        val userPrefsRepository = UserPrefsRepository(SharedPreferencesUserPrefsStrategy(context!!))

        return ViewModelProviders.of(
            this,
            TimeMachineViewModelFactory(
                context!!,
                GetIntervalDateTimetableUseCase(timetableRepository),
                GetUserPrefsUseCase(userPrefsRepository)
            )
        ).get(TimeMachineViewModel::class.java)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_time_machine, container, false)
    }

    override fun setupUi() {
        statusView = fragment_time_machine_status_view

        bottomSheetView = layoutInflater.inflate(R.layout.bottom_sheet_date_interval, null)

        fromDateText = bottomSheetView.bottom_sheet_date_interval_from_text
        fromDateText.setOnClickListener {
            MaterialDialog(context!!).show {
                datePicker(
                    currentDate = getCurrentFromDate()
                ) { _, date ->
                    updateFromDate(DateUtils.formatDateToString(date.time))
                }
            }
        }

        toDateText = bottomSheetView.bottom_sheet_date_interval_to_text
        toDateText.setOnClickListener {
            MaterialDialog(context!!).show {
                datePicker(
                    currentDate = getCurrentToDate()
                ) { _, date ->
                    updateToDate(DateUtils.formatDateToString(date.time))
                }
            }
        }

        timeTravelButton = bottomSheetView.bottom_sheet_date_interval_button
        timeTravelButton.setOnClickListener {
            model?.loadTimetable(
                model?.selectedDateInterval?.value!!.first,
                model?.selectedDateInterval?.value!!.second,
                "1"
            )

            changeBottomSheetState()
        }

        bottomSheetDialog = BottomSheetDialog(context!!)
        bottomSheetDialog.setContentView(bottomSheetView)
        bottomSheetDialog.setOnCancelListener { changeBottomSheetState() }

        recyclerView = fragment_time_machine_recycler_view
        recyclerView.apply {
            layoutManager = LinearLayoutManager(activity)
            adapter = groupAdapter
        }

        floatingActionButton = fragment_time_machine_fab
        floatingActionButton.setOnClickListener { changeBottomSheetState() }

        skeleton = Skeleton.bind(recyclerView)
            .adapter(groupAdapter)
            .load(R.layout.item_skeleton)
            .color(R.color.colorSkeletonShimmer)
            .show()
    }

    override fun attachObservers() {
        model?.let {
            it.timetable.observe(this, Observer { timetable ->
                groupAdapter.clear()

                timetable.forEach { day ->
                    val section = Section()
                    section.setHeader(DayItem(day))

                    day.courses.forEach { course ->
                        section.add(CourseItem(course))
                    }

                    groupAdapter.add(section)
                }
            })

            it.error.observe(this, Observer { error ->
                if (error.isNotEmpty()) {
                    showStatusView(error)
                } else {
                    hideStatusView()
                }
            })

            it.loadingState.observe(this, Observer { isLoading ->
                if (isLoading) skeleton.show() else skeleton.hide()
            })

            it.selectedDateInterval.observe(this, Observer { interval ->
                fromDateText.text = interval.first
                toDateText.text = interval.second
            })

            it.bottomSheetState.observe(this, Observer { bottomSheetState ->
                when (bottomSheetState) {
                    TimeMachineViewModel.BottomSheetState.OPENED -> bottomSheetDialog.show()
                    TimeMachineViewModel.BottomSheetState.CLOSED -> bottomSheetDialog.hide()
                    else -> bottomSheetDialog.hide()
                }
            })
        }
    }

    override fun startLoadingData() {
        model?.loadTimetable(
            DateUtils.getCurrentDateFormatted(),
            DateUtils.getCurrentDatePlusYearsFormatted(1),
            "1"
        )
    }

    private fun changeBottomSheetState() {
        model?.bottomSheetState?.value = when (model?.bottomSheetState?.value) {
            TimeMachineViewModel.BottomSheetState.CLOSED -> TimeMachineViewModel.BottomSheetState.OPENED
            TimeMachineViewModel.BottomSheetState.OPENED -> TimeMachineViewModel.BottomSheetState.CLOSED
            null -> TimeMachineViewModel.BottomSheetState.CLOSED
        }
    }

    private fun getCurrentFromDate(): Calendar? {
        val fromDate = DateUtils.formatStringToDate(model?.selectedDateInterval?.value?.first!!)

        return if (fromDate != null) {
            DateUtils.getCalendarFromDate(fromDate)
        } else {
            null
        }
    }

    private fun updateFromDate(newDate: String) {
        model?.selectedDateInterval?.value = newDate to model?.selectedDateInterval?.value!!.second
    }

    private fun getCurrentToDate(): Calendar? {
        val fromDate = DateUtils.formatStringToDate(model?.selectedDateInterval?.value?.second!!)

        return if (fromDate != null) {
            DateUtils.getCalendarFromDate(fromDate)
        } else {
            null
        }
    }

    private fun updateToDate(newDate: String) {
        model?.selectedDateInterval?.value = model?.selectedDateInterval?.value!!.first to newDate
    }

    private fun showStatusView(error: String) {
        statusView.setText(error)
        statusView.visibility = View.VISIBLE
        recyclerView.visibility = View.GONE
    }

    private fun hideStatusView() {
        statusView.visibility = View.GONE
        recyclerView.visibility = View.VISIBLE
    }
}