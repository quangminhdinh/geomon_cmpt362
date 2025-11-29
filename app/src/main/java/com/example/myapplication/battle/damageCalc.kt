package com.example.myapplication.battle
import android.util.Log
import com.example.myapplication.battle.Monster
import com.example.myapplication.battle.Move
import com.example.myapplication.battle.TypeChart
/* calculates following monsters relating stat and type changes return is the damage
Level
Power
Attack/special
Defense/special
Weather
Critical
Random (85-100)/100
STAB
Type 1/8, 1/4, 1/2. 1 2 4 8
Status .5 burn
*/
data class DamageResult(
    val damage: Float,
    val isCrit: Boolean,
    val typeMultiplier: Float
)
fun damageCalc(attacker: Monster, move: Move, defender: Monster): DamageResult  {


    val level: Float = attacker.level.toFloat()
    val power: Float = move.baseDamage.toFloat()

    var monsterAttack = 10f
    var defenderDefense = 10f

    if (move.category == "physical") {
        monsterAttack = attacker.attack.toFloat()
        defenderDefense = defender.defense.toFloat()
    } else if (move.category == "special") {
        monsterAttack = attacker.specialAttack.toFloat()
        defenderDefense = defender.specialDefense.toFloat()
    }


    val isCrit = (0..99).random() < 5
    val crit = if (isCrit) 1.5f else 1.0f


    val random = (85..100).random() / 100f


    var stab = 1.0f

    Log.d("DamageCalc STAB", "Attacker Types: ${attacker.type1}, ${attacker.type2}, ${attacker.type3}")
    Log.d("DamageCalc STAB", "Move Type: ${move.type1}")

    var stabApplied = false

    if (attacker.type1 == move.type1) {
        stab = 1.5f
        stabApplied = true
        Log.d("DamageCalc STAB", "STAB matched type1: ${attacker.type1}")
    }
    else if (attacker.type2 == move.type1) {
        stab = 1.5f
        stabApplied = true
        Log.d("DamageCalc STAB", "STAB matched type2: ${attacker.type2}")
    }
    else if (attacker.type3 == move.type1) {
        stab = 1.5f
        stabApplied = true
        Log.d("DamageCalc STAB", "STAB matched type3: ${attacker.type3}")
    }

    Log.d("DamageCalc STAB", "STAB Applied: $stabApplied  (Value = $stab)")


    val moveType = Element.fromString(move.type1)

    val defenderTypes = listOfNotNull(
        Element.fromString(defender.type1),
        Element.fromString(defender.type2),
        Element.fromString(defender.type3)
    )

    val typeMultiplier = TypeChart.multiplier(moveType, defenderTypes)


    val other = 1.0f

    val main = (((2f * level) / 5f + 2f) * power * (monsterAttack / defenderDefense)) / 50f

    val tail = crit * random * stab * typeMultiplier * other


    val finalDamage = (main * tail).toFloat()

    Log.d("DamageCalc", "----- DAMAGE CALCULATION -----")
    Log.d("DamageCalc", "Move: ${move.name} (${move.category})")
    Log.d("DamageCalc", "Attacker: ${attacker.name}  Defender: ${defender.name}")
    Log.d("DamageCalc", "Level = $level")
    Log.d("DamageCalc", "Power = $power")
    Log.d("DamageCalc", "Attack = $monsterAttack   Defense = $defenderDefense")
    Log.d("DamageCalc", "Crit Multiplier = $crit   (isCrit = $isCrit)")
    Log.d("DamageCalc", "Random Multiplier = $random")
    Log.d("DamageCalc", "STAB = $stab")
    Log.d("DamageCalc", "Type Effectiveness = $typeMultiplier")
    Log.d("DamageCalc", "Other Multipliers = $other")
    Log.d("DamageCalc", "Main Formula Output = $main")
    Log.d("DamageCalc", "TOTAL Damage = $finalDamage")
    Log.d("DamageCalc", "--------------------------------")

    return DamageResult(
        damage = finalDamage,
        isCrit = isCrit,
        typeMultiplier = typeMultiplier.toFloat()
    )
}
