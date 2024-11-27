package com.myapp.grpnutrisup.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.myapp.grpnutrisup.R
import com.myapp.grpnutrisup.models.ActivityLog
import java.text.SimpleDateFormat
import java.util.*

class ActivityLogAdapter(private val activities: List<ActivityLog>) :
    RecyclerView.Adapter<ActivityLogAdapter.ActivityLogViewHolder>() {

    // ViewHolder class for managing views
    class ActivityLogViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val activityType: TextView = view.findViewById(R.id.activity_type)
        val duration: TextView = view.findViewById(R.id.duration)
        val caloriesBurned: TextView = view.findViewById(R.id.calories_burned)
        val timestamp: TextView = view.findViewById(R.id.timestamp)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ActivityLogViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.activity_log_item, parent, false)
        return ActivityLogViewHolder(view)
    }

    override fun onBindViewHolder(holder: ActivityLogViewHolder, position: Int) {
        val activity = activities[position]

        // Safely populate the view with data
        holder.activityType.text = activity.activityType ?: "Unknown Activity"
        holder.duration.text = "Duration: ${activity.duration ?: 0} mins"
        holder.caloriesBurned.text = "Calories Burned: ${activity.caloriesBurned ?: 0}"
        holder.timestamp.text = formatTimestamp(activity.timestamp)
    }

    override fun getItemCount(): Int = activities.size

    // Helper function to format the timestamp
    private fun formatTimestamp(timestamp: Long?): String {
        return if (timestamp != null && timestamp > 0) {
            val date = Date(timestamp)
            val dateFormat = SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault())
            dateFormat.format(date)
        } else {
            "Unknown Date"
        }
    }
}
