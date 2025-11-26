package com.example.myapplication.ui.pokedex

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R
import com.example.myapplication.battle.Monster
import com.example.myapplication.data.AuthManager
import com.example.myapplication.data.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PokedexActivity : AppCompatActivity() {

    private lateinit var rvPokedex: RecyclerView
    private lateinit var btnBack: Button
    private val monsterList = mutableListOf<Monster>()
    private lateinit var adapter: PokedexAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        setContentView(R.layout.activity_pokedex)

        rvPokedex = findViewById(R.id.rvPokedex)
        btnBack = findViewById(R.id.btnBack)

        adapter = PokedexAdapter(monsterList) { monster, index ->
            val intent = Intent(this, MonsterInfoActivity::class.java)
            intent.putExtra("monster_id", monster.id)
            intent.putExtra("monster_index", index)
            startActivity(intent)
        }
        
        rvPokedex.adapter = adapter

        btnBack.setOnClickListener { finish() }

        loadUserMonsters()
    }

    private fun loadUserMonsters() {
        val userId = AuthManager.userId
        if (userId == null) {
            Log.e("PokedexActivity", "User not signed in")
            return
        }

        lifecycleScope.launch(Dispatchers.IO) {
            val user = User.fetchById(userId)
            if (user == null) {
                Log.e("PokedexActivity", "Failed to fetch user")
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
                Log.d("PokedexActivity", "Loaded ${monsters.size} monsters")
            }
        }
    }
}
