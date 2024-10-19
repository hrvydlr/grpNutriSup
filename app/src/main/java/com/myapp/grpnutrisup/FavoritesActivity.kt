package com.myapp.grpnutrisup

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.myapp.grpnutrisup.adapters.FavoritesAdapter
import com.myapp.grpnutrisup.models.Food

class FavoritesActivity : AppCompatActivity() {

    private lateinit var favoritesRecyclerView: RecyclerView
    private lateinit var favoritesAdapter: FavoritesAdapter

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_favorites)

        // Initialize RecyclerView and Adapter
        favoritesRecyclerView = findViewById(R.id.favorites_recycler_view)
        favoritesRecyclerView.layoutManager = LinearLayoutManager(this)

        favoritesAdapter = FavoritesAdapter(listOf()) // Empty list initially
        favoritesRecyclerView.adapter = favoritesAdapter

        // Initialize Firebase authentication and Firestore
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        // Fetch favorite foods for the current user
        fetchUserFavorites()
    }

    // Fetch favorite foods from the user's Firestore document
    private fun fetchUserFavorites() {
        val currentUser = auth.currentUser
        currentUser?.let { user ->
            val userEmail = user.email
            if (userEmail != null) {
                val userRef = firestore.collection("users").document(userEmail)

                // Fetch the user's document which contains the 'favoriteFoods' array
                userRef.get()
                    .addOnSuccessListener { document ->
                        if (document.exists()) {
                            Log.d("FavoritesActivity", "User document found: ${document.data}")
                            val favoriteFoods = document.get("favoriteFoods") as? List<String>
                            if (favoriteFoods != null && favoriteFoods.isNotEmpty()) {
                                fetchFavoriteFoodDetails(favoriteFoods)
                            } else {
                                Log.d("FavoritesActivity", "No favorite foods found.")
                                updateUI(emptyList())  // No favorite foods
                            }
                        } else {
                            Log.e("FavoritesActivity", "No such document for user")
                        }
                    }
                    .addOnFailureListener { e ->
                        Log.e("FavoritesActivity", "Error fetching user document", e)
                    }
            }
        } ?: Log.e("FavoritesActivity", "No current user")
    }

    // Fetch food details for the favorite food items
    private fun fetchFavoriteFoodDetails(favoriteFoods: List<String>) {
        if (favoriteFoods.isEmpty()) {
            updateUI(emptyList())  // No favorites to fetch
            return
        }

        // Log the food names from Firestore document to debug
        Log.d("FavoritesActivity", "Fetching food details for: $favoriteFoods")

        // Assuming food names are stored with exact casing and no extra spaces
        // Query Firestore where the food_name exactly matches those in favoriteFoods
        firestore.collection("food_db")
            .whereIn("food_name", favoriteFoods)  // Keep the names as is
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    val favoriteFoodItems = documents.mapNotNull { document ->
                        document.toObject(Food::class.java)  // Map Firestore document to Food object
                    }
                    updateUI(favoriteFoodItems)
                } else {
                    Log.d("FavoritesActivity", "No matching foods found in the foods collection.")
                    updateUI(emptyList())
                }
            }
            .addOnFailureListener { e ->
                Log.e("FavoritesActivity", "Error fetching favorite foods", e)
            }
    }

    // Update the RecyclerView with the favorite food items
    private fun updateUI(favoriteFoods: List<Food>) {
        favoritesAdapter.updateData(favoriteFoods)
    }
}
