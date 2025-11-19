package geomon.myapplication.battle.ui

import android.os.Bundle
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import geomon.myapplication.R
import geomon.myapplication.ui.battle.BattleLogAdapter
import geomon.myapplication.ui.battle.BattleViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class BattleActivity : AppCompatActivity() {

    private val vm: BattleViewModel by viewModels()
    private lateinit var btn1: Button
    private lateinit var btn2: Button
    private lateinit var btn3: Button
    private lateinit var btn4: Button
    private lateinit var btnRun: Button
    private lateinit var tvTurn: TextView
    private lateinit var tvP1: TextView
    private lateinit var tvP2: TextView
    private lateinit var hpP1: ProgressBar
    private lateinit var hpP2: ProgressBar
    private lateinit var logList: RecyclerView
    private lateinit var logAdapter: BattleLogAdapter

    private var lastState: BattleViewModel.UiState? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_battle)

        btn1 = findViewById(R.id.btnMove1)
        btn2 = findViewById(R.id.btnMove2)
        btn3 = findViewById(R.id.btnMove3)
        btn4 = findViewById(R.id.btnMove4)
        btnRun = findViewById(R.id.btnRun)
        tvTurn = findViewById(R.id.tvTurn)
        tvP1 = findViewById(R.id.tvPlayerName)
        tvP2 = findViewById(R.id.tvOpponentName)
        hpP1 = findViewById(R.id.hpPlayer)
        hpP2 = findViewById(R.id.hpOpponent)
        logList = findViewById(R.id.logList)

        logAdapter = BattleLogAdapter(emptyList())
        logList.layoutManager = LinearLayoutManager(this)
        logList.adapter = logAdapter


        btn1.setOnClickListener { resolveTurn(1) }
        btn2.setOnClickListener { resolveTurn(2) }
        btn3.setOnClickListener { resolveTurn(3) }
        btn4.setOnClickListener { resolveTurn(4) }


        btnRun.setOnClickListener { finish() }


        lifecycleScope.launch {
            vm.ui.collectLatest { s ->
                lastState = s
                tvTurn.text = "Turn ${s.turn}"
                tvP1.text = s.p1Name
                tvP2.text = s.p2Name
                hpP1.progress = s.p1HpPct
                hpP2.progress = s.p2HpPct
                logAdapter.submit(s.log)
                logList.scrollToPosition((s.log.size - 1).coerceAtLeast(0))


                if (s.p1HpPct <= 0) {
                    finish()
                }
            }
        }
    }

    // needs to do calcs with data base implementation
    private fun resolveTurn(moveIndex: Int) {
        val s = lastState ?: return

        lifecycleScope.launch {
            if (s.p1Speed >= s.p2Speed) {
                vm.useMove(moveIndex)
                delay(150L)

                val afterPlayer = lastState
                if (afterPlayer != null && afterPlayer.p2HpPct > 0) {
                    vm.enemyTurn()
                }
            } else {
                vm.enemyTurn()
                delay(150L)

                val afterEnemy = lastState
                if (afterEnemy != null && afterEnemy.p1HpPct > 0) {
                    vm.useMove(moveIndex)
                }
            }

            delay(50L)
            val end = lastState
            if (end != null && end.p1HpPct <= 0) {
                finish()
            }
        }
    }
}
