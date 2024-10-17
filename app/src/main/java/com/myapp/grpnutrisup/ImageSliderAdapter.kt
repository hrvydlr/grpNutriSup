import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.myapp.grpnutrisup.R

class ImageSliderAdapter(private val images: List<Int>) : RecyclerView.Adapter<ImageSliderAdapter.ImageViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.image_slider_item, parent, false)
        return ImageViewHolder(view)
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        Log.d("ImageSliderAdapter", "Binding image at position: $position") // Log binding action
        holder.bind(images[position])
    }

    override fun getItemCount(): Int {
        Log.d("ImageSliderAdapter", "Total images: ${images.size}") // Log item count
        return images.size
    }

    class ImageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val imageView: ImageView = itemView.findViewById(R.id.slider_image)

        fun bind(imageRes: Int) {
            imageView.setImageResource(imageRes)  // Set the image dynamically
        }
    }
}
