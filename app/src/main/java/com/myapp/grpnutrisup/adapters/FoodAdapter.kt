package com.myapp.grpnutrisup.adapters

import android.app.AlertDialog
import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.myapp.grpnutrisup.R
import com.myapp.grpnutrisup.models.Food

class FoodAdapter(
    private val context: Context,
    private var foodList: List<Food>,
    private var favoriteFoodNames: MutableList<String> // MutableList for easier updates
) : RecyclerView.Adapter<FoodAdapter.FoodViewHolder>() {

    // Initializing Firestore and FirebaseAuth once, reducing repeated initializations
    private val db: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FoodViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.food_item, parent, false)
        return FoodViewHolder(view)
    }

    override fun onBindViewHolder(holder: FoodViewHolder, position: Int) {
        val food = foodList[position]

        // Populate food details efficiently using string templates
        holder.apply {
            foodNameTextView.text = food.food_name
            foodDescriptionTextView.text = food.food_desc
            foodCaloriesTextView.text = "Calories: ${food.calories}"
            foodCarbsTextView.text = "Carbohydrates: ${food.carbohydrates?.takeIf { it > 0 } ?: "N/A"}"
            foodProteinsTextView.text = "Proteins: ${food.proteins?.takeIf { it > 0 } ?: "N/A"}"
            foodFatsTextView.text = "Fats: ${food.fat?.takeIf { it > 0 } ?: "N/A"}"
            foodAllergenTextView.text = "Allergens: ${food.allergens.ifEmpty { "None" }}"
        }

        // Efficiently handle favorite state using `contains`
        holder.favoriteButton.setImageResource(
            if (favoriteFoodNames.contains(food.food_name)) {
                android.R.drawable.star_big_on // Favorited icon
            } else {
                android.R.drawable.star_big_off // Not favorited icon
            }
        )

        // Log the image path for debugging
        Log.d("FoodAdapter", "Fetching image for food: ${food.food_name}, Storage Path: ${food.image_url}")

        // Efficiently fetch and load the image from Firebase Storage
        fetchImageUrl(food.image_url) { imageUrl ->
            Glide.with(context)
                .load(imageUrl)
                .placeholder(R.drawable.placeholder_image)
                .error(R.drawable.error_image)
                .into(holder.foodImageView)
        }

        // Item click listener for showing food details in a dialog
        holder.itemView.setOnClickListener {
            showFoodDetailsDialog(food)
        }

        // Favorite button handling
        holder.favoriteButton.setOnClickListener {
            auth.currentUser?.email?.let { userEmail ->
                toggleFavorite(userEmail, food)
            } ?: run {
                Toast.makeText(context, "Please log in to manage favorites", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun getItemCount() = foodList.size

    // Update food list efficiently
    fun updateList(newList: List<Food>) {
        foodList = newList
        notifyDataSetChanged()
    }

    // Update favorite list efficiently
    fun updateFavorites(newFavorites: List<String>) {
        favoriteFoodNames.apply {
            clear()
            addAll(newFavorites)
        }
        notifyDataSetChanged()
    }

    // Optimized image fetching method using Firebase Storage
    private fun fetchImageUrl(storagePath: String, callback: (String) -> Unit) {
        if (storagePath.isNotEmpty()) {
            FirebaseStorage.getInstance().getReference(storagePath)
                .downloadUrl
                .addOnSuccessListener { uri -> callback(uri.toString()) }
                .addOnFailureListener {
                    Log.e("FoodAdapter", "Error fetching image URL: $it")
                    callback("")
                }
        } else {
            callback("")
        }
    }

    // Method to toggle favorites more efficiently
    private fun toggleFavorite(userEmail: String, food: Food) {
        val userFavoritesRef = db.collection("users").document(userEmail)

        userFavoritesRef.get().addOnSuccessListener { documentSnapshot ->
            val favoriteFoods = documentSnapshot.get("favoriteFoods") as? MutableList<String> ?: mutableListOf()

            if (favoriteFoods.contains(food.food_name)) {
                // Remove from favorites
                favoriteFoods.remove(food.food_name)
                userFavoritesRef.update("favoriteFoods", favoriteFoods)
                    .addOnSuccessListener {
                        favoriteFoodNames.remove(food.food_name)
                        notifyItemChanged(foodList.indexOf(food))
                        Toast.makeText(context, "${food.food_name} removed from favorites", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener {
                        Toast.makeText(context, "Failed to remove favorite", Toast.LENGTH_SHORT).show()
                    }
            } else {
                // Add to favorites
                favoriteFoods.add(food.food_name)
                userFavoritesRef.update("favoriteFoods", favoriteFoods)
                    .addOnSuccessListener {
                        favoriteFoodNames.add(food.food_name)
                        notifyItemChanged(foodList.indexOf(food))
                        Toast.makeText(context, "${food.food_name} added to favorites", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener {
                        Toast.makeText(context, "Failed to add favorite", Toast.LENGTH_SHORT).show()
                    }
            }
        }.addOnFailureListener {
            Toast.makeText(context, "Failed to retrieve user favorites", Toast.LENGTH_SHORT).show()
        }
    }

    // Show food details in a dialog
    private fun showFoodDetailsDialog(food: Food) {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_food_details, null)
        AlertDialog.Builder(context).apply {
            setView(dialogView)

            // Initialize dialog components
            val foodImageView = dialogView.findViewById<ImageView>(R.id.dialogFoodImageView)
            val foodNameTextView = dialogView.findViewById<TextView>(R.id.dialogFoodNameTextView)
            val foodDescriptionTextView = dialogView.findViewById<TextView>(R.id.dialogFoodDescriptionTextView)
            val foodCaloriesTextView = dialogView.findViewById<TextView>(R.id.dialogFoodCaloriesTextView)
            val foodCarbsTextView = dialogView.findViewById<TextView>(R.id.dialogFoodCarbsTextView)
            val foodProteinsTextView = dialogView.findViewById<TextView>(R.id.dialogFoodProteinsTextView)
            val foodFatsTextView = dialogView.findViewById<TextView>(R.id.dialogFoodFatsTextView)
            val foodAllergenTextView = dialogView.findViewById<TextView>(R.id.dialogFoodAllergenTextView)
            val foodServingSize = dialogView.findViewById<TextView>(R.id.dialogFoodServingSizeTextView)
            val eatenButton = dialogView.findViewById<Button>(R.id.dialogFoodEatenButton)

            // Set dialog values
            foodNameTextView.text = food.food_name
            foodDescriptionTextView.text = food.food_desc
            foodCaloriesTextView.text = "Calories: ${food.calories}"
            foodCarbsTextView.text = "Carbohydrates: ${food.carbohydrates?.takeIf { it > 0 } ?: "N/A"}"
            foodProteinsTextView.text = "Proteins: ${food.proteins?.takeIf { it > 0 } ?: "N/A"}"
            foodFatsTextView.text = "Fats: ${food.fat?.takeIf { it > 0 } ?: "N/A"}"
            foodAllergenTextView.text = "Allergens: ${food.allergens.ifEmpty { "None" }}"
            foodServingSize.text = "Serving Size: ${food.serving_size.ifEmpty { "N/A" }}"

            // Load image
            fetchImageUrl(food.image_url) { imageUrl ->
                Glide.with(context).load(imageUrl)
                    .placeholder(R.drawable.placeholder_image)
                    .error(R.drawable.error_image)
                    .into(foodImageView)
            }

            // Handle 'Eaten' button click
            eatenButton.setOnClickListener {
                auth.currentUser?.email?.let { userEmail ->
                    updateIntakes(userEmail, food.calories, food.proteins ?: 0, food.fat ?: 0)
                } ?: run {
                    Toast.makeText(context, "Please log in to track your intake", Toast.LENGTH_SHORT).show()
                }
            }

            // Build and show the dialog
            setPositiveButton("Close", null).create().show()
        }
    }

    // Update user's calorie, protein, and fat intake
    private fun updateIntakes(userEmail: String, calories: Int, proteins: Int, fats: Int) {
        val userRef = db.collection("users").document(userEmail)

        userRef.get().addOnSuccessListener { document ->
            if (document.exists()) {
                val updatedCalories = (document.get("calorieIntake") as? Long ?: 0) + calories
                val updatedProteins = (document.get("proteinIntake") as? Long ?: 0) + proteins
                val updatedFats = (document.get("fatIntake") as? Long ?: 0) + fats

                // Batch updating fields for consistency
                userRef.update(mapOf(
                    "calorieIntake" to updatedCalories,
                    "proteinIntake" to updatedProteins,
                    "fatIntake" to updatedFats
                )).addOnSuccessListener {
                    Toast.makeText(context, "Intake updated!", Toast.LENGTH_SHORT).show()
                }.addOnFailureListener {
                    Toast.makeText(context, "Failed to update intake", Toast.LENGTH_SHORT).show()
                }
            }
        }.addOnFailureListener {
            Toast.makeText(context, "Failed to retrieve user data", Toast.LENGTH_SHORT).show()
        }
    }

    // ViewHolder pattern optimized
    class FoodViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val foodNameTextView: TextView = itemView.findViewById(R.id.foodNameTextView)
        val foodDescriptionTextView: TextView = itemView.findViewById(R.id.foodDescriptionTextView)
        val foodCaloriesTextView: TextView = itemView.findViewById(R.id.foodCaloriesTextView)
        val foodCarbsTextView: TextView = itemView.findViewById(R.id.foodCarbsTextView)
        val foodProteinsTextView: TextView = itemView.findViewById(R.id.foodProteinsTextView)
        val foodFatsTextView: TextView = itemView.findViewById(R.id.foodFatsTextView)
        val foodAllergenTextView: TextView = itemView.findViewById(R.id.foodAllergenTextView)
        val foodImageView: ImageView = itemView.findViewById(R.id.foodImageView)
        val favoriteButton: ImageButton = itemView.findViewById(R.id.favouriteButton)
    }
}
