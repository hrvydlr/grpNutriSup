package com.myapp.grpnutrisup.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.myapp.grpnutrisup.R
import com.myapp.grpnutrisup.models.Food

class FoodAdapter(private val context: Context, private var foodList: List<Food>) :
    RecyclerView.Adapter<FoodAdapter.FoodViewHolder>() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FoodViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.food_item, parent, false)
        return FoodViewHolder(view)
    }

    override fun onBindViewHolder(holder: FoodViewHolder, position: Int) {
        val food = foodList[position]

        // Populate food details
        holder.foodNameTextView.text = food.foodName
        holder.foodDescriptionTextView.text = food.description

        // Handle nutritional data safely
        holder.foodCaloriesTextView.text = "Calories: ${food.calories}"
        holder.foodCarbsTextView.text = "Carbohydrates: ${food.carbohydrates ?: "N/A"}"
        holder.foodProteinsTextView.text = "Proteins: ${food.proteins.takeIf { it != 0 } ?: "N/A"}"
        holder.foodFatsTextView.text = "Fats: ${food.fats.takeIf { it != 0 } ?: "N/A"}"
        holder.foodAllergenTextView.text = "Allergens: ${food.allergens ?: "None"}"

        // Toggle favorite button
        holder.favouriteButton.setOnClickListener {
            handleAddToFavourites(food, holder)
        }
    }

    override fun getItemCount(): Int = foodList.size

    // Method to update the food list and notify the adapter
    fun updateList(newList: List<Food>) {
        foodList = newList
        notifyDataSetChanged() // Notify that the data has changed
    }

    // Method to handle adding food to the favourites
    private fun handleAddToFavourites(food: Food, holder: FoodViewHolder) {
        val userEmail = auth.currentUser?.email
        if (userEmail != null) {
            val favouriteData = mapOf(
                "food_name" to food.foodName,
                "description" to food.description
            )

            db.collection("users").document(userEmail).collection("favourites")
                .add(favouriteData)
                .addOnSuccessListener {
                    Toast.makeText(context, "${food.foodName} added to favourites!", Toast.LENGTH_SHORT).show()
                    holder.favouriteButton.setImageResource(android.R.drawable.star_big_on) // Set to 'on' state
                }
                .addOnFailureListener { e ->
                    Toast.makeText(context, "Failed to add to favourites: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        } else {
            Toast.makeText(context, "User not logged in.", Toast.LENGTH_SHORT).show()
        }
    }

    // ViewHolder class to manage the item layout
    class FoodViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val foodNameTextView: TextView = itemView.findViewById(R.id.foodNameTextView)
        val foodDescriptionTextView: TextView = itemView.findViewById(R.id.foodDescriptionTextView)
        val foodCaloriesTextView: TextView = itemView.findViewById(R.id.foodCaloriesTextView)
        val foodCarbsTextView: TextView = itemView.findViewById(R.id.foodCarbsTextView)
        val foodProteinsTextView: TextView = itemView.findViewById(R.id.foodProteinsTextView)
        val foodFatsTextView: TextView = itemView.findViewById(R.id.foodFatsTextView)
        val foodAllergenTextView: TextView = itemView.findViewById(R.id.foodAllergenTextView)
        val favouriteButton: ImageButton = itemView.findViewById(R.id.favouriteButton)

        init {
            // Default to unselected state
            favouriteButton.setImageResource(android.R.drawable.star_big_off)
        }
    }
}
