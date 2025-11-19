package geomon.myapplication.ui.battle

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlin.math.max

class BattleViewModel : ViewModel() {
    //Basic Ui state without database implementation
    data class UiState(
        val turn: Int = 1,
        val p1Name: String = "Player",
        val p2Name: String = "Enemy",
        val p1Hp: Int = 100,
        val p2Hp: Int = 100,
        val p1HpPct: Int = 100,
        val p2HpPct: Int = 100,
        val p1Speed: Int = 50,
        val p2Speed: Int = 60,
        val log: List<String> = emptyList()

    )

    private val _ui = MutableStateFlow(UiState())
    val ui: StateFlow<UiState> = _ui

    fun useMove(index: Int) {
        when (index) {
            1 -> useMove1()
            2 -> useMove2()
            3 -> useMove3()
            4 -> useMove4()
        }
    }

    fun useMove1() = applyDamage(
        attacker = 1,
        basePower = 22,
        moveName = "Mud Slap"
    )

    fun useMove2() = applyDamage(
        attacker = 1,
        basePower = 0,
        moveName = "Recover",
        heal = 25
    )

    fun useMove3() = applyDamage(
        attacker = 1,
        basePower = 35,
        moveName = "Shadow Punch"
    )

    fun useMove4() = applyDamage(
        attacker = 1,
        basePower = 28,
        moveName = "Earth Quake"
    )


    fun enemyTurn() = applyDamage(
        attacker = 2,
        basePower = 20,
        moveName = "Acid Spray"
    )

   //Applies damage depending on attacker and creates a lot list for debugging
    private fun applyDamage(attacker: Int, basePower: Int, moveName: String, heal: Int = 0) {
        viewModelScope.launch {
            val s = _ui.value

            var newP1 = s.p1Hp
            var newP2 = s.p2Hp
            val newLog = s.log.toMutableList()

            if (attacker == 1) {
                if (heal > 0) {
                    newP1 = max(0, newP1 + heal).coerceAtMost(100)
                    newLog += "${s.p1Name} used $moveName (heal +$heal)"
                } else {
                    val dmg = basePower
                    newP2 = (newP2 - dmg).coerceAtLeast(0)
                    newLog += "${s.p1Name} used $moveName (-$dmg)"
                }
            } else {
                val dmg = basePower
                newP1 = (newP1 - dmg).coerceAtLeast(0)
                newLog += "${s.p2Name} used $moveName (-$dmg)"
            }

            val nextTurn = if (attacker == 2) s.turn + 1 else s.turn

            _ui.value = s.copy(
                turn = nextTurn,
                p1Hp = newP1,
                p2Hp = newP2,
                p1HpPct = newP1.coerceIn(0, 100),
                p2HpPct = newP2.coerceIn(0, 100),
                log = newLog
            )
        }
    }
}
