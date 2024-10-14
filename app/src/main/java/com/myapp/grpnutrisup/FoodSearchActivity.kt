package com.myapp.grpnutrisup

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
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
import kotlinx.coroutines.*

class FoodSearchActivity : AppCompatActivity() {

    private lateinit var autoCompleteTextView: AutoCompleteTextView
    private lateinit var recyclerViewFoods: RecyclerView
    private lateinit var foodAdapter: FoodAdapter
    private var foodList: List<Food> = emptyList() // Avoid late init for safe nullability
    private lateinit var progressBar: ProgressBar
    private lateinit var emptyMessage: TextView
    private lateinit var db: FirebaseFirestore

    // Debounce handler
    private var searchJob: Job? = null

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
        setupBottomNavigation()

        // Fetch food data from Firestore
        fetchFoodData()

        // Set up TextWatcher for AutoCompleteTextView
        autoCompleteTextView.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // Use debounce logic to delay search execution
                searchJob?.cancel()
                searchJob = GlobalScope.launch(Dispatchers.Main) {
                    delay(300L) // Debounce for 300ms
                    filterFoodList(s.toString())
                }
            }

            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun setupBottomNavigation() {
        val bottomNavigation: BottomNavigationView = findViewById(R.id.bottom_navigation)
        bottomNavigation.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_home -> {
                    startActivity(Intent(this, MainActivity::class.java))
                    true
                }
                R.id.navigation_search -> true
                R.id.navigation_meal -> {
                    startActivity(Intent(this, MealPlanActivity::class.java))
                    true
                }
                R.id.navigation_profile -> {
                    startActivity(Intent(this, ProfileActivity::class.java))
                    true
                }
                else -> false
            }
        }
    }

    private fun fetchFoodData() {
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

                    emptyMessage.visibility = View.GONE
                } else {
                    showEmptyMessage()
                }

                progressBar.visibility = View.GONE
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
        if (query.isNotEmpty()) {
            val filteredList = foodList.filter { food ->
                food.foodName.contains(query, ignoreCase = true)
            }

            foodAdapter.updateList(filteredList)

            // Show or hide the empty message based on the filter result
            emptyMessage.visibility = if (filteredList.isEmpty()) View.VISIBLE else View.GONE
        } else {
            // Reset the list if query is empty
            foodAdapter.updateList(foodList)
            emptyMessage.visibility = View.GONE
        }
    }
}
