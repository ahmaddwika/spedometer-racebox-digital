package com.speedometer.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.speedometer.app.databinding.ActivityMainBinding
import com.speedometer.app.ui.history.HistoryFragment
import com.speedometer.app.ui.racebox.RaceboxFragment
import com.speedometer.app.ui.settings.SettingsFragment
import com.speedometer.app.ui.speedometer.SpeedometerFragment

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (savedInstanceState == null) {
            showFragment(SpeedometerFragment())
        }

        binding.bottomNav.setOnItemSelectedListener { item ->
            val frag = when (item.itemId) {
                R.id.nav_speedometer -> SpeedometerFragment()
                R.id.nav_racebox -> RaceboxFragment()
                R.id.nav_history -> HistoryFragment()
                R.id.nav_settings -> SettingsFragment()
                else -> null
            }
            frag?.let { showFragment(it); true } ?: false
        }

        requestPermissionsIfNeeded()
    }

    private fun showFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }

    private fun requestPermissionsIfNeeded() {
        val permissions = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        val missing = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, missing.toTypedArray(), REQ_PERMISSIONS)
        }
    }

    companion object {
        private const val REQ_PERMISSIONS = 42
    }
}
