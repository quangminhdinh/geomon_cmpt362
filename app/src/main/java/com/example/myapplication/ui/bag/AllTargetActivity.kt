package com.example.myapplication.ui.bag

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R
import com.example.myapplication.battle.Monster
import com.example.myapplication.data.AuthManager
import com.example.myapplication.data.FirebaseManager
import com.example.myapplication.data.User
import com.example.myapplication.items.Item
import com.example.myapplication.ui.pokedex.PokedexAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AllTargetActivity : AppCompatActivity() {

    private lateinit var rvPokedex: RecyclerView
    private lateinit var btnBack: Button
    private val monsterList = mutableListOf<Monster>()
    private lateinit var adapter: PokedexAdapter

    private var itemDisplayName: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        setContentView(R.layout.activity_pokedex)

        rvPokedex = findViewById(R.id.rvPokedex)
        btnBack = findViewById(R.id.btnBack)

        itemDisplayName = intent.getStringExtra("item_display_name") ?: ""

        adapter = PokedexAdapter(monsterList) { monster, index ->
            onMonsterClicked(monster)
        }
        rvPokedex.adapter = adapter

        btnBack.setOnClickListener { finish() }

        loadAllMonsters()
    }

    private fun loadAllMonsters() {
        val userId = AuthManager.userId
        if (userId == null) {
            Log.e("LevelUpTargetActivity", "User not signed in")
            return
        }

        lifecycleScope.launch(Dispatchers.IO) {
            val user = User.fetchById(userId)
            if (user == null) {
                Log.e("LevelUpTargetActivity", "Failed to fetch user")
                return@launch
            }

            val monsters = mutableListOf<Monster>()
            for (monsterId in user.monsterIds) {
                val monster = Monster.fetchById(monsterId)
                if (monster != null) {
                    monsters.add(monster)
                }
            }
            withContext(Dispatchers.Main) {
                monsterList.clear()
                monsterList.addAll(monsters)
                adapter.notifyDataSetChanged()
            }
        }
    }

    private fun onMonsterClicked(monster: Monster) {
        val userId = AuthManager.userId
        if (userId == null) {
            Log.e("HealTargetActivity", "User isn't signed in")
            return
        }

        lifecycleScope.launch(Dispatchers.IO) {
            val item = Item.searchByDisplayName(this@AllTargetActivity, itemDisplayName)
            if (item == null) {
                Log.e("HealTargetActivity", "Item isn't found in DB: $itemDisplayName")
                return@launch
            }
            if (item.effect == "candy1") {
                val amount = item.sideAffect
                monster.levelUp(amount)
                var originalLevel = monster.level - amount
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@AllTargetActivity,
                        "${monster.name} leveled up from ${originalLevel} to ${monster.level}",
                        Toast.LENGTH_LONG
                    ).show()
                }

                if (monster.id.isNotEmpty()) {
                    FirebaseManager.monstersRef.child(monster.id)
                        .setValue(monster.toMap())
                } else {
                    Log.e("LevelUpTargetActivity", "Monster has empty id")
                }

                User.removeByItemName(userId, item.displayName, 1)


                withContext(Dispatchers.Main) {
                    finish()
                }
            }

        }
    }
}
