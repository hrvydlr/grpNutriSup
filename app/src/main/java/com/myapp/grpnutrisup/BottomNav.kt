package com.myapp.grpnutrisup

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView


class BottomNav : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home) // Your main layout with BottomNavigationView

        // Initialize BottomNavigationView
        val bottomNavigationView: BottomNavigationView = findViewById(R.id.bottom_navigation)

        // Load the default activity on startup (if applicable)
        if (savedInstanceState == null) {
            // Optionally start the HomeActivity as the default activity
            startActivity(Intent(this, HomeActivity::class.java))
        }

        // Set item selected listener for BottomNavigationView
        bottomNavigationView.setOnNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.navigation_home -> {
                    startActivity(Intent(this, HomeActivity::class.java)) // Start Home activity
                    true
                }
                R.id.navigation_search -> {
                    startActivity(Intent(this, FoodSearchActivity::class.java)) // Start Search activity
                    true
                }
                R.id.navigation_meal -> {
                    startActivity(Intent(this, MealActivity::class.java)) // Start Meal Plan activity
                    true
                }
                R.id.navigation_profile -> {
                    startActivity(Intent(this, ProfileActivity::class.java)) // Start Profile activity
                    true
                }
                else -> false
            }
        }
    }
}
