package com.riccardobusetti.unibztimetable.ui.main

import android.annotation.TargetApi
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricPrompt
import androidx.fragment.app.Fragment
import com.riccardobusetti.unibztimetable.R
import com.riccardobusetti.unibztimetable.ui.adapters.FragmentsAdapter
import com.riccardobusetti.unibztimetable.ui.configuration.ConfigurationActivity
import com.riccardobusetti.unibztimetable.ui.next7days.Next7DaysFragment
import com.riccardobusetti.unibztimetable.ui.timemachine.TimeMachineFragment
import com.riccardobusetti.unibztimetable.ui.today.TodayFragment
import com.riccardobusetti.unibztimetable.utils.custom.views.NonSwipeableViewPager
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    data class IndexableFragment(
        val index: Int,
        val itemId: Int,
        val fragment: Fragment
    )

    private val indexableFragments = listOf(
        IndexableFragment(
            0,
            R.id.action_today,
            TodayFragment()
        ),
        IndexableFragment(
            1,
            R.id.action_next_7_days,
            Next7DaysFragment()
        ),
        IndexableFragment(
            2,
            R.id.action_time_machine,
            TimeMachineFragment()
        )
    )

    private lateinit var viewPager: NonSwipeableViewPager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(activity_main_toolbar)
        supportActionBar!!.setDisplayShowTitleEnabled(false)

        setupUi()
        attachListeners()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_activity_main, menu)

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.action_settings -> {
            startActivity(Intent(this@MainActivity, ConfigurationActivity::class.java))
            true
        }
        else -> {
            // If we got here, the user's action was not recognized.
            // Invoke the superclass to handle it.
            super.onOptionsItemSelected(item)
        }
    }

    private fun setupUi() {
        viewPager = activity_main_view_pager
        viewPager.offscreenPageLimit = indexableFragments.size
        viewPager.adapter = FragmentsAdapter(indexableFragments, supportFragmentManager)
        viewPager.currentItem = indexableFragments[0].index
    }

    private fun attachListeners() {
        activity_main_bottom_navigation.setOnNavigationItemSelectedListener { menuItem ->
            viewPager.currentItem = indexableFragments.find { it.itemId == menuItem.itemId }!!.index

            true
        }
    }

    @TargetApi(Build.VERSION_CODES.P)
    private fun instanceOfBiometricPrompt(): BiometricPrompt {
        val executor = mainExecutor

        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                Toast.makeText(this@MainActivity, "Error while authenticating", Toast.LENGTH_SHORT)
                    .show()
            }

            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                startActivity(Intent(this@MainActivity, ConfigurationActivity::class.java))
            }
        }

        return BiometricPrompt(this, executor, callback)
    }

    private fun getPromptInfo() = BiometricPrompt.PromptInfo.Builder()
        .setTitle("Unibz timetable authentication")
        .setSubtitle("Please login to get access")
        .setDescription("Biometric authentication is required in order to access the settings")
        .setDeviceCredentialAllowed(true)
        .build()
}
