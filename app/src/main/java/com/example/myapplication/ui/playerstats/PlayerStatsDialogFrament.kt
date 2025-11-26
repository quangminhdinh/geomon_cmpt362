package com.example.myapplication.ui.playerstats

import android.os.Bundle
import android.view.*
import android.widget.TextView
import android.widget.ImageView
import android.widget.Button
import androidx.fragment.app.DialogFragment
import com.example.myapplication.R
import com.example.myapplication.data.AuthManager
import com.example.myapplication.data.User
import com.example.myapplication.battle.Monster
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch


class PlayerStatsDialogFragment : DialogFragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, saved: Bundle?): View {
        return inflater.inflate(R.layout.dialog_player_stats, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val tvPlayerName   = view.findViewById<TextView>(R.id.tvPlayerName)
        val tvPlayerLevel  = view.findViewById<TextView>(R.id.tvPlayerLevel)
        val tvDefaultMon   = view.findViewById<TextView>(R.id.tvDefaultMon)
        val btnClose       = view.findViewById<Button>(R.id.btnCloseStats)

        btnClose.setOnClickListener { dismiss() }

        // Load user + first monster (default mon)
        val userId = AuthManager.userId ?: return
        lifecycleScope.launch {
            val user = User.fetchById(userId) ?: return@launch
            tvPlayerName.text = user.displayName.ifBlank { "Player" }
            tvPlayerLevel.text = "Lv. ${1 + (user.monsterIds.size / 3)}" // placeholder level

            user.firstMonsterId?.let { mid ->
                val mon = Monster.fetchById(mid)
                if (mon != null) {
                    tvDefaultMon.text = "${mon.name} (Lv. ${mon.level})"
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    companion object { const val TAG = "PlayerStatsDialog" }
}
