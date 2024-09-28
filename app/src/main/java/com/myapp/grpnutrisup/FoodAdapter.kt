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
        holder.foodNameTextView.text = food.foodName // Make sure the property is `name`
        holder.foodDescriptionTextView.text = food.description

        // Handle favorite button click
        holder.favouriteButton.setOnClickListener {
            addToFavourites(food)
        }
    }

    override fun getItemCount(): Int {
        return foodList.size
    }

    private fun addToFavourites(food: Food) {
        val userEmail = auth.currentUser?.email
        if (userEmail != null) {
            // Create a map to hold the favorite food data
            val favouriteData = mapOf(
                "food_name" to food.foodName, // Adjust to match your Food class property
                "description" to food.description
            )

            // Add to Firestore under the user's favourites collection
            db.collection("users").document(userEmail).collection("favourites").add(favouriteData)
                .addOnSuccessListener {
                    Toast.makeText(context, "${food.foodName} added to favourites!", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(context, "Failed to add favourite: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        } else {
            Toast.makeText(context, "User not logged in", Toast.LENGTH_SHORT).show()
        }
    }

    class FoodViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val foodNameTextView: TextView = itemView.findViewById(R.id.foodNameTextView)
        val foodDescriptionTextView: TextView = itemView.findViewById(R.id.foodDescriptionTextView)
        val favouriteButton: ImageButton = itemView.findViewById(R.id.favouriteButton)
    }

    // Update the food list when filtered results are available
    fun updateList(newFoodList: List<Food>) {
        foodList = newFoodList
        notifyDataSetChanged()
    }
}
