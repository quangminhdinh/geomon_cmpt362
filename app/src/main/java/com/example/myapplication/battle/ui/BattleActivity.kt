package com.example.myapplication.battle.ui

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

import com.example.myapplication.battle.Monster
import com.example.myapplication.battle.Move
import com.example.myapplication.R
import com.example.myapplication.battle.damageCalc
import com.example.myapplication.data.AuthManager
import com.example.myapplication.data.User
import com.example.myapplication.data.FirebaseManager
import android.widget.LinearLayout
import android.widget.Toast
import com.example.myapplication.data.BattleState
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener


class BattleActivity : ComponentActivity() {

    private lateinit var player: Monster
    private lateinit var opponent: Monster
    private var isPvP: Boolean = false
    private var opponentUserId: String? = null
    private var battleId: String? = null
    private var battleStateListener: ValueEventListener? = null
    private var currentUserId: String? = null
    private var isMyTurn: Boolean = false

    // UI references
    private lateinit var tvPlayerName: TextView
    private lateinit var tvOpponentName: TextView

    private lateinit var hpPlayer: ProgressBar
    private lateinit var hpOpponent: ProgressBar

    private lateinit var tvAnnouncement: TextView

    private lateinit var imgPlayer: ImageView
    private lateinit var imgOpponent: ImageView

    private lateinit var logList: RecyclerView

    private lateinit var btnMove1: LinearLayout
    private lateinit var btnMove2: LinearLayout
    private lateinit var btnMove3: LinearLayout
    private lateinit var btnMove4: LinearLayout

    private lateinit var btnMove1Text: TextView


