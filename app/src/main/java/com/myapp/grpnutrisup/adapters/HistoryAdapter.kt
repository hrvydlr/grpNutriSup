package com.myapp.grpnutrisup.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.myapp.grpnutrisup.R
import com.myapp.grpnutrisup.models.DailyCalories

class HistoryAdapter(private val historyList: List<DailyCalories>) :
    RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_daily_calories, parent, false)
        return HistoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        val dailyCalories = historyList[position]
        holder.dateTextView.text = dailyCalories.date
        holder.caloriesTextView.text = "Calories: ${dailyCalories.calories}"
    }

    override fun getItemCount() = historyList.size

    inner class HistoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val dateTextView: TextView = itemView.findViewById(R.id.dateTextView)
        val caloriesTextView: TextView = itemView.findViewById(R.id.caloriesTextView)
    }
}
