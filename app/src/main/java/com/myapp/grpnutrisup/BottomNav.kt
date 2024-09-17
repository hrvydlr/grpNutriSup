package com.myapp.grpnutrisup

import android.os.Bundle
import android.view.MenuInflater
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.ActionMenuView

class BottomNav : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)  // Change this to activity_home

        // Find the ActionMenuView
        val actionMenuView: ActionMenuView = findViewById(R.id.bottom_navigation)

        // Manually inflate the menu
        val menuInflater: MenuInflater = MenuInflater(this)
        menuInflater.inflate(R.menu.bottom_navigation_menu, actionMenuView.menu)

        // Handle click events
        actionMenuView.setOnMenuItemClickListener { menuItem: MenuItem ->
            when (menuItem.itemId) {
                R.id.navigation_home -> {
                    // Handle Home click
                    true
                }
                R.id.navigation_search -> {
                    // Handle Search click
                    true
                }
                R.id.navigation_meal -> {
                    // Handle Meal click
                    true
                }
                R.id.navigation_profile -> {
                    // Handle Profile click
                    true
                }
                else -> false
            }
        }
    }
}