    private lateinit var btnMove2Text: TextView
    private lateinit var btnMove3Text: TextView
    private lateinit var btnMove4Text: TextView
    private lateinit var btnMove1Type: TextView
    private lateinit var btnMove2Type: TextView
    private lateinit var btnMove3Type: TextView
    private lateinit var btnMove4Type: TextView
    private lateinit var btnRun: Button
    private lateinit var btnCapture: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_battle)

        // Initialize UI references
        tvPlayerName    = findViewById(R.id.tvPlayerName)
        tvOpponentName  = findViewById(R.id.tvOpponentName)

        hpPlayer        = findViewById(R.id.hpPlayer)
        hpOpponent      = findViewById(R.id.hpOpponent)

        tvAnnouncement  = findViewById(R.id.tvAnnouncement)

        imgPlayer       = findViewById(R.id.imgPlayer)
        imgOpponent     = findViewById(R.id.imgOpponent)

        logList         = findViewById(R.id.logList)

        btnMove1 = findViewById(R.id.btnMove1)

        btnMove2 = findViewById(R.id.btnMove2)
        btnMove3 = findViewById(R.id.btnMove3)
        btnMove4 = findViewById(R.id.btnMove4)

        btnMove1Text = findViewById(R.id.btnMove1Text)
        btnMove2Text = findViewById(R.id.btnMove2Text)
        btnMove3Text = findViewById(R.id.btnMove3Text)
        btnMove4Text = findViewById(R.id.btnMove4Text)

        btnMove1Type = findViewById(R.id.moveLeftTextType1)
        btnMove2Type = findViewById(R.id.moveLeftTextType2)
        btnMove3Type = findViewById(R.id.moveLeftTextType3)
        btnMove4Type = findViewById(R.id.moveLeftTextType4)




        btnRun     = findViewById(R.id.btnRun)
        btnCapture = findViewById(R.id.btnCapture)

        val playerId = intent.getStringExtra(EXTRA_PLAYER_ID)
        val enemyId = intent.getStringExtra(EXTRA_ENEMY_ID)
        isPvP = intent.getBooleanExtra("IS_PVP", false)
        opponentUserId = intent.getStringExtra("OPPONENT_USER_ID")
        battleId = intent.getStringExtra("BATTLE_ID")
        currentUserId = AuthManager.userId

        lifecycleScope.launch {
            if (playerId == null || enemyId == null) {
                Log.e("BattleActivity", "Missing player or enemy ID")
                finish()
                return@launch
            }

            val fetchedPlayer = Monster.fetchById(playerId)
            if (fetchedPlayer == null) {
                Log.e("BattleActivity", "Failed to fetch player monster")
                finish()
                return@launch
            }
            player = fetchedPlayer

            val fetchedEnemy = Monster.fetchById(enemyId)
            if (fetchedEnemy == null) {
                Log.e("BattleActivity", "Failed to fetch enemy monster")
                finish()
                return@launch
            }
            opponent = fetchedEnemy

            tvPlayerName.text = player.name
            tvOpponentName.text = opponent.name

            loadMonsterSprite(player, imgPlayer)
            loadMonsterSprite(opponent, imgOpponent)

            updateHpUi()
            setupMoveButtons()
            logStats()

            if (isPvP && battleId != null) {
                setupBattleStateListener()
            }
        }
    }

    private fun loadMonsterSprite(monster: Monster, imageView: ImageView) {
        val spriteName = monster.name.lowercase().replace(" ", "_")

        val resourceId = resources.getIdentifier(
            spriteName,
            "drawable",
            packageName
        )

        if (resourceId != 0) {
            imageView.setImageResource(resourceId)
            Log.d("BattleActivity", "Loaded sprite for ${monster.name}: $spriteName")
        } else {
            Log.w("BattleActivity", "Sprite not found for ${monster.name}, using placeholder")
            imageView.setImageResource(R.drawable.ic_launcher_foreground)
        }
    }


    private fun setupMoveButtons() {

        btnMove1Text.text = player.move1 ?: "-"
        btnMove2Text.text = player.move2 ?: "-"
        btnMove3Text.text = player.move3 ?: "-"
        btnMove4Text.text = player.move4 ?: "-"


        btnMove1Type.text = ""
        btnMove2Type.text = ""
        btnMove3Type.text = ""
        btnMove4Type.text = ""


        lifecycleScope.launch {
            player.move1?.let { name ->
                val move = Move.initializeByName(this@BattleActivity, name)
                btnMove1Type.text = move.type1 ?: "-"
            }

            player.move2?.let { name ->
                val move = Move.initializeByName(this@BattleActivity, name)
                btnMove2Type.text = move.type1 ?: "-"
            }

            player.move3?.let { name ->
                val move = Move.initializeByName(this@BattleActivity, name)
                btnMove3Type.text = move.type1 ?: "-"
            }

            player.move4?.let { name ->
                val move = Move.initializeByName(this@BattleActivity, name)
                btnMove4Type.text = move.type1 ?: "-"
            }
        }


        btnMove1.setOnClickListener { onPlayerMoveSelected(player.move1) }
        btnMove2.setOnClickListener { onPlayerMoveSelected(player.move2) }
        btnMove3.setOnClickListener { onPlayerMoveSelected(player.move3) }
        btnMove4.setOnClickListener { onPlayerMoveSelected(player.move4) }

        if (isPvP) {
            btnRun.isEnabled = false
            btnCapture.isEnabled = false
        } else {
            btnRun.setOnClickListener { runFromBattle() }
            btnCapture.setOnClickListener { attemptCapture() }
        }
    }

    private fun attemptCapture() {
        if (player.isFainted || opponent.isFainted) return

        lifecycleScope.launch {
            setMoveButtonsEnabled(false)
            btnCapture.isEnabled = false

            val hpPercent = opponent.currentHp / opponent.maxHp
            val captureChance = 0.2 + (1 - hpPercent) * 0.6

            appendLog("Attempting to capture ${opponent.name}...")
            delay(1000)

            val roll = Random.nextDouble()
            if (roll < captureChance) {
                appendLog("${opponent.name} was captured!")

                val userId = AuthManager.userId
                if (userId != null) {
                    User.addMonster(userId, opponent.id)
                    Monster.setOwner(opponent.id, userId)
                    Log.d("BattleActivity", "Monster ${opponent.id} captured by user $userId")
                }

                delay(1500)
                finish()
            } else {
                appendLog("Capture failed!")
                delay(800)

                val aiMove = aiChoice(opponent)
                performAttack(attacker = opponent, defender = player, move = aiMove, isPlayer = false)
                updateHpUi()
                checkBattleEnd()

                if (!player.isFainted && !opponent.isFainted) {
                    setMoveButtonsEnabled(true)
                    btnCapture.isEnabled = true
                }
            }
        }
    }

    private fun setMoveButtonsEnabled(enabled: Boolean) {
        btnMove1.isEnabled = enabled
        btnMove2.isEnabled = enabled
        btnMove3.isEnabled = enabled
        btnMove4.isEnabled = enabled
    }

    private fun updateHpUi() {
        hpPlayer.max = player.maxHp.toInt()
        hpOpponent.max = opponent.maxHp.toInt()

        hpPlayer.progress = player.currentHp.toInt()
        hpOpponent.progress = opponent.currentHp.toInt()
    }

    private fun appendLog(message: String) {
        tvAnnouncement.text = message
    }


    private fun onPlayerMoveSelected(moveName: String?) {
        if (moveName == null) return
        if (player.isFainted || opponent.isFainted) return

        if (isPvP) {
            handlePvPMoveSelection(moveName)
        } else {
            handlePvEMoveSelection(moveName)
        }
    }

    private fun handlePvEMoveSelection(moveName: String) {
        lifecycleScope.launch {
            setMoveButtonsEnabled(false)

            val chosenMove = Move.initializeByName(this@BattleActivity, moveName)

            if (player.speed >= opponent.speed) {
                performAttack(attacker = player, defender = opponent, move = chosenMove, isPlayer = true)
                if (!opponent.isFainted) {
                    delay(800)
                    val aiMove = aiChoice(opponent)
                    performAttack(attacker = opponent, defender = player, move = aiMove, isPlayer = false)
                }
            } else {
                val aiMove = aiChoice(opponent)
                performAttack(attacker = opponent, defender = player, move = aiMove, isPlayer = false)

                if (!player.isFainted) {
                    delay(800)
                    performAttack(attacker = player, defender = opponent, move = chosenMove, isPlayer = true)
                }
            }

            updateHpUi()
            checkBattleEnd()

            if (!player.isFainted && !opponent.isFainted) {
                setMoveButtonsEnabled(true)
            }
        }
    }

    private fun handlePvPMoveSelection(moveName: String) {
        if (!isMyTurn) {
            Toast.makeText(this, "Wait for your turn!", Toast.LENGTH_SHORT).show()
            return
        }

        val bId = battleId ?: return
        val userId = currentUserId ?: return

        setMoveButtonsEnabled(false)
        appendLog("Executing move...")

        lifecycleScope.launch {
            // Execute move locally and update local HP
            val move = Move.initializeByName(this@BattleActivity, moveName)
            performAttack(player, opponent, move, true)
            delay(1000)

            // Update Firebase with both players' HP
            FirebaseManager.battleStatesRef.child(bId).get()
                .addOnSuccessListener { snapshot ->
                    val state = BattleState.fromSnapshot(snapshot) ?: return@addOnSuccessListener
                    val isPlayer1 = userId == state.player1Id

                    val player1Hp = if (isPlayer1) player.currentHp else opponent.currentHp
                    val player2Hp = if (isPlayer1) opponent.currentHp else player.currentHp
                    val nextTurn = if (isPlayer1) state.player2Id else state.player1Id

                    // Always update HP and switch turn, even if battle ends
                    // This allows the loser to process the final damage before battle finishes
                    BattleState.updateHpAndNextTurn(bId, player1Hp, player2Hp, nextTurn, moveName, userId)
                }

            checkBattleEnd()
        }
    }
    private suspend fun performAttack(attacker: Monster, defender: Monster, move: Move, isPlayer: Boolean) {
        if (!move.doesHit()) {
            appendLog("${attacker.name}'s ${move.name} missed!")
            return
        }


        val result = damageCalc(attacker, move, defender)

        defender.takeDamage(result.damage)

        val monsterUser: String
        if (isPlayer) {
            monsterUser = "Player"
        } else {
            monsterUser = "Enemy"
        }


        val announcerBuild = StringBuilder()
        announcerBuild.append("$monsterUser ${attacker.name} used ${move.name}! It dealt ${result.damage.toInt()} damage.")

        when {
            result.typeMultiplier == 0f -> {
                announcerBuild.append(" It had no effect...")
            }
            result.typeMultiplier > 1.01f -> {
                announcerBuild.append(" It's super effective!")
            }
            result.typeMultiplier < 0.99f -> {
                announcerBuild.append(" It's not very effective...")
            }
        }

        if (result.isCrit) {
            announcerBuild.append(" A critical hit!")
        }

        appendLog(announcerBuild.toString())

        updateHpUi()
        delay(400)
    }


    private suspend fun aiChoice(monster: Monster): Move {
        val moveNames = listOfNotNull(monster.move1, monster.move2, monster.move3, monster.move4)
        if (moveNames.isEmpty()) {

            return Move.initializeByName(this@BattleActivity, "Tackle")
        }
        val chosenName = moveNames.random()
        return Move.initializeByName(this@BattleActivity, chosenName)
    }

    private var lastProcessedTurn: String = ""
    private var lastProcessedMoveTimestamp: Long = 0

    private fun setupBattleStateListener() {
        val bId = battleId ?: return
        val userId = currentUserId ?: return

        battleStateListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val state = BattleState.fromSnapshot(snapshot) ?: return

                if (state.status == "finished") {
                    battleStateListener?.let {
                        FirebaseManager.battleStatesRef.child(bId).removeEventListener(it)
                    }

                    // Update HP from final state
                    updateHpFromBattleState(state)

                    // Handle battle end
                    lifecycleScope.launch {
                        delay(500)
                        checkBattleEnd()
                    }
                    return
                }

                val wasMyTurn = isMyTurn
                isMyTurn = state.currentTurn == userId

                // Check if opponent made a move (new timestamp and not our move)
                if (state.lastMove != null && state.lastMoveUser != null &&
                    state.lastMoveUser != userId &&
                    state.timestamp > lastProcessedMoveTimestamp) {

                    lastProcessedMoveTimestamp = state.timestamp

                    // Update HP from Firebase (remote HP already updated by opponent)
                    val oldPlayerHp = player.currentHp
                    val oldOpponentHp = opponent.currentHp
                    updateHpFromBattleState(state)

                    // Calculate HP changes
                    val playerHpChange = player.currentHp - oldPlayerHp
                    val opponentHpChange = opponent.currentHp - oldOpponentHp

                    // Show text announcement about opponent's move
                    lifecycleScope.launch {
                        if (playerHpChange < 0) {
                            appendLog("Opponent used ${state.lastMove}! Your ${player.name} took ${-playerHpChange.toInt()} damage.")
                        } else {
                            appendLog("Opponent used ${state.lastMove}!")
                        }

                        delay(500)

                        // Check if this move caused a faint, and if so, finish the battle
                        if (player.isFainted || opponent.isFainted) {
                            val winnerId = if (opponent.isFainted) userId else opponentUserId
                            BattleState.finishBattle(bId, winnerId, state.player1Hp, state.player2Hp, state.lastMove, state.lastMoveUser)
                        }

                        checkBattleEnd()

                        if (isMyTurn && !player.isFainted && !opponent.isFainted) {
                            delay(500)
                            appendLog("Your turn! Select a move.")
                            setMoveButtonsEnabled(true)
                        }
                    }
                } else {
                    // Just update HP without announcement (initial state or turn switch)
                    updateHpFromBattleState(state)

                    if (!wasMyTurn && isMyTurn && !player.isFainted && !opponent.isFainted) {
                        lifecycleScope.launch {
                            delay(300)
                            appendLog("Your turn! Select a move.")
                            setMoveButtonsEnabled(true)
                        }
                    } else if (wasMyTurn && !isMyTurn) {
                        appendLog("Opponent's turn...")
                        setMoveButtonsEnabled(false)
                    } else if (!isMyTurn) {
                        appendLog("Opponent's turn...")
                        setMoveButtonsEnabled(false)
                    }

                    checkBattleEnd()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("BattleActivity", "Battle state listener cancelled: ${error.message}")
            }
        }

        FirebaseManager.battleStatesRef.child(bId).addValueEventListener(battleStateListener!!)
    }

    private fun updateHpFromBattleState(state: BattleState) {
        val userId = currentUserId ?: return

        val myHp = if (userId == state.player1Id) state.player1Hp else state.player2Hp
        val opponentHp = if (userId == state.player1Id) state.player2Hp else state.player1Hp

        player.currentHp = myHp
        opponent.currentHp = opponentHp
        updateHpUi()
    }

    private fun checkBattleEnd() {
        if (player.isFainted && opponent.isFainted) {
            appendLog("Both monsters fainted! It's a tie.")
            setMoveButtonsEnabled(false)
            btnRun.isEnabled = false
            btnCapture.isEnabled = false
            // Delete opponent if wild (not PvP)
            if (!isPvP && opponent.isWild) {
                FirebaseManager.monstersRef.child(opponent.id).removeValue()
            }
            lifecycleScope.launch {
                delay(2000)
                finish()
            }
        } else if (player.isFainted || player.currentHp <= 0f) {
            appendLog("${player.name} fainted! Defeat!")
            setMoveButtonsEnabled(false)
            btnRun.isEnabled = false
            btnCapture.isEnabled = false
            if (isPvP) {
                handlePvPDefeat()
            }
            lifecycleScope.launch {
                delay(2000)
                finish()
            }
        } else if (opponent.isFainted || opponent.currentHp <= 0f) {
            appendLog("${opponent.name} fainted! Victory!")
            setMoveButtonsEnabled(false)
            btnRun.isEnabled = false
            btnCapture.isEnabled = false
            if (isPvP) {
                handlePvPVictory()
            } else {
                giveVictoryRewards()
            }
            // Delete opponent if wild (not PvP)
            if (!isPvP && opponent.isWild) {
                FirebaseManager.monstersRef.child(opponent.id).removeValue()
                Log.d("BattleActivity", "Defeated wild monster ${opponent.id} deleted from Firebase")
            }
            lifecycleScope.launch {
                delay(2000)
                finish()
            }
        }
    }

    private fun handlePvPVictory() {
        val currentUserId = AuthManager.userId ?: return
        val opponentId = opponentUserId ?: return

        lifecycleScope.launch {
            val opponentUser = User.fetchById(opponentId) ?: return@launch

            if (opponentUser.bag.isEmpty()) {
                Toast.makeText(
                    this@BattleActivity,
                    "Victory! Opponent has no items to take.",
                    Toast.LENGTH_LONG
                ).show()
                return@launch
            }

            val randomItem = opponentUser.bag.entries.random()
            val itemName = randomItem.key
            val itemCount = randomItem.value

            if (itemCount > 0) {
                User.removeByItemName(opponentId, itemName, 1)
                User.addItem(currentUserId, itemName, 1)

                Toast.makeText(
                    this@BattleActivity,
                    "Victory! You received 1x $itemName from your opponent!",
                    Toast.LENGTH_LONG
                ).show()

                Log.d("BattleActivity", "PvP Victory: Took $itemName from opponent $opponentId")
            }
        }
    }

    private fun handlePvPDefeat() {
        val currentUserId = AuthManager.userId ?: return
        val opponentId = opponentUserId ?: return

        lifecycleScope.launch {
            val currentUser = User.fetchById(currentUserId) ?: return@launch

            if (currentUser.bag.isEmpty()) {
                Toast.makeText(
                    this@BattleActivity,
                    "Defeat! You have no items to lose.",
                    Toast.LENGTH_LONG
                ).show()
                return@launch
            }

            val randomItem = currentUser.bag.entries.random()
            val itemName = randomItem.key
            val itemCount = randomItem.value

            if (itemCount > 0) {
                User.removeByItemName(currentUserId, itemName, 1)
                User.addItem(opponentId, itemName, 1)

                Toast.makeText(
                    this@BattleActivity,
                    "Defeat! You lost 1x $itemName to your opponent!",
                    Toast.LENGTH_LONG
                ).show()

                Log.d("BattleActivity", "PvP Defeat: Lost $itemName to opponent $opponentId")
            }
        }
    }

    companion object {
        const val EXTRA_PLAYER_ID   = "extra_player_id"
        const val EXTRA_ENEMY_ID    = "extra_enemy_id"
    }

    private fun logStats() {
        logMonsterStats("PLAYER", player)
        logMonsterStats("ENEMY", opponent)
    }

    private fun logMonsterStats(tag: String, mon: Monster) {
        Log.d("BattleStats", "[$tag] ${mon.name}")
        Log.d("BattleStats", "[$tag] Level: ${mon.level}")
        Log.d("BattleStats", "[$tag] HP: ${mon.currentHp}/${mon.maxHp}")
        Log.d("BattleStats", "[$tag] Attack: ${mon.attack}")
        Log.d("BattleStats", "[$tag] Defense: ${mon.defense}")
        Log.d("BattleStats", "[$tag] Speed: ${mon.speed}")

        Log.d("BattleStats", "[$tag] Moves (names only):")
        mon.move1?.let { Log.d("BattleStats", "   Move1: $it") }
        mon.move2?.let { Log.d("BattleStats", "   Move2: $it") }
        mon.move3?.let { Log.d("BattleStats", "   Move3: $it") }
        mon.move4?.let { Log.d("BattleStats", "   Move4: $it") }
    }
    private fun giveVictoryRewards() {
        val userId = AuthManager.userId ?: return


        User.addItem(userId, "Level Up Scroll", 1)

        if (opponent.type1 == "") {
            User.addItem(userId, "Water Badge", 1)
            Toast.makeText(this, "You received a Water Badge!", Toast.LENGTH_SHORT).show()

        }// add later for other rewards
        val roll = Random.nextDouble()
        if (roll > 0.2) {
            User.addItem(userId, "Level Up Scroll", 1)
            Toast.makeText(this, "You received a Level Up Scroll!", Toast.LENGTH_SHORT).show()

        } else {
            User.addItem(userId, "Legendary Level Up Scroll", 1)
            Toast.makeText(this, "You received a Legendary Level Up Scroll!", Toast.LENGTH_SHORT).show()

        }


    }
    //despawns monsters after running from them
    private fun runFromBattle() {

        if (!::opponent.isInitialized) {
            finish()
            return
        }
        FirebaseManager.monstersRef.child(opponent.id).removeValue()
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()

        if (isPvP && battleId != null) {
            battleStateListener?.let {
                FirebaseManager.battleStatesRef.child(battleId!!).removeEventListener(it)
            }
        }
    }

}
