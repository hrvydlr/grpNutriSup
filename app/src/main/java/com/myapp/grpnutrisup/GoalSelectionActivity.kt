package com.myapp.grpnutrisup

import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class GoalSelectionActivity : AppCompatActivity() {

    private lateinit var buttonMaintainWeight: Button
    private lateinit var buttonLoseWeight: Button
    private lateinit var buttonGainWeight: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_goal_selection)

        buttonMaintainWeight = findViewById(R.id.buttonMaintainWeight)
        buttonLoseWeight = findViewById(R.id.buttonLoseWeight)
        buttonGainWeight = findViewById(R.id.buttonGainWeight)

        buttonMaintainWeight.setOnClickListener {
            handleGoalSelection("Maintain Weight")
        }

        buttonLoseWeight.setOnClickListener {
            handleGoalSelection("Lose Weight")
        }

        buttonGainWeight.setOnClickListener {
            handleGoalSelection("Gain Weight")
        }
    }

    private fun handleGoalSelection(goal: String) {
        // For now, just show a Toast message
        Toast.makeText(this, "You selected: $goal", Toast.LENGTH_SHORT).show()

        // You can proceed to the next activity or save the selection in a database
    }
}