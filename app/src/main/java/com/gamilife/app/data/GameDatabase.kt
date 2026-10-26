package com.gamilife.app.data

import android.content.Context
import androidx.room.*
import com.gamilife.app.domain.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "player")
data class PlayerEntity(@PrimaryKey val id: Int = 1, val name: String, val level: Int, val rank: String, val xp: Int, val hp: Int, val maxHp: Int,
    val atk: Int, val crit: Int, val critDamage: Int, val accuracy: Int, val ame: Int, val rme: Int)
@Entity(tableName = "skills")
data class SkillEntity(@PrimaryKey val id: Long = 0, val name: String, val description: String, val kind: String, val rank: String,
    val level: Int, val xp: Int, val rewardXp: Int, val power: Double, val lastUsed: Long, val cooldownHours: Int,
    val carDeadline: Long?, val irregularity: String, val enabled: Boolean = true)
@Entity(tableName = "items")
data class ItemEntity(@PrimaryKey val id: Long = 0, val name: String, val description: String, val category: String, val rarity: String,
    val quantity: Int, val slot: String?, val equipped: Boolean, val minDamage: Int, val maxDamage: Int, val atkBonus: Int, val hpBonus: Int)
@Entity(tableName = "monsters")
data class MonsterEntity(@PrimaryKey val id: Long = 0, val name: String, val description: String, val rank: String, val level: Int,
    val hp: Int, val maxHp: Int, val atk: Int, val minDamage: Int, val maxDamage: Int, val rme: Int, val rewardItem: String)
@Entity(tableName = "effects")
data class EffectEntity(@PrimaryKey val id: Long = 0, val name: String, val type: String, val description: String, val turns: Int,
    val stat: String, val flat: Int, val percent: Double, val multiplier: Double, val active: Boolean)
@Entity(tableName = "history")
data class HistoryEntity(@PrimaryKey(autoGenerate = true) val id: Long = 0, val createdAt: Long, val title: String, val details: String)
@Entity(tableName = "dungeons")
data class DungeonEntity(@PrimaryKey val id: Long = 0, val name: String, val description: String, val difficulty: Int, val currentStep: Int, val complete: Boolean)

@Dao interface GameDao {
    @Query("SELECT * FROM player WHERE id=1") fun playerFlow(): Flow<PlayerEntity?>
    @Query("SELECT * FROM skills ORDER BY kind, name") fun skillsFlow(): Flow<List<SkillEntity>>
    @Query("SELECT * FROM items ORDER BY equipped DESC, rarity, name") fun itemsFlow(): Flow<List<ItemEntity>>
    @Query("SELECT * FROM monsters ORDER BY level") fun monstersFlow(): Flow<List<MonsterEntity>>
    @Query("SELECT * FROM effects") fun effectsFlow(): Flow<List<EffectEntity>>
    @Query("SELECT * FROM dungeons") fun dungeonsFlow(): Flow<List<DungeonEntity>>
    @Query("SELECT * FROM history ORDER BY createdAt DESC LIMIT 40") fun historyFlow(): Flow<List<HistoryEntity>>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun putPlayer(value: PlayerEntity)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun putSkill(value: SkillEntity)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun putItem(value: ItemEntity)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun putMonster(value: MonsterEntity)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun putEffect(value: EffectEntity)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun putDungeon(value: DungeonEntity)
    @Insert suspend fun addHistory(value: HistoryEntity)
    @Query("SELECT * FROM player WHERE id=1") suspend fun player(): PlayerEntity?
    @Query("SELECT * FROM skills WHERE id=:id") suspend fun skill(id: Long): SkillEntity?
    @Query("SELECT * FROM monsters WHERE id=:id") suspend fun monster(id: Long): MonsterEntity?
    @Query("SELECT * FROM items WHERE id=:id") suspend fun item(id: Long): ItemEntity?
    @Query("UPDATE items SET equipped=0 WHERE slot=:slot") suspend fun unequipSlot(slot: String)
    @Query("SELECT COUNT(*) FROM player") suspend fun playerCount(): Int
}

@Database(entities = [PlayerEntity::class, SkillEntity::class, ItemEntity::class, MonsterEntity::class, EffectEntity::class, HistoryEntity::class, DungeonEntity::class], version = 1, exportSchema = true)
abstract class GameDatabase : RoomDatabase() { abstract fun dao(): GameDao
    companion object { fun create(context: Context) = Room.databaseBuilder(context, GameDatabase::class.java, "gamilife.db").build() }
}

