package com.group14.geomon.ui.playerstats

import android.os.Bundle
import android.view.*
import android.widget.TextView
import android.widget.ImageView
import android.widget.Button
import androidx.fragment.app.DialogFragment
import com.group14.geomon.R
import com.group14.geomon.data.AuthManager
import com.group14.geomon.data.User
import com.group14.geomon.battle.Monster
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import com.group14.geomon.ui.home.ChangeAvatarDialogFragment
import com.bumptech.glide.Glide


class PlayerStatsDialogFragment : DialogFragment(), ChangeAvatarDialogFragment.OnAvatarUpdatedListener {

    private lateinit var imgPlayerAvatar: ImageView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, saved: Bundle?): View {
        return inflater.inflate(R.layout.dialog_player_stats, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val tvPlayerName   = view.findViewById<TextView>(R.id.tvPlayerName)
        val tvPlayerLevel  = view.findViewById<TextView>(R.id.tvPlayerLevel)
        val tvDefaultMon   = view.findViewById<TextView>(R.id.tvDefaultMon)
        imgPlayerAvatar = view.findViewById(R.id.imgPlayerAvatar)
        val btnChangeAvatar = view.findViewById<Button>(R.id.btnChangeAvatar)
        val btnClose       = view.findViewById<Button>(R.id.btnCloseStats)

        btnClose.setOnClickListener { dismiss() }

        btnChangeAvatar.setOnClickListener {
            ChangeAvatarDialogFragment().show(childFragmentManager, ChangeAvatarDialogFragment.TAG)
        }

        // Load user + first monster (default mon)
        val userId = AuthManager.userId ?: return
        lifecycleScope.launch {
            val user = User.fetchById(userId) ?: return@launch
            tvPlayerName.text = user.displayName.ifBlank { "Player" }
            tvPlayerLevel.text = "Lv. ${1 + (user.monsterIds.size / 3)}" // placeholder level

            // Load avatar
            if (user.avatarUrl.isNotBlank()) {
                Glide.with(this@PlayerStatsDialogFragment)
                    .load(user.avatarUrl)
                    .circleCrop()
                    .into(imgPlayerAvatar)
            }

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

    override fun onAvatarUpdated(avatarUrl: String) {
        Glide.with(this)
            .load(avatarUrl)
            .circleCrop()
            .into(imgPlayerAvatar)
    }

    companion object { const val TAG = "PlayerStatsDialog" }
}
