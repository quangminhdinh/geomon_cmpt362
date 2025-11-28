package com.example.myapplication.ui.pokedex

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.myapplication.R
import com.example.myapplication.battle.Monster
import com.example.myapplication.battle.Move
import com.example.myapplication.data.AuthManager
import com.example.myapplication.data.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MonsterInfoActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.monsterinfoactivity)


        findViewById<Button>(R.id.btnBack).setOnClickListener {
            finish()
        }


        val monsterId = intent.getStringExtra("monster_id")
        if (monsterId == null) {
            Log.e("MonsterInfo", "No monster_id passed to MonsterInfoActivity")
            finish()
            return
        }
        val index = intent.getIntExtra("monster_index", -1)

        if (index == null) {
            Log.e("MonsterInfo", "No monster_id passed to MonsterInfoActivity")
            finish()
            return
        }

        lifecycleScope.launch {
            val monster = withContext(Dispatchers.IO) {
                Monster.fetchById(monsterId)
            }

            if (monster == null) {
                Log.e("MonsterInfo", "Monster not found for id=$monsterId")
                finish()
            } else {
                bindMonster(monster)
            }
        }
        val userId = AuthManager.userId
        if (userId == null) {
            Log.e("PokedexActivity", "User not signed in")
            return
        }
        Log.e("PokedexActivity index", "User index changed$index")
        findViewById<Button>(R.id.setBattle).setOnClickListener {
            User.setUserActiveMonster(userId, index )
            finish()
        }
    }

    private suspend fun bindMonster(monster: Monster) {

        val nameText = findViewById<TextView>(R.id.monsterName)
        val levelInfoText = findViewById<TextView>(R.id.monsterLevelInfo)
        val typeText = findViewById<TextView>(R.id.monsterLevel)
        val spriteView = findViewById<ImageView>(R.id.rightImage)

        nameText.text = monster.name
        levelInfoText.text = "Level: ${monster.level}"

        val typeParts = listOfNotNull(monster.type1, monster.type2, monster.type3)


        val formattedTypes = typeParts
            .map { type -> type.lowercase().replaceFirstChar { it.uppercase() } }
            .joinToString("/")

        typeText.text = if (typeParts.isNotEmpty()) {
            "Type: $formattedTypes"
        } else {
            "Type: Unknown"
        }



        val imageName = monster.name.lowercase().replace(" ", "_")
        val resId = resources.getIdentifier(imageName, "drawable", packageName)
        if (resId != 0) {
            spriteView.setImageResource(resId)
        } else {
            spriteView.setImageResource(R.drawable.ic_launcher_foreground)
        }


        val hpValue = findViewById<TextView>(R.id.hpValue)
        val attackValue = findViewById<TextView>(R.id.attackValue)
        val defenseValue = findViewById<TextView>(R.id.defenseValue)
        val spAttackValue = findViewById<TextView>(R.id.specialAttackValue)
        val spDefenseValue = findViewById<TextView>(R.id.specialDefenseValue)
        val speedValue = findViewById<TextView>(R.id.speedValue)


        val currentHpInt = monster.currentHp.toInt()
        val maxHpInt = monster.maxHp.toInt()
        hpValue.text = "$currentHpInt/$maxHpInt"

        attackValue.text = monster.attack.toInt().toString()
        defenseValue.text = monster.defense.toInt().toString()
        spAttackValue.text = monster.specialAttack.toInt().toString()
        spDefenseValue.text = monster.specialDefense.toInt().toString()
        speedValue.text = monster.speed.toInt().toString()


        val moveRow1 = findViewById<View>(R.id.moveButton1)
        val moveRow2 = findViewById<View>(R.id.moveButton2)
        val moveRow3 = findViewById<View>(R.id.moveButton3)
        val moveRow4 = findViewById<View>(R.id.moveButton4)

        val moveLeft1 = findViewById<TextView>(R.id.moveLeftText)
        val moveRight1 = findViewById<TextView>(R.id.moveRightText)

        val moveLeft2 = findViewById<TextView>(R.id.moveLeft2Text)
        val moveRight2 = findViewById<TextView>(R.id.moveRight2Text)

        val moveLeft3 = findViewById<TextView>(R.id.moveLeft3Text)
        val moveRight3 = findViewById<TextView>(R.id.moveRight3Text)

        val moveLeft4 = findViewById<TextView>(R.id.moveLeft4Text)
        val moveRight4 = findViewById<TextView>(R.id.moveRight4Text)


        val chosenMove1 = Move.initializeByName(this@MonsterInfoActivity, monster.move1)
        val chosenMove2 = Move.initializeByName(this@MonsterInfoActivity, monster.move2)
        val chosenMove3 = Move.initializeByName(this@MonsterInfoActivity, monster.move3)
        val chosenMove4 = Move.initializeByName(this@MonsterInfoActivity, monster.move4)

        bindMoveRow(chosenMove1, moveRow1, moveLeft1, moveRight1)
        bindMoveRow(chosenMove2, moveRow2, moveLeft2, moveRight2)
        bindMoveRow(chosenMove3, moveRow3, moveLeft3, moveRight3)
        bindMoveRow(chosenMove4, moveRow4, moveLeft4, moveRight4)


        // moveRow1.setOnClickListener { /* open move details above  */ }
    }

    private fun bindMoveRow(
        move: Move?,
        row: View,
        leftText: TextView,
        rightText: TextView
    ) {
        if (move == null) {
            row.visibility = View.GONE
        } else {
            row.visibility = View.VISIBLE
            Log.d("MoveDebugging", """
            ---- MOVEs STATS ----
            ID: ${move.id}
            Name: ${move.name}
            Type1: ${move.type1}
            Type2: ${move.type2}
            Type3: ${move.type3}
            Base Damage: ${move.baseDamage}
            Category: ${move.category}
            Accuracy: ${move.accuracy}
            Priority: ${move.priority}
            PP Max: ${move.ppMax}
            Healing: ${move.healing}
            Other Effect: ${move.otherEffect}
            ----------------------
        """.trimIndent())

            leftText.text = move.type1 ?: "Normal"
            rightText.text = move.name
        }
    }
}