data class GameSnapshot(val player: PlayerEntity?, val skills: List<SkillEntity>, val items: List<ItemEntity>, val monsters: List<MonsterEntity>, val effects: List<EffectEntity>, val dungeons: List<DungeonEntity>, val history: List<HistoryEntity>)

class GameRepository(private val db: GameDatabase) {
    private val dao = db.dao()
    private data class Primary(val player: PlayerEntity?, val skills: List<SkillEntity>, val items: List<ItemEntity>, val monsters: List<MonsterEntity>, val effects: List<EffectEntity>)
    val state: Flow<GameSnapshot> = kotlinx.coroutines.flow.combine(dao.playerFlow(), dao.skillsFlow(), dao.itemsFlow(), dao.monstersFlow(), dao.effectsFlow()) { p, s, i, m, e -> Primary(p,s,i,m,e) }
        .let { primary -> kotlinx.coroutines.flow.combine(primary, dao.dungeonsFlow(), dao.historyFlow()) { first, d, h -> GameSnapshot(first.player,first.skills,first.items,first.monsters,first.effects,d,h) } }
    suspend fun seed() { if (dao.playerCount() == 0) db.withTransaction {
        dao.putPlayer(PlayerEntity(name="Kenjimaru", level=1, rank="F", xp=0, hp=120, maxHp=120, atk=12, crit=15, critDamage=50, accuracy=90, ame=5, rme=5))
        dao.putSkill(SkillEntity(1,"Se réveiller avant 08:00","Valider avant 08:00 pour entretenir la régularité.","QUOTIDIENNE","C",1,0,30,1.25,0,24,null,"PERTE_XP"))
        dao.putSkill(SkillEntity(2,"Hydratation du jour","Boire 2 litres d’eau.","HABITUDE","D",1,0,20,1.0,0,12,null,"CONSERVE"))
        dao.putSkill(SkillEntity(3,"Session de concentration","Une session de travail ciblée de 25 minutes.","PONCTUELLE","B",1,0,40,1.55,0,0,null,"PERSONNALISE"))
        dao.putItem(ItemEntity(1,"Épée de l’Aube","Arme de démonstration : +3 ATK, dégâts 5–11.","Arme","Rare",1,"ARME",true,5,11,3,0))
        dao.putItem(ItemEntity(2,"Talisman du souffle","Augmente les PV maximum.","Bijou","Inhabituel",1,"BIJOU",false,0,0,0,25))
        dao.putItem(ItemEntity(3,"Potion de soin","Objet consommable de démonstration.","Consommable","Commun",3,null,false,0,0,0,0))
        dao.putMonster(MonsterEntity(1,"Gobelin des retards","Se nourrit des matins reportés.","F",1,75,75,6,3,8,2,"Potion de soin"))
        dao.putMonster(MonsterEntity(2,"Ombre de procrastination","Une forme lente qui grignote la concentration.","D",3,150,150,12,6,14,8,"Talisman du souffle"))
        dao.putMonster(MonsterEntity(3,"Dragon de l’échéance","Boss du donjon de démonstration.","B",6,280,280,21,12,24,15,"Épée de l’Aube"))
        dao.putEffect(EffectEntity(1,"Élan", "BE", "+10 ATK pendant 2 tours.",2,"ATK",10,0.0,1.0,true))
        dao.putEffect(EffectEntity(2,"Lenteur", "ME", "-10 % de précision pendant 1 tour.",1,"ACCURACY",0,-0.10,1.0,false))
        dao.putDungeon(DungeonEntity(1,"La Routine perdue","Trois ennemis symbolisent les obstacles du quotidien.",2,0,false))
        dao.addHistory(HistoryEntity(createdAt=System.currentTimeMillis(), title="Bienvenue dans Gamilife", details="Données de démonstration créées. Elles sont modifiables dans Administration."))
    } }
    suspend fun equip(itemId: Long): String = db.withTransaction {
        val item = dao.item(itemId) ?: return@withTransaction "Objet introuvable"
        if (item.slot == null) return@withTransaction "Cet objet ne peut pas être équipé"
        dao.unequipSlot(item.slot); dao.putItem(item.copy(equipped = true)); dao.addHistory(HistoryEntity(createdAt=System.currentTimeMillis(), title="Équipement", details="${item.name} équipé.")); "${item.name} équipé"
    }
    suspend fun validateSkill(id: Long, now: Long): String = db.withTransaction {
        val skill = dao.skill(id) ?: return@withTransaction "Compétence introuvable"
        val cooldown = skill.cooldownHours * 3_600_000L
        if (!skill.enabled) return@withTransaction "Compétence désactivée"
        if (skill.carDeadline != null && now > skill.carDeadline) return@withTransaction "CAR expiré : la règle d’irrégularité doit être appliquée"
        if (now - skill.lastUsed < cooldown) return@withTransaction "Compétence indisponible : délai en cours"
        val result = ProgressionRules.gain(skill.xp, skill.level, Rank.valueOf(skill.rank), skill.rewardXp)
        dao.putSkill(skill.copy(xp=result.xp, level=result.level, rank=result.rank.name, lastUsed=now))
        val player = dao.player()!!; val general = ProgressionRules.gain(player.xp, player.level, Rank.valueOf(player.rank), skill.rewardXp / 2)
        dao.putPlayer(player.copy(xp=general.xp, level=general.level, rank=general.rank.name))
        dao.addHistory(HistoryEntity(createdAt=now, title="Compétence validée", details="${skill.name} : +${skill.rewardXp} XP."))
        "${skill.name} validée : +${skill.rewardXp} XP"
    }
    suspend fun fight(skillId: Long, monsterId: Long): CombatResolution? = db.withTransaction {
        val p = dao.player() ?: return@withTransaction null; val s = dao.skill(skillId) ?: return@withTransaction null; val m = dao.monster(monsterId) ?: return@withTransaction null
        val equipped = listOfNotNull(dao.item(1), dao.item(2)).filter { it.equipped }
        val stats = StatCalculator.calculate(mapOf(StatKey.HP to p.maxHp, StatKey.ATK to p.atk, StatKey.CRIT_CHANCE to p.crit, StatKey.CRIT_DAMAGE to p.critDamage, StatKey.ACCURACY to p.accuracy, StatKey.AME to p.ame, StatKey.RME to p.rme, StatKey.MIN_DAMAGE to 1, StatKey.MAX_DAMAGE to 4), equipped.flatMap { listOf(StatModifier(StatKey.ATK,it.atkBonus),StatModifier(StatKey.HP,it.hpBonus),StatModifier(StatKey.MIN_DAMAGE,it.minDamage),StatModifier(StatKey.MAX_DAMAGE,it.maxDamage)) })
        val monsterStats = StatCalculator.calculate(mapOf(StatKey.HP to m.maxHp,StatKey.ATK to m.atk,StatKey.MIN_DAMAGE to m.minDamage,StatKey.MAX_DAMAGE to m.maxDamage,StatKey.RME to m.rme),emptyList())
        val res = CombatEngine.resolve(Combatant(p.name,p.hp,stats), Combatant(m.name,m.hp,monsterStats), SkillPower(s.name,s.power,Rank.valueOf(s.rank),listOf(EffectRule("Lenteur",EffectKind.BAD,30,1,listOf()))), DefaultRandom)
        dao.putMonster(m.copy(hp=res.targetHp)); dao.addHistory(HistoryEntity(createdAt=System.currentTimeMillis(),title="Combat",details=res.log))
        if (res.targetHp == 0) { dao.putItem(ItemEntity(99,"Récompense : ${m.rewardItem}","Butin obtenu sur ${m.name}","Butin","Rare",1,null,false,0,0,0,0)); dao.addHistory(HistoryEntity(createdAt=System.currentTimeMillis(),title="Victoire",details="${m.name} vaincu. Butin attribué : ${m.rewardItem}.")) }
        res
    }
    suspend fun applyIrregularity(id: Long): String = db.withTransaction { val s=dao.skill(id) ?: return@withTransaction "Compétence introuvable"; val r=ProgressionRules.irregular(s.xp,s.level,Rank.valueOf(s.rank),IrregularityMode.valueOf(s.irregularity),15); dao.putSkill(s.copy(xp=r.xp,level=r.level,rank=r.rank.name)); dao.addHistory(HistoryEntity(createdAt=System.currentTimeMillis(),title="Irrégularité",details="${s.name} : règle ${s.irregularity} appliquée.")); "Règle d’irrégularité appliquée" }
}
