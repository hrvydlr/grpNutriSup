package com.myapp.grpnutrisup

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.firestore.FirebaseFirestore
import com.myapp.grpnutrisup.adapters.FoodAdapter
import com.myapp.grpnutrisup.models.Food

class FoodSearchActivity : AppCompatActivity() {

    private lateinit var autoCompleteTextView: AutoCompleteTextView
    private lateinit var recyclerViewFoods: RecyclerView
    private lateinit var foodAdapter: FoodAdapter
    private lateinit var foodList: List<Food>
    private lateinit var progressBar: ProgressBar
    private lateinit var emptyMessage: TextView
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_food_search)

        // Initialize Firestore
        db = FirebaseFirestore.getInstance()

        // Initialize UI components
        autoCompleteTextView = findViewById(R.id.autoCompleteTextView)
        recyclerViewFoods = findViewById(R.id.recyclerViewFoods)
        progressBar = findViewById(R.id.progressBar)
        emptyMessage = findViewById(R.id.emptyMessage)
        recyclerViewFoods.layoutManager = LinearLayoutManager(this)

        // Initialize clear button
        val clearButton: ImageButton = findViewById(R.id.clear_button)
        clearButton.setOnClickListener {
            autoCompleteTextView.text.clear()
            foodAdapter.updateList(foodList) // Show all items again
            emptyMessage.visibility = View.GONE
        }

        // Set up bottom navigation
        val bottomNavigation: BottomNavigationView = findViewById(R.id.bottom_navigation)
        bottomNavigation.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_home -> {
                    startActivity(Intent(this, MainActivity::class.java))
                    true
                }
                R.id.navigation_search -> {
                    true
                }
                R.id.navigation_meal -> {
                    // Start MealPlanActivity
                    startActivity(Intent(this, MealPlanActivity::class.java))
                    true
                }
                R.id.navigation_profile -> {
                    // Start ProfileActivity
                    startActivity(Intent(this, ProfileActivity::class.java))
                    true
                }
                else -> false
            }
        }

        // Fetch food data from Firestore
        fetchFoodData()

        // Set up TextWatcher for AutoCompleteTextView
        autoCompleteTextView.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterFoodList(s.toString())
            }

            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun fetchFoodData() {
        // Show the loading spinner
        progressBar.visibility = View.VISIBLE

        // Fetch food items from Firestore
        db.collection("food_db").get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    foodList = documents.map { doc ->
                        Food(
                            foodName = doc.getString("food_name") ?: "",
                            description = doc.getString("food_desc") ?: "",
                            calories = doc.getLong("calories")?.toInt() ?: 0,
                            carbohydrates = doc.getString("carbohydrates") ?: "",
                            fats = doc.getLong("fat")?.toInt() ?: 0,
                            fiber = doc.getString("fiber") ?: "",
                            proteins = doc.getLong("protien")?.toInt() ?: 0,
                            servingSize = doc.getString("serving_size") ?: "",
                            goal = doc.getString("goal_type") ?: "",
                            mealType = doc.getString("meal_type") ?: "",
                            allergens = doc.getString("allergens") ?: ""
                        )
                    }
                    updateRecyclerView()

                    // Set up the AutoCompleteTextView with food names
                    val foodNames = foodList.map { it.foodName }
                    val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, foodNames)
                    autoCompleteTextView.setAdapter(adapter)

                    // Hide empty message and loading spinner
                    emptyMessage.visibility = View.GONE
                    progressBar.visibility = View.GONE
                } else {
                    showEmptyMessage()
                }
            }
            .addOnFailureListener { e ->
                progressBar.visibility = View.GONE
                showErrorSnackbar("Error fetching food data: ${e.message}")
            }
    }

    private fun updateRecyclerView() {
        foodAdapter = FoodAdapter(this, foodList)
        recyclerViewFoods.adapter = foodAdapter
    }

    private fun showEmptyMessage() {
        emptyMessage.visibility = View.VISIBLE
        progressBar.visibility = View.GONE
    }

    private fun showErrorSnackbar(message: String) {
        val snackbar = Snackbar.make(findViewById(R.id.root_layout), message, Snackbar.LENGTH_LONG)
        snackbar.show()
    }

    private fun filterFoodList(query: String) {
        val filteredList = foodList.filter { food ->
            food.foodName.contains(query, ignoreCase = true)
        }

        // Show or hide the empty message based on the filter result
        emptyMessage.visibility = if (filteredList.isEmpty()) View.VISIBLE else View.GONE

        foodAdapter.updateList(filteredList)
    }
}
