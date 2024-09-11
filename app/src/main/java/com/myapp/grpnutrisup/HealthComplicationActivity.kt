package com.myapp.grpnutrisup

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

class HealthComplicationActivity : AppCompatActivity() {

    private lateinit var buttonYes: Button
    private lateinit var buttonNo: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_health_complication)

        buttonYes = findViewById(R.id.buttonYes)
        buttonNo = findViewById(R.id.buttonNo)

        buttonYes.setOnClickListener {
            showWarningDialog()
        }

        buttonNo.setOnClickListener {
            val intent = Intent(this, GoalSelectionActivity::class.java)
            startActivity(intent)
            finish() // Optionally close this activity
        }
    }

    private fun showWarningDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Warning")
        builder.setMessage("Please consult a healthcare provider before proceeding.")
        builder.setPositiveButton("OK") { dialog, _ ->
            dialog.dismiss()
        }
        val alertDialog = builder.create()
        alertDialog.show()
    }
}
