package com.example.myapplication.ui.pokedex

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R
import com.example.myapplication.battle.Monster

class PokedexAdapter(
    private val monsters: List<Monster>,
    private val onItemClick: (Monster, Int) -> Unit
) : RecyclerView.Adapter<PokedexAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imgMonster: ImageView = view.findViewById(R.id.imgMonster)
        val tvMonsterName: TextView = view.findViewById(R.id.tvMonsterName)
        val tvMonsterLevel: TextView = view.findViewById(R.id.tvMonsterLevel)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_pokedex_monster, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val monster = monsters[position]
        holder.tvMonsterName.text = monster.name
        holder.tvMonsterLevel.text = "Lv. ${monster.level}"

        val context = holder.itemView.context
        val spriteName = monster.name.lowercase().replace(" ", "_")
        val resourceId = context.resources.getIdentifier(spriteName, "drawable", context.packageName)
        holder.imgMonster.setImageResource(
            if (resourceId != 0) resourceId else R.drawable.ic_launcher_foreground
        )

        holder.itemView.setOnClickListener {
            onItemClick(monster, position)
        }
    }

    override fun getItemCount() = monsters.size
}
