package com.myapp.grpnutrisup.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.myapp.grpnutrisup.R
import com.myapp.grpnutrisup.models.Meal
import java.text.SimpleDateFormat
import java.util.*

class MealHistoryAdapter(
    private val context: Context,
    private val mealList: List<Meal>
) : RecyclerView.Adapter<MealHistoryAdapter.MealViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MealViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_meal_history, parent, false)
        return MealViewHolder(view)
    }

    override fun onBindViewHolder(holder: MealViewHolder, position: Int) {
        val meal = mealList[position]

        holder.apply {
            foodNameTextView.text = meal.foodName
            caloriesTextView.text = "Calories: ${meal.calories}"
            carbsTextView.text = "Carbohydrates: ${meal.carbohydrates}"
            proteinsTextView.text = "Proteins: ${meal.proteins}"
            fatsTextView.text = "Fats: ${meal.fats}"
            allergensTextView.text = "Allergens: ${meal.allergens.ifEmpty { "None" }}"
            timestampTextView.text = meal.timestamp?.let { formatTimestamp(it) } ?: "N/A"
        }
    }

    override fun getItemCount() = mealList.size

    private fun formatTimestamp(timestamp: com.google.firebase.Timestamp): String {
        val date = timestamp.toDate()
        val sdf = SimpleDateFormat("MMM dd, yyyy - HH:mm", Locale.getDefault())
        return sdf.format(date)
    }

    class MealViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val foodNameTextView: TextView = itemView.findViewById(R.id.foodNameTextView)
        val caloriesTextView: TextView = itemView.findViewById(R.id.caloriesTextView)
        val carbsTextView: TextView = itemView.findViewById(R.id.carbsTextView)
        val proteinsTextView: TextView = itemView.findViewById(R.id.proteinsTextView)
        val fatsTextView: TextView = itemView.findViewById(R.id.fatsTextView)
        val allergensTextView: TextView = itemView.findViewById(R.id.allergensTextView)
        val timestampTextView: TextView = itemView.findViewById(R.id.timestampTextView)
    }
}
