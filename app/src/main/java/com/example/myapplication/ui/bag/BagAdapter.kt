package com.example.myapplication.ui.bag

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R

class BagAdapter(
    private val items: List<BagItem>,
    private val onItemClick: (BagItem) -> Unit
) : RecyclerView.Adapter<BagAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imgItem: ImageView = view.findViewById(R.id.imgItem)
        val tvItemName: TextView = view.findViewById(R.id.tvItemName)
        val tvItemCount: TextView = view.findViewById(R.id.tvItemCount)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_bag_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val bagItem = items[position]
        holder.tvItemName.text = bagItem.displayName
        holder.tvItemCount.text = "x${bagItem.count}"

        val context = holder.itemView.context
        val iconName = bagItem.displayName.lowercase().replace(" ", "_")
        val resId = context.resources.getIdentifier(iconName, "drawable", context.packageName)
        if (resId != 0) {
            holder.imgItem.setImageResource(resId)
        } else {
            holder.imgItem.setImageResource(R.drawable.ic_launcher_foreground)
        }

        holder.itemView.setOnClickListener {
            onItemClick(bagItem)
        }
    }

    override fun getItemCount() = items.size
}
