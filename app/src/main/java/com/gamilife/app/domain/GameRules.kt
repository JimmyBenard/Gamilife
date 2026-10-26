package com.gamilife.app.domain

import kotlin.math.roundToInt

/** Règles pures : aucune dépendance Android, donc vérifiables en test unitaire. */
enum class StatKey { HP, ATK, CRIT_CHANCE, CRIT_DAMAGE, ACCURACY, MIN_DAMAGE, MAX_DAMAGE, AME, RME }
enum class EffectKind { GOOD, BAD }
enum class SkillKind { HABITUDE, QUOTIDIENNE, PONCTUELLE }
enum class Rank { F, E, D, C, B, A, S }
enum class IrregularityMode { CONSERVE, PERTE_XP, RETROGRADATION, PERSONNALISE }

data class StatModifier(
    val stat: StatKey, val flat: Int = 0, val percent: Double = 0.0,
    val multiplier: Double = 1.0
)
data class FinalStats(val values: Map<StatKey, Int>) {
    operator fun get(key: StatKey) = values[key] ?: 0
    val hp get() = this[StatKey.HP]
}

object StatCalculator {
    /** Ordre déterministe : base + plats → pourcentages cumulés → multiplicateurs → bornes. */
    fun calculate(base: Map<StatKey, Int>, modifiers: List<StatModifier>): FinalStats = FinalStats(
        StatKey.entries.associateWith { stat ->
            val relevant = modifiers.filter { it.stat == stat }
            val raw = (base[stat] ?: 0).toDouble() + relevant.sumOf { it.flat }
            val percent = 1.0 + relevant.sumOf { it.percent }
            val multi = relevant.fold(1.0) { acc, m -> acc * m.multiplier }
            val result = (raw * percent * multi).coerceIn(0.0, 2_000_000_000.0).roundToInt()
            when (stat) {
                StatKey.CRIT_CHANCE, StatKey.ACCURACY, StatKey.AME, StatKey.RME -> result.coerceIn(0, 100)
                else -> result
            }
        }
    )
}

interface RandomSource { fun nextInt(from: Int, until: Int): Int; fun chance(percent: Int): Boolean }
object DefaultRandom : RandomSource {
    private val random = java.util.Random()
    override fun nextInt(from: Int, until: Int) = random.nextInt(until - from) + from
    override fun chance(percent: Int) = nextInt(0, 100) < percent.coerceIn(0, 100)
}
data class Combatant(val name: String, val hp: Int, val stats: FinalStats)
data class SkillPower(val name: String, val coefficient: Double, val rank: Rank, val effects: List<EffectRule> = emptyList())
data class EffectRule(val name: String, val kind: EffectKind, val baseChance: Int, val durationTurns: Int, val modifiers: List<StatModifier>)
data class CombatResolution(val damage: Int, val critical: Boolean, val appliedEffects: List<EffectRule>, val targetHp: Int, val log: String)

object CombatEngine {
    fun resolve(player: Combatant, monster: Combatant, skill: SkillPower, random: RandomSource): CombatResolution {
        val min = player.stats[StatKey.MIN_DAMAGE].coerceAtLeast(1)
        val max = player.stats[StatKey.MAX_DAMAGE].coerceAtLeast(min)
        val base = random.nextInt(min, max + 1) + player.stats[StatKey.ATK]
        val critical = random.chance(player.stats[StatKey.CRIT_CHANCE])
        val criticalMultiplier = if (critical) 1.0 + player.stats[StatKey.CRIT_DAMAGE] / 100.0 else 1.0
        val damage = (base * skill.coefficient * criticalMultiplier).roundToInt().coerceAtLeast(1)
        val effects = skill.effects.filter { rule ->
            val chance = if (rule.kind == EffectKind.BAD)
                (rule.baseChance + player.stats[StatKey.AME] - monster.stats[StatKey.RME]).coerceIn(0, 100)
            else rule.baseChance.coerceIn(0, 100)
            random.chance(chance)
        }
        val remaining = (monster.hp - damage).coerceAtLeast(0)
        val tag = if (critical) "COUP CRITIQUE ! " else ""
        return CombatResolution(damage, critical, effects, remaining, "$tag${skill.name} inflige $damage dégâts à ${monster.name}.")
    }
}

object ProgressionRules {
    fun xpToNext(level: Int, rank: Rank) = 40 + level * 25 + rank.ordinal * 20
    data class Result(val xp: Int, val level: Int, val rank: Rank, val leveledUp: Boolean)
    fun gain(xp: Int, level: Int, rank: Rank, reward: Int): Result {
        var currentXp = xp + reward; var currentLevel = level; var currentRank = rank; var leveled = false
        while (currentXp >= xpToNext(currentLevel, currentRank)) {
            currentXp -= xpToNext(currentLevel, currentRank); currentLevel++; leveled = true
            if (currentLevel > 10 && currentRank != Rank.S) { currentRank = Rank.entries[currentRank.ordinal + 1]; currentLevel = 1 }
        }
        return Result(currentXp, currentLevel, currentRank, leveled)
    }
    fun irregular(xp: Int, level: Int, rank: Rank, mode: IrregularityMode, loss: Int): Result = when (mode) {
        IrregularityMode.CONSERVE -> Result(xp, level, rank, false)
        IrregularityMode.PERTE_XP, IrregularityMode.PERSONNALISE -> Result((xp - loss).coerceAtLeast(0), level, rank, false)
        IrregularityMode.RETROGRADATION -> if (level > 1) Result(0, level - 1, rank, false) else Result(0, 1, rank, false)
    }
}
