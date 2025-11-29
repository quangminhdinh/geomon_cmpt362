package com.example.myapplication.ui.bag

import android.os.Bundle
import android.util.Log
import android.widget.Button
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

class HealTargetActivity : AppCompatActivity() {

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

        loadDamagedMonsters()
    }

    // Sorts Only monsters with low hp
    private fun loadDamagedMonsters() {
        val userId = AuthManager.userId
        if (userId == null) {
            Log.e("HealTargetActivity", "User not signed in")
            return
        }

        lifecycleScope.launch(Dispatchers.IO) {
            val user = User.fetchById(userId)
            if (user == null) {
                Log.e("HealTargetActivity", "Failed to fetch user")
                return@launch
            }

            val monsters = mutableListOf<Monster>()
            for (monsterId in user.monsterIds) {
                val monster = Monster.fetchById(monsterId)
                if (monster != null && monster.currentHp != monster.maxHp) {
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
//Tapping a monster results in the item consumed, monster healed firebase sync
    private fun onMonsterClicked(monster: Monster) {
        val userId = AuthManager.userId
        if (userId == null) {
            Log.e("HealTargetActivity", "User isn't signed in")
            return
        }

        lifecycleScope.launch(Dispatchers.IO) {
            val item = Item.searchByDisplayName(this@HealTargetActivity, itemDisplayName)
            if (item == null) {
                Log.e("HealTargetActivity", "Item isn't found in DB: $itemDisplayName")
                return@launch
            }

            val amount = item.returnAffect() ?: 0
            if (amount <= 0) {
                Log.e("HealTargetActivity", "Item has no effect: ${item.effect}")
                return@launch
            }


            if (monster.currentHp != monster.maxHp) {
                val heal = amount.toFloat()
                monster.healDamage(heal)


                if (monster.id.isNotEmpty()) {
                    val updates = mapOf(
                        "currentHp" to monster.currentHp,
                        "isFainted" to monster.isFainted
                    )
                    FirebaseManager.monstersRef.child(monster.id).updateChildren(updates)
                } else {
                    Log.e("HealTargetActivity", "Monster has empty id")
                }
            }

            User.removeByItemName(userId, item.displayName, 1)

            withContext(Dispatchers.Main) {
                finish()
            }
        }
    }
}
