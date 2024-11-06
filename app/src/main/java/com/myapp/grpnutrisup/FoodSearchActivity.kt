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
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.myapp.grpnutrisup.adapters.FoodAdapter
import com.myapp.grpnutrisup.models.Food
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FoodSearchActivity : AppCompatActivity() {

    private lateinit var autoCompleteTextView: AutoCompleteTextView
    private lateinit var recyclerViewFoods: RecyclerView
    private lateinit var foodAdapter: FoodAdapter
    private var foodList: List<Food> = emptyList()
    private lateinit var progressBar: ProgressBar
    private lateinit var emptyMessage: TextView
    private lateinit var db: FirebaseFirestore
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private var favoriteFoodNames: MutableList<String> = mutableListOf() // Initialize here
    private var searchJob: Job? = null
    private var hasHealthComplication: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_food_search)

        db = FirebaseFirestore.getInstance()

        autoCompleteTextView = findViewById(R.id.autoCompleteTextView)
        recyclerViewFoods = findViewById(R.id.recyclerViewFoods)
        progressBar = findViewById(R.id.progressBar)
        emptyMessage = findViewById(R.id.emptyMessage)

        // Set up the RecyclerView layout manager and add spacing decoration
        recyclerViewFoods.layoutManager = LinearLayoutManager(this)
        val spacingInPixels = resources.getDimensionPixelSize(R.dimen.recycler_view_item_spacing)
        recyclerViewFoods.addItemDecoration(SpacingItemDecoration(spacingInPixels))

        findViewById<ImageButton>(R.id.clear_button).setOnClickListener {
            autoCompleteTextView.text.clear()
            foodAdapter.updateList(foodList)
            emptyMessage.visibility = View.GONE
        }

        checkHealthComplication()
        fetchFoodData()

        autoCompleteTextView.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                searchJob?.cancel()
                searchJob = lifecycleScope.launch {
                    delay(300L) // Debounce delay
                    filterFoodList(s.toString())
                }
            }

            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun checkHealthComplication() {
        val currentUser = auth.currentUser ?: return

        db.collection("users").document(currentUser.email!!)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    hasHealthComplication = document.getString("healthComp") == "yes"
                    fetchFavoriteFoodNames() // Fetch favorite food names here
                    setupBottomNavigation()
                }
            }
            .addOnFailureListener {
                setupBottomNavigation()
            }
    }

    private fun fetchFavoriteFoodNames() {
        val currentUser = auth.currentUser ?: return

        db.collection("users").document(currentUser.email!!)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    favoriteFoodNames = document.get("favoriteFoods") as? MutableList<String> ?: mutableListOf()
                    // After fetching favorite food names, update the adapter
                    foodAdapter = FoodAdapter(this, foodList, favoriteFoodNames)
                    recyclerViewFoods.adapter = foodAdapter
                }
            }
            .addOnFailureListener { e ->
                showErrorSnackbar("Error fetching favorite foods: ${e.message}")
            }
    }

    private fun setupBottomNavigation() {
        val bottomNavigation: BottomNavigationView = findViewById(R.id.bottom_navigation)
        bottomNavigation.selectedItemId = R.id.navigation_search
        bottomNavigation.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_home -> {
                    startActivity(Intent(this, HomeActivity::class.java))
                    true
                }
                R.id.navigation_search -> true
                R.id.navigation_meal -> {
                    if (hasHealthComplication) {
                        showHealthComplicationDialog()
                        false
                    } else {
                        startActivity(Intent(this, MealActivity::class.java))
                        true
                    }
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

        db.collection("food_db").get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    foodList = documents.map { doc ->
                        Food(
                            food_name = doc.getString("food_name") ?: "",
                            food_desc = doc.getString("food_desc") ?: "",
                            calories = doc.getLong("calories")?.toInt() ?: 0,
                            carbohydrates = doc.getLong("carbohydrates")?.toInt() ?: 0,
                            fat = doc.getLong("fat")?.toInt() ?: 0,
                            fiber = doc.getLong("fiber")?.toInt() ?: 0,
                            proteins = doc.getLong("proteins")?.toInt() ?: 0,
                            serving_size = doc.getString("serving_size") ?: "",
                            goal_type = doc.getString("goal_type") ?: "",
                            meal_type = doc.getString("meal_type") ?: "",
                            allergens = doc.getString("allergens") ?: "",
                            image_url = doc.getString("image_url") ?: ""
                        )
                    }
                    updateRecyclerView()

                    val foodNames = foodList.map { it.food_name }
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
        foodAdapter = FoodAdapter(this, foodList, favoriteFoodNames) // Ensure to pass the favoriteFoodNames here
        recyclerViewFoods.adapter = foodAdapter
    }

    private fun showEmptyMessage() {
        emptyMessage.visibility = View.VISIBLE
        progressBar.visibility = View.GONE
    }

    private fun showErrorSnackbar(message: String) {
        Snackbar.make(findViewById(R.id.root_layout), message, Snackbar.LENGTH_LONG).show()
    }

    private fun filterFoodList(query: String) {
        if (query.isNotEmpty()) {
            val filteredList = foodList.filter { food ->
                food.food_name.contains(query, ignoreCase = true)
            }
            foodAdapter.updateList(filteredList)

            emptyMessage.visibility = if (filteredList.isEmpty()) View.VISIBLE else View.GONE
        } else {
            foodAdapter.updateList(foodList)
            emptyMessage.visibility = View.GONE
        }
    }

    private fun showHealthComplicationDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Health Advisory")
        builder.setMessage("You have reported a health complication. Please consult a healthcare professional for personalized meal plans.")
        builder.setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
        builder.show()
    }
}
