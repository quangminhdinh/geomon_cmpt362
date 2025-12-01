package com.example.myapplication.ui.bag

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R
import com.example.myapplication.data.AuthManager
import com.example.myapplication.data.User
import com.example.myapplication.items.Item
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class BagItem(
    val displayName: String,
    val count: Int
)

class BagActivity : AppCompatActivity() {

    private lateinit var rvBag: RecyclerView
    private lateinit var btnBack: Button
    private lateinit var adapter: BagAdapter
    private val bagItems = mutableListOf<BagItem>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        setContentView(R.layout.activity_bag)

        rvBag = findViewById(R.id.rvBag)
        btnBack = findViewById(R.id.btnBack)

        adapter = BagAdapter(bagItems) { clickedItem ->
            onItemClicked(clickedItem)
        }
        rvBag.adapter = adapter

        btnBack.setOnClickListener { finish() }

        loadUserBag()
    }

    private fun loadUserBag() {
        val userId = AuthManager.userId
        if (userId == null) {
            Log.e("BagActivity", "User Id error")
            return
        }

        lifecycleScope.launch(Dispatchers.IO) {
            val user = User.fetchById(userId)
            if (user == null) {
                Log.e("BagActivity", "Failed to fetch user")
                return@launch
            }

            val list = user.bag.map { (name, count) ->
                BagItem(name, count)
            }.sortedBy { it.displayName }

            withContext(Dispatchers.Main) {
                bagItems.clear()
                bagItems.addAll(list)
                adapter.notifyDataSetChanged()
            }
        }
    }

    private fun onItemClicked(bagItem: BagItem) {
        lifecycleScope.launch(Dispatchers.IO) {
            val item = Item.searchByDisplayName(this@BagActivity, bagItem.displayName)
            if (item == null) {
                Log.e("BagActivity", "Item not found in DB: ${bagItem.displayName}")
                return@launch
            }

            when (item.effect) {
                "healing" -> openHealTargetSelection(item)
                "candy1" -> openLevelUpSelection(item)
                else -> {
                    //add rare candies for leveling?
                }
            }
        }
    }

    private suspend fun openHealTargetSelection(item: Item) {
        withContext(Dispatchers.Main) {
            val intent = Intent(this@BagActivity, HealTargetActivity::class.java)
            intent.putExtra("item_display_name", item.displayName)
            startActivity(intent)
        }
    }

    private suspend fun openLevelUpSelection(item: Item) {
        withContext(Dispatchers.Main) {
            val intent = Intent(this@BagActivity, AllTargetActivity::class.java)
            intent.putExtra("item_display_name", item.displayName)
            startActivity(intent)
        }
    }
    override fun onResume() {
        super.onResume()
        loadUserBag()
    }
}
