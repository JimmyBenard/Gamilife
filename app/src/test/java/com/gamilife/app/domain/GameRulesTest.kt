package com.gamilife.app.domain

import org.junit.Assert.*
import org.junit.Test

private class FixedRandom(private val value:Int = 0): RandomSource {
    override fun nextInt(from:Int, until:Int) = value.coerceIn(from, until-1)
    override fun chance(percent:Int) = percent > value
}

class GameRulesTest {
    @Test fun `statistiques additionnent bonus puis pourcentage puis multiplicateur`() {
        val stats=StatCalculator.calculate(mapOf(StatKey.ATK to 10),listOf(StatModifier(StatKey.ATK,flat=5,percent=.20,multiplier=2.0)))
        assertEquals(36,stats[StatKey.ATK])
    }
    @Test fun `les chances sont bornees`() { val s=StatCalculator.calculate(mapOf(StatKey.CRIT_CHANCE to 90),listOf(StatModifier(StatKey.CRIT_CHANCE,flat=50)));assertEquals(100,s[StatKey.CRIT_CHANCE]) }
    @Test fun `degats respectent minimum maximum et critique`() {
        val player=Combatant("J",100,FinalStats(mapOf(StatKey.MIN_DAMAGE to 3,StatKey.MAX_DAMAGE to 9,StatKey.ATK to 4,StatKey.CRIT_CHANCE to 100,StatKey.CRIT_DAMAGE to 50,StatKey.AME to 0)))
        val monster=Combatant("M",100,FinalStats(mapOf(StatKey.RME to 0)))
        val r=CombatEngine.resolve(player,monster,SkillPower("Test",1.0,Rank.F),FixedRandom(3))
        assertTrue(r.critical);assertEquals(11,r.damage);assertEquals(89,r.targetHp)
    }
    @Test fun `mauvais effet tient compte ame rme`() {
        val p=Combatant("J",1,FinalStats(mapOf(StatKey.MIN_DAMAGE to 1,StatKey.MAX_DAMAGE to 1,StatKey.ATK to 0,StatKey.CRIT_CHANCE to 0,StatKey.CRIT_DAMAGE to 0,StatKey.AME to 30)))
        val m=Combatant("M",10,FinalStats(mapOf(StatKey.RME to 10)))
        val r=CombatEngine.resolve(p,m,SkillPower("S",1.0,Rank.F,listOf(EffectRule("Poison",EffectKind.BAD,10,1,emptyList()))),FixedRandom(19))
        assertEquals(1,r.appliedEffects.size)
    }
    @Test fun `progression declenche niveau`() { val r=ProgressionRules.gain(60,1,Rank.F,10);assertTrue(r.leveledUp);assertEquals(2,r.level) }
    @Test fun `irregularite retire xp sans valeur negative`() { assertEquals(0,ProgressionRules.irregular(8,1,Rank.F,IrregularityMode.PERTE_XP,20).xp) }
}
