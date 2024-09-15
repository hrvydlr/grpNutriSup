package com.myapp.grpnutrisup

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.CheckBox
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

class AllergenSelectionActivity : AppCompatActivity() {

    private lateinit var checkBoxMilk: CheckBox
    private lateinit var checkBoxEggs: CheckBox
    private lateinit var checkBoxFish: CheckBox
    private lateinit var checkBoxShellfish: CheckBox
    private lateinit var checkBoxTreeNuts: CheckBox
    private lateinit var checkBoxPeanuts: CheckBox
    private lateinit var checkBoxWheat: CheckBox
    private lateinit var checkBoxSoybeans: CheckBox
    private lateinit var checkBoxSesame: CheckBox
    private lateinit var checkBoxNone: CheckBox
    private lateinit var buttonSubmitAllergens: Button

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_allergen_selection)

        // Initialize FirebaseAuth and Firestore
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Initialize the views
        checkBoxMilk = findViewById(R.id.checkBoxMilk)
        checkBoxEggs = findViewById(R.id.checkBoxEggs)
        checkBoxFish = findViewById(R.id.checkBoxFish)
        checkBoxShellfish = findViewById(R.id.checkBoxShellfish)
        checkBoxTreeNuts = findViewById(R.id.checkBoxTreeNuts)
        checkBoxPeanuts = findViewById(R.id.checkBoxPeanuts)
        checkBoxWheat = findViewById(R.id.checkBoxWheat)
        checkBoxSoybeans = findViewById(R.id.checkBoxSoybeans)
        checkBoxSesame = findViewById(R.id.checkBoxSesame)
        checkBoxNone = findViewById(R.id.checkBoxNone)
        buttonSubmitAllergens = findViewById(R.id.buttonSubmitAllergens)

        // Handle the "None" checkbox logic
        checkBoxNone.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                disableAllOtherCheckBoxes(true)
            } else {
                disableAllOtherCheckBoxes(false)
            }
        }

        buttonSubmitAllergens.setOnClickListener {
            saveAllergens()
        }
    }

    private fun disableAllOtherCheckBoxes(disable: Boolean) {
        // Disable/enable all allergen checkboxes based on the "None" selection
        checkBoxMilk.isEnabled = !disable
        checkBoxEggs.isEnabled = !disable
        checkBoxFish.isEnabled = !disable
        checkBoxShellfish.isEnabled = !disable
        checkBoxTreeNuts.isEnabled = !disable
        checkBoxPeanuts.isEnabled = !disable
        checkBoxWheat.isEnabled = !disable
        checkBoxSoybeans.isEnabled = !disable
        checkBoxSesame.isEnabled = !disable
    }

    private fun saveAllergens() {
        val user = auth.currentUser
        if (user != null) {
            val userEmail = user.email // Use email instead of UID for storing user-specific data

            if (userEmail != null) {
                // Create a list of selected allergens
                val allergens = mutableListOf<String>()
                if (checkBoxMilk.isChecked) allergens.add("Milk")
                if (checkBoxEggs.isChecked) allergens.add("Eggs")
                if (checkBoxFish.isChecked) allergens.add("Fish")
                if (checkBoxShellfish.isChecked) allergens.add("Crustacean shellfish")
                if (checkBoxTreeNuts.isChecked) allergens.add("Tree nuts")
                if (checkBoxPeanuts.isChecked) allergens.add("Peanuts")
                if (checkBoxWheat.isChecked) allergens.add("Wheat")
                if (checkBoxSoybeans.isChecked) allergens.add("Soybeans")
                if (checkBoxSesame.isChecked) allergens.add("Sesame")
                if (checkBoxNone.isChecked) allergens.add("None")

                // Create a map to store the allergen data
                val allergenData = mapOf(
                    "allergens" to allergens
                )

                // Save the allergen data under the user's document in Firestore using the email
                db.collection("users").document(userEmail)
                    .set(allergenData, SetOptions.merge()) // Use set() with merge to update/merge fields
                    .addOnSuccessListener {
                        Toast.makeText(this, "Allergens saved successfully!", Toast.LENGTH_SHORT).show()

                        // After saving, navigate to the next activity (e.g., ActivityLevelActivity)
                        val intent = Intent(this, ActivityLevelActivity::class.java)
                        startActivity(intent)
                        finish() // Optionally close this activity
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Failed to save allergens: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            } else {
                Toast.makeText(this, "User email not available!", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "User not logged in!", Toast.LENGTH_SHORT).show()
        }
    }
}
