package com.group14.geomon.battle


enum class Element {
    NORMAL, FIRE, WATER, GRASS, ELECTRIC, ICE, FIGHTING, POISON,
    GROUND, FLYING, PSYCHIC, BUG, ROCK, GHOST, DRAGON, DARK, STEEL, FAIRY;

    companion object {
        fun fromString(value: String?): Element? {
            if (value == null)
            {
                return null
            }
            return try {

                valueOf(value.uppercase())
            } catch (e: IllegalArgumentException) {
                null
            }
        }
    }
}


// 0 is no effect
// 0.5 is not very effective
// 1 is normal
// 2 is super effective

object TypeChart {

    private val chart: Map<Pair<Element, Element>, Double> = mapOf(


        (Element.FIRE to Element.GRASS)   to 2.0,
        (Element.FIRE to Element.BUG)     to 2.0,
        (Element.FIRE to Element.STEEL)   to 2.0,
        (Element.FIRE to Element.ICE)     to 2.0,
        (Element.FIRE to Element.FIRE)    to 0.5,
        (Element.FIRE to Element.WATER)   to 0.5,
        (Element.FIRE to Element.ROCK)    to 0.5,
        (Element.FIRE to Element.DRAGON)  to 0.5,

        (Element.WATER to Element.FIRE)   to 2.0,
        (Element.WATER to Element.ROCK)   to 2.0,
        (Element.WATER to Element.GROUND) to 2.0,
        (Element.WATER to Element.WATER)  to 0.5,
        (Element.WATER to Element.GRASS)  to 0.5,
        (Element.WATER to Element.DRAGON) to 0.5,

        (Element.GRASS to Element.WATER)  to 2.0,
        (Element.GRASS to Element.ROCK)   to 2.0,
        (Element.GRASS to Element.GROUND) to 2.0,
        (Element.GRASS to Element.GRASS)  to 0.5,
        (Element.GRASS to Element.FIRE)   to 0.5,
        (Element.GRASS to Element.BUG)    to 0.5,
        (Element.GRASS to Element.POISON) to 0.5,
        (Element.GRASS to Element.FLYING) to 0.5,
        (Element.GRASS to Element.DRAGON) to 0.5,

        (Element.ELECTRIC to Element.WATER)    to 2.0,
        (Element.ELECTRIC to Element.FLYING)   to 2.0,
        (Element.ELECTRIC to Element.ELECTRIC) to 0.5,
        (Element.ELECTRIC to Element.GRASS)    to 0.5,
        (Element.ELECTRIC to Element.DRAGON)   to 0.5,
        (Element.ELECTRIC to Element.GROUND)   to 0.0,

        (Element.GROUND to Element.FIRE)     to 2.0,
        (Element.GROUND to Element.ELECTRIC) to 2.0,
        (Element.GROUND to Element.POISON)   to 2.0,
        (Element.GROUND to Element.ROCK)     to 2.0,
        (Element.GROUND to Element.STEEL)    to 2.0,
        (Element.GROUND to Element.GRASS)    to 0.5,
        (Element.GROUND to Element.BUG)      to 0.5,
        (Element.GROUND to Element.FLYING)   to 0.0,

        (Element.NORMAL to Element.ROCK)   to 0.5,
        (Element.NORMAL to Element.STEEL)  to 0.5,
        (Element.NORMAL to Element.GHOST)  to 0.0,


            (Element.ICE to Element.GRASS)   to 2.0,
        (Element.ICE to Element.GROUND)  to 2.0,
        (Element.ICE to Element.FLYING)  to 2.0,
        (Element.ICE to Element.DRAGON)  to 2.0,
        (Element.ICE to Element.FIRE)    to 0.5,
        (Element.ICE to Element.WATER)   to 0.5,
        (Element.ICE to Element.ICE)     to 0.5,
        (Element.ICE to Element.STEEL)   to 0.5,

        (Element.ROCK to Element.FIRE)     to 2.0,
        (Element.ROCK to Element.ICE)      to 2.0,
        (Element.ROCK to Element.FLYING)   to 2.0,
        (Element.ROCK to Element.BUG)      to 2.0,
        (Element.ROCK to Element.FIGHTING) to 0.5,
        (Element.ROCK to Element.GROUND)   to 0.5,
        (Element.ROCK to Element.STEEL)    to 0.5,


        (Element.FLYING to Element.GRASS)  to 2.0,
        (Element.FLYING to Element.FIGHTING) to 2.0,
        (Element.FLYING to Element.BUG)    to 2.0,
        (Element.FLYING to Element.ELECTRIC) to 0.5,
        (Element.FLYING to Element.ROCK)   to 0.5,
        (Element.FLYING to Element.STEEL)  to 0.5,

        (Element.BUG to Element.GRASS)   to 2.0,
        (Element.BUG to Element.PSYCHIC) to 2.0,
        (Element.BUG to Element.DARK)    to 2.0,
        (Element.BUG to Element.FIRE)    to 0.5,
        (Element.BUG to Element.FIGHTING) to 0.5,
        (Element.BUG to Element.POISON)  to 0.5,
        (Element.BUG to Element.FLYING)  to 0.5,
        (Element.BUG to Element.GHOST)   to 0.5,
        (Element.BUG to Element.STEEL)   to 0.5,
        (Element.BUG to Element.FAIRY)   to 0.5,


        (Element.POISON to Element.GRASS)  to 2.0,
        (Element.POISON to Element.FAIRY)  to 2.0,
        (Element.POISON to Element.POISON) to 0.5,
        (Element.POISON to Element.GROUND) to 0.5,
        (Element.POISON to Element.ROCK)   to 0.5,
        (Element.POISON to Element.GHOST)  to 0.5,
        (Element.POISON to Element.STEEL)  to 0.0,


        (Element.FIGHTING to Element.NORMAL)   to 2.0,
        (Element.FIGHTING to Element.ROCK)     to 2.0,
        (Element.FIGHTING to Element.STEEL)    to 2.0,
        (Element.FIGHTING to Element.ICE)      to 2.0,
        (Element.FIGHTING to Element.DARK)     to 2.0,
        (Element.FIGHTING to Element.FLYING)   to 0.5,
        (Element.FIGHTING to Element.POISON)   to 0.5,
        (Element.FIGHTING to Element.BUG)      to 0.5,
        (Element.FIGHTING to Element.PSYCHIC)  to 0.5,
        (Element.FIGHTING to Element.FAIRY)    to 0.5,
        (Element.FIGHTING to Element.GHOST)    to 0.0,


        (Element.PSYCHIC to Element.FIGHTING) to 2.0,
        (Element.PSYCHIC to Element.POISON)   to 2.0,
        (Element.PSYCHIC to Element.PSYCHIC)  to 0.5,
        (Element.PSYCHIC to Element.STEEL)    to 0.5,
        (Element.PSYCHIC to Element.DARK)     to 0.0,


        (Element.GHOST to Element.GHOST)   to 2.0,
        (Element.GHOST to Element.PSYCHIC) to 2.0,
        (Element.GHOST to Element.DARK)    to 0.5,
        (Element.GHOST to Element.NORMAL)  to 0.0,


        (Element.DRAGON to Element.DRAGON) to 2.0,
        (Element.DRAGON to Element.STEEL)  to 0.5,
        (Element.DRAGON to Element.FAIRY)  to 0.0,


        (Element.DARK to Element.GHOST)   to 2.0,
        (Element.DARK to Element.PSYCHIC) to 2.0,
        (Element.DARK to Element.DARK)    to 0.5,
        (Element.DARK to Element.FIGHTING) to 0.5,
        (Element.DARK to Element.FAIRY)   to 0.5,


        (Element.STEEL to Element.ICE)    to 2.0,
        (Element.STEEL to Element.ROCK)   to 2.0,
        (Element.STEEL to Element.FAIRY)  to 2.0,
        (Element.STEEL to Element.FIRE)   to 0.5,
        (Element.STEEL to Element.WATER)  to 0.5,
        (Element.STEEL to Element.ELECTRIC) to 0.5,
        (Element.STEEL to Element.STEEL)  to 0.5,

        (Element.FAIRY to Element.DRAGON) to 2.0,
        (Element.FAIRY to Element.FIGHTING) to 2.0,
        (Element.FAIRY to Element.DARK)  to 2.0,
        (Element.FAIRY to Element.FIRE)  to 0.5,
        (Element.FAIRY to Element.POISON) to 0.5,
        (Element.FAIRY to Element.STEEL)  to 0.5

    )


    fun multiplier(attackType: Element?, defenderTypes: List<Element?>): Double {
        if (attackType == null){
            return 1.0
        }

        return defenderTypes
            .filterNotNull()
            .fold(1.0) { acc, def ->
                val m = chart[attackType to def] ?: 1.0
                acc * m
            }
    }

    fun effectivenessCategory(attackType: Element?, defenderTypes: List<Element?>): Effectiveness {
        val m = multiplier(attackType, defenderTypes)
        return when {
            m == 0.0  -> Effectiveness.NO_EFFECT
            m > 1.0   -> Effectiveness.SUPER_EFFECTIVE
            m < 1.0   -> Effectiveness.NOT_VERY_EFFECTIVE
            else      -> Effectiveness.NORMAL
        }
    }

}


enum class Effectiveness {
    SUPER_EFFECTIVE,
    NOT_VERY_EFFECTIVE,
    NO_EFFECT,
    NORMAL
}
