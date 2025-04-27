package com.example.finbot
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.view.MenuItem
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.finbot.databinding.ActivityMainBinding
import com.example.finbot.fragment.profileFragment
import com.example.finbot.fragment.statFragment
import com.example.finbot.fragments.homeFragment
import com.example.finbot.fragment.AddExpenseFragment
import com.example.finbot.util.NotificationHelper
import com.example.finbot.util.SharedPreferencesManager
import com.example.finbot.fragment.earningFragment

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var sharedPrefsManager: SharedPreferencesManager
    private lateinit var notificationHelper: NotificationHelper
    
    // Permission request launcher
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            // Permission granted, initialize notifications
            setupNotifications()
        } else {
            Toast.makeText(this, 
                "Notification permission denied. Budget alerts will be disabled.", 
                Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Initialize managers
        sharedPrefsManager = SharedPreferencesManager.getInstance(this)
        notificationHelper = NotificationHelper.getInstance(this)
        
        // Request notification permission for Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestNotificationPermission()
        } else {
            setupNotifications()
        }
        
        // Handle intent extras
        if (intent.getBooleanExtra("OPEN_ADD_EXPENSE", false)) {
            loadFragment(AddExpenseFragment())
            binding.bottomNavigation.selectedItemId = R.id.nav_add
        } else {
            // Load the default fragment (HomeFragment)
            loadFragment(homeFragment())
        }

        // Set up bottom navigation
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    loadFragment(homeFragment())
                    true
                }
                R.id.nav_stat -> {
                    loadFragment(statFragment())
                    true
                }
                R.id.nav_add -> {
                    loadFragment(AddExpenseFragment())
                    true
                }
                R.id.nav_earning -> {
                    loadFragment(earningFragment())
                    true
                }
                R.id.nav_profile -> {
                    loadFragment(profileFragment())
                    true
                }
                else -> false
            }
        }
        
        // Check for budget alerts
        notificationHelper.checkAndShowBudgetAlertIfNeeded()
    }
    
    private fun requestNotificationPermission() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED -> {
                // Permission already granted
                setupNotifications()
            }
            shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) -> {
                // Explain why we need notification permission
                Toast.makeText(
                    this,
                    "FinBot needs notification permission to send budget alerts",
                    Toast.LENGTH_LONG
                ).show()
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
            else -> {
                // Request the permission
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
    
    private fun setupNotifications() {
        // Schedule daily reminder if enabled
        if (sharedPrefsManager.isReminderEnabled()) {
            notificationHelper.scheduleDailyReminder()
        }
    }
    
    override fun onResume() {
        super.onResume()
        // Check for budget alerts every time the app comes to foreground
        notificationHelper.checkAndShowBudgetAlertIfNeeded()
    }

    // Helper function to load fragments
    fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }
}