package com.myapp.grpnutrisup

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import com.myapp.grpnutrisup.adapters.FoodAdapter
import com.myapp.grpnutrisup.models.Food

class FoodSearchActivity : AppCompatActivity() {

    private lateinit var autoCompleteTextView: AutoCompleteTextView
    private lateinit var recyclerViewFoods: RecyclerView
    private lateinit var foodAdapter: FoodAdapter
    private lateinit var foodList: List<Food>
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_food_search)

        // Initialize Firestore
        db = FirebaseFirestore.getInstance()

        // Initialize UI components
        autoCompleteTextView = findViewById(R.id.autoCompleteTextView)
        recyclerViewFoods = findViewById(R.id.recyclerViewFoods)
        recyclerViewFoods.layoutManager = LinearLayoutManager(this)

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
        // Fetch food items from Firestore
        db.collection("foods").get()
            .addOnSuccessListener { documents ->
                foodList = documents.map { doc ->
                    Food(
                        foodName = doc.getString("food_name") ?: "",
                        description = doc.getString("desc") ?: ""
                    )
                }
                // Correctly instantiate FoodAdapter with the right parameters
                foodAdapter = FoodAdapter(this, foodList) // <-- Here is the fix
                recyclerViewFoods.adapter = foodAdapter

                // Set up the AutoCompleteTextView with food names
                val foodNames = foodList.map { it.foodName }
                val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, foodNames)
                autoCompleteTextView.setAdapter(adapter)
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error fetching food data: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun filterFoodList(query: String) {
        val filteredList = foodList.filter { food ->
            food.foodName.contains(query, ignoreCase = true)
        }
        foodAdapter.updateList(filteredList)
    }
}
