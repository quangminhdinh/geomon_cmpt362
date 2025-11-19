package geomon.myapplication.ui.battle

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import geomon.myapplication.R

class BattleLogAdapter(private var items: List<String>) :
    RecyclerView.Adapter<BattleLogAdapter.VH>() {
    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val tv: TextView = v.findViewById(R.id.tvLogLine)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_battle_log, parent, false)
        return VH(v)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.tv.text = items[position]
    }

    fun submit(list: List<String>) {
        items = list
        notifyDataSetChanged()
    }
}
