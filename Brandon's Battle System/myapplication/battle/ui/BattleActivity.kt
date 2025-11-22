package geomon.myapplication.battle.ui

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

import geomon.myapplication.battle.Monster
import geomon.myapplication.battle.Move
import geomon.myapplication.R
import geomon.myapplication.battle.damageCalc

class BattleActivity : ComponentActivity() {

    private lateinit var player: Monster
    private lateinit var opponent: Monster

    // UI references
    private lateinit var tvPlayerName: TextView
    private lateinit var tvOpponentName: TextView

    private lateinit var hpPlayer: ProgressBar
    private lateinit var hpOpponent: ProgressBar

    private lateinit var tvAnnouncement: TextView

    private lateinit var logList: RecyclerView

    private lateinit var btnMove1: Button
    private lateinit var btnMove2: Button
    private lateinit var btnMove3: Button
    private lateinit var btnMove4: Button

    private lateinit var btnRun: Button

    private lateinit var playerTeam: List<Monster>// take from list later, implement teams for both enemy and player
    private lateinit var enemyTeam: List<Monster>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_battle)




        tvPlayerName = findViewById(R.id.tvPlayerName)
        tvPlayerName    = findViewById(R.id.tvPlayerName)
        tvOpponentName  = findViewById(R.id.tvOpponentName)

        hpPlayer        = findViewById(R.id.hpPlayer)
        hpOpponent      = findViewById(R.id.hpOpponent)

        tvAnnouncement  = findViewById(R.id.tvAnnouncement)
        logList         = findViewById(R.id.logList)


        btnMove1 = findViewById(R.id.btnMove1)
        btnMove2 = findViewById(R.id.btnMove2)
        btnMove3 = findViewById(R.id.btnMove3)
        btnMove4 = findViewById(R.id.btnMove4)
        btnRun = findViewById(R.id.btnRun)

        val playerName  = intent.getStringExtra(EXTRA_PLAYER_NAME) ?: "Aquacoon"
        val enemyName   = intent.getStringExtra(EXTRA_ENEMY_NAME) ?: "Aquacoon"


        lifecycleScope.launch {
            player = Monster.initializeByName(this@BattleActivity, playerName)
            opponent = Monster.initializeByName(this@BattleActivity, enemyName)

            tvPlayerName.text = player.name
            tvOpponentName.text = opponent.name
            updateHpUi()
            setupMoveButtons()
            logStats()

        }


    }

    // UI Setup

    private fun setupMoveButtons() {


        btnMove1.text = player.move1?.name ?: "-"
        btnMove2.text = player.move2?.name ?: "-"
        btnMove3.text = player.move3?.name ?: "-"
        btnMove4.text = player.move4?.name ?: "-"

        btnMove1.setOnClickListener { onPlayerMoveSelected(player.move1) }
        btnMove2.setOnClickListener { onPlayerMoveSelected(player.move2) }
        btnMove3.setOnClickListener { onPlayerMoveSelected(player.move3) }
        btnMove4.setOnClickListener { onPlayerMoveSelected(player.move4) }


        btnRun.setOnClickListener { finish() }
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

    // Battle Logic

    private fun onPlayerMoveSelected(chosenMove: Move?) {
        if (chosenMove == null) return
        if (player.isFainted || opponent.isFainted) return


        lifecycleScope.launch {
            setMoveButtonsEnabled(false)

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
    //If the move hits by calling doeshit(), calculate damage through damageCalc. Log appends depending on the move
    private suspend fun performAttack(attacker: Monster, defender: Monster, move: Move, isPlayer: Boolean) {
        if (!move.doesHit()) {
            appendLog("${attacker.name}'s ${move.name} missed!")
            return
        }

        val damage = damageCalc(attacker, move, defender)
        defender.takeDamage(damage)

        val who = if (isPlayer) "Player" else "Enemy"
        appendLog("$who ${attacker.name} used ${move.name}! It dealt ${damage.toInt()} damage.")

        updateHpUi()
        delay(400)
    }
    /*
    private fun damageCalc(attacker: Monster, move: Move, receiver: Monster): Float {
        // Simple damage: use Move.getDamage() from your Move.kt
        return move.getDamage(
            attackerLevel   = attacker.level,
            attackerAttack  = attacker.attack,
            defenderDefense = receiver.defense
        )
    }

     */
    //Randomly pick 1 of the available moves
    private fun aiChoice(monster: Monster): Move {
        val moves = listOfNotNull(monster.move1, monster.move2, monster.move3, monster.move4)
        if (moves.isEmpty()) {
            return Move.initializeByName("Tackle")
        }
        val index = Random.nextInt(moves.size)
        return moves[index]
    }

    private fun checkBattleEnd() {
        if (player.isFainted && opponent.isFainted) {
            appendLog("Both monsters fainted! It's a tie.")
            setMoveButtonsEnabled(false)
        } else if (player.isFainted) {
            appendLog("${player.name} fainted! Defeat!")
            setMoveButtonsEnabled(false)
        } else if (opponent.isFainted) {
            appendLog("${opponent.name} fainted! Victory!")
            setMoveButtonsEnabled(false)
        }
    }

    companion object {
        const val EXTRA_PLAYER_NAME = "extra_player_name"
        const val EXTRA_ENEMY_NAME  = "extra_enemy_name"
    }

    private fun logStats() {
        logMonsterStats("PLAYER", player)
        logMonsterStats("ENEMY", opponent)
    }
//debugging stats
    private fun logMonsterStats(tag: String, mon: Monster) {
        Log.d("BattleStats", "[$tag] ${mon.name}")
        Log.d("BattleStats", "[$tag] Level: ${mon.level}")
        Log.d("BattleStats", "[$tag] HP: ${mon.currentHp}/${mon.maxHp}")
        Log.d("BattleStats", "[$tag] Attack: ${mon.attack}")
        Log.d("BattleStats", "[$tag] Defense: ${mon.defense}")
        Log.d("BattleStats", "[$tag] Speed: ${mon.speed}")

        Log.d("BattleStats", "[$tag] Moves:")
        mon.move1?.let { Log.d("BattleStats", "   Move1: ${it.name} (Power ${it.baseDamage})") }
        mon.move2?.let { Log.d("BattleStats", "   Move2: ${it.name} (Power ${it.baseDamage})") }
        mon.move3?.let { Log.d("BattleStats", "   Move3: ${it.name} (Power ${it.baseDamage})") }
        mon.move4?.let { Log.d("BattleStats", "   Move4: ${it.name} (Power ${it.baseDamage})") }
    }


}
