package com.example.myapplication

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.myapplication.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    // Using ViewBinding to access views - this avoids findViewById() calls and is type-safe
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize ViewBinding - this inflates our layout and gives us access to all views
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Get a reference to our NavHostFragment which contains all our app's fragments
        // This is the container that will swap fragments in and out as user navigates
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment_activity_main) as NavHostFragment
        val navController = navHostFragment.navController

        // Connect our bottom navigation with the NavController
        // This automatically handles selecting the right nav item when fragments change
        binding.navView.setupWithNavController(navController)

        // Listen for destination changes to show/hide the bottom nav on certain screens
        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                // Hide the bottom navigation bar on workout and add exercise screens
                // This gives more screen space for these interactive features
                R.id.navigation_active_workout, R.id.navigation_add_exercise -> {
                    binding.navView.visibility = View.GONE
                }
                else -> {
                    // Show bottom nav on all other screens (history, profile, etc.)
                    binding.navView.visibility = View.VISIBLE
                }
            }
        }
    }
}