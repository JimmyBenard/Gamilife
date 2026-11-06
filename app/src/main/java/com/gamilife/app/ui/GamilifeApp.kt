@file:OptIn(ExperimentalMaterial3Api::class)

package com.gamilife.app

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gamilife.app.data.*
import com.gamilife.app.domain.*

val GamilifeColors = darkColorScheme(primary=Color(0xFFFFC857), secondary=Color(0xFF8BD3DD), tertiary=Color(0xFFC6A0F6), background=Color(0xFF111A29), surface=Color(0xFF1B2A41), surfaceVariant=Color(0xFF263A55), onPrimary=Color(0xFF17100A), onBackground=Color(0xFFF0F4FA), onSurface=Color(0xFFF0F4FA))

private enum class Screen(val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    CHARACTER("Personnage", Icons.Default.Person), SKILLS("Compétences",Icons.Default.AutoAwesome), DUNGEONS("Donjons",Icons.Default.Castle), BESTIARY("Bestiaire",Icons.Default.MenuBook), COMBAT("Combat",Icons.Default.SportsMartialArts), INVENTORY("Inventaire",Icons.Default.Inventory2), EQUIPMENT("Équipement",Icons.Default.Shield), EFFECTS("Effets",Icons.Default.Bolt), COLLECTION("Collection",Icons.Default.CollectionsBookmark), FORGE("Forge",Icons.Default.Build), SETTINGS("Paramètres",Icons.Default.Settings), ADMIN("Administration",Icons.Default.AdminPanelSettings)
}

@Composable fun GamilifeApp(game: GameSnapshot, vm: GameViewModel) {
    var screen by rememberSaveable { mutableStateOf(Screen.CHARACTER) }
    var selectedSkill by remember { mutableStateOf<SkillEntity?>(null) }; var selectedMonster by remember { mutableStateOf<MonsterEntity?>(null) }
    Box(Modifier.fillMaxSize()) {
        if (screen == Screen.CHARACTER) {
            CharacterView(game, onNavigate = { screen = it }, onEquipment = { screen = Screen.EQUIPMENT })
        } else Scaffold(
            topBar = { TopAppBar(title={ Column { Text("GAMILIFE",fontWeight=FontWeight.Black); Text("V0.1.5 · RPG de la vie réelle",style=MaterialTheme.typography.labelSmall) } }, actions={ IconButton({ screen=Screen.SETTINGS }) { Icon(Icons.Default.Settings,"Paramètres") } }) },
            bottomBar = { GameNavigationBar(screen) { screen = it } },
            snackbarHost = { SnackbarHost(remember { SnackbarHostState() }) }
        ) { padding -> Box(Modifier.fillMaxSize().padding(padding)) {
        when(screen) {
            Screen.CHARACTER -> Unit
            Screen.SKILLS -> SkillsView(game.skills,{selectedSkill=it},{vm.validate(it.id)})
            Screen.DUNGEONS -> DungeonsView(game.dungeons,game.monsters,{ screen=Screen.COMBAT; selectedMonster=it })
            Screen.BESTIARY -> BestiaryView(game.monsters,{selectedMonster=it})
            Screen.COMBAT -> CombatView(game,selectedMonster,selectedSkill,{ selectedMonster=it },{ selectedSkill=it },{ skill,monster -> vm.fight(skill.id,monster.id) })
            Screen.INVENTORY -> InventoryView(game.items,{vm.equip(it.id)},{screen=Screen.EQUIPMENT})
            Screen.EQUIPMENT -> EquipmentView(game.items,{vm.equip(it.id)})
            Screen.EFFECTS -> EffectsView(game.effects)
            Screen.COLLECTION -> CollectionView(game.items)
            Screen.FORGE -> ForgeView()
            Screen.SETTINGS -> SettingsView(vm.adminMode, vm::updateAdminMode, { screen=Screen.ADMIN })
            Screen.ADMIN -> if (vm.adminMode) AdminView(game,{screen=Screen.EFFECTS},{vm.irregular(it.id)},vm::updatePlayer) else SettingsView(vm.adminMode, vm::updateAdminMode, { screen=Screen.ADMIN })
        }
        } }
        if (screen == Screen.CHARACTER) Box(Modifier.align(Alignment.BottomCenter)) { GameNavigationBar(screen) { screen = it } }
        vm.message?.let { text -> AlertDialog(onDismissRequest=vm::dismiss,title={Text("Gamilife")},text={Text(text)},confirmButton={TextButton(vm::dismiss){Text("Compris")}}) }
        selectedSkill?.let { SkillDetail(it,onBack={selectedSkill=null},onValidate={vm.validate(it.id)}) }
        selectedMonster?.let { MonsterDetail(it,onBack={selectedMonster=null},onFight={screen=Screen.COMBAT; selectedMonster=it}) }
    }
}

@Composable private fun GameNavigationBar(active: Screen, onNavigate: (Screen) -> Unit) {
    val tabs = listOf(
        Triple(Screen.CHARACTER, "Perso", Icons.Default.Person),
        Triple(Screen.SKILLS, "Compét.", Icons.Default.AutoAwesome),
        Triple(Screen.DUNGEONS, "Donjons", Icons.Default.Castle),
        Triple(Screen.INVENTORY, "Sac", Icons.Default.Inventory2),
        Triple(Screen.BESTIARY, "Bestiaire", Icons.Default.Pets),
        Triple(Screen.COLLECTION, "Collection", Icons.Default.CollectionsBookmark),
        Triple(Screen.FORGE, "Forge", Icons.Default.Build)
    )
    val visualActive = if (active == Screen.EQUIPMENT) Screen.INVENTORY else active
    Surface(color=Color(0xFF08111F), shadowElevation=12.dp) {
        Row(Modifier.fillMaxWidth().height(78.dp).padding(horizontal=5.dp, vertical=6.dp), horizontalArrangement=Arrangement.spacedBy(4.dp)) {
            tabs.forEach { (destination, label, icon) ->
                GamingNavigationItem(label, icon, destination == visualActive) { onNavigate(destination) }
            }
        }
    }
}

@Composable private fun RowScope.GamingNavigationItem(label:String, icon:androidx.compose.ui.graphics.vector.ImageVector, active:Boolean, onClick:()->Unit) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) .90f else if (active) 1.035f else 1f, animationSpec=tween(110), label="navScale")
    val glow by animateFloatAsState(if (active) 1f else 0f, animationSpec=tween(180), label="navGlow")
    val shape = RoundedCornerShape(12.dp)
    Column(
        Modifier.weight(1f).fillMaxHeight().graphicsLayer { scaleX=scale; scaleY=scale; shadowElevation=if (active) 10f else 2f }
            .clip(shape)
            .background(if (active) Brush.verticalGradient(listOf(Color(0xFFFFE39A),Color(0xFFB97812),Color(0xFF5B3607))) else Brush.verticalGradient(listOf(Color(0xFF253A5B),Color(0xFF101B31))))
            .border(1.dp, if (active) Color(0xFFFFF0B5) else Color(0xFF637CA7), shape)
            .clickable(interactionSource=interaction, indication=null, onClick=onClick)
            .padding(horizontal=2.dp, vertical=4.dp),
        horizontalAlignment=Alignment.CenterHorizontally,
        verticalArrangement=Arrangement.Center
    ) {
        if (active) Box(Modifier.width(22.dp).height(2.dp).clip(RoundedCornerShape(2.dp)).background(Color(0xFFFFF3C8).copy(alpha=glow)))
        Icon(icon, label, Modifier.size(25.dp), tint=if (active) Color(0xFF231704) else Color(0xFFC8D8F5))
        Text(label, maxLines=1, overflow=TextOverflow.Ellipsis, color=if (active) Color(0xFF241704) else Color(0xFFE8F0FF), fontSize=8.sp, fontWeight=FontWeight.Bold, textAlign=TextAlign.Center)
    }
}

@Composable private fun CharacterView(g: GameSnapshot, onNavigate:(Screen)->Unit, onEquipment:()->Unit) {
    val p = g.player ?: return Loading()
    val equipped = g.items.filter { it.equipped }
    val bonusAtk = equipped.sumOf { it.atkBonus }
    val bonusHp = equipped.sumOf { it.hpBonus }
    val maxHp = p.maxHp + bonusHp
    val maxXp = ProgressionRules.xpToNext(p.level, Rank.valueOf(p.rank)).coerceAtLeast(1)
    var revealed by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { revealed = true }
    val entry by animateFloatAsState(if(revealed) 1f else .96f, tween(360), label="heroEntry")
    val alpha by animateFloatAsState(if(revealed) 1f else 0f, tween(280), label="heroFade")
    LazyColumn(
        Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF07101E),Color(0xFF162C49),Color(0xFF091423)))),
        contentPadding=PaddingValues(start=12.dp,end=12.dp,top=14.dp,bottom=96.dp), verticalArrangement=Arrangement.spacedBy(10.dp)
    ) {
        item {
            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) { Text("GAMILIFE",color=Color(0xFFFFD879),fontSize=13.sp,fontWeight=FontWeight.Black,letterSpacing=1.sp); Text("FICHE DU HÉROS",fontSize=24.sp,fontWeight=FontWeight.Black) }
                IconButton({onNavigate(Screen.SETTINGS)},Modifier.border(1.dp,Color(0xFFE7BD63),RoundedCornerShape(12.dp)).background(Color(0xFF152842),RoundedCornerShape(12.dp))) { Icon(Icons.Default.Settings,"Paramètres",tint=Color(0xFFFFD879)) }
            }
        }
        item {
            GamingPanel(Modifier.graphicsLayer { scaleX=entry; scaleY=entry; this.alpha=alpha }) {
                Row(verticalAlignment=Alignment.CenterVertically) {
                    Box(Modifier.size(82.dp).border(2.dp,Color(0xFFFFCD6A),RoundedCornerShape(25.dp)).background(Brush.radialGradient(listOf(Color(0xFF3C6CA3),Color(0xFF0D1C33))),RoundedCornerShape(25.dp)),contentAlignment=Alignment.Center) { Icon(Icons.Default.Person,null,Modifier.size(52.dp),tint=Color(0xFFC7D8F4)) }
                    Column(Modifier.padding(start=12.dp).weight(1f)) {
                        Text(p.name,fontSize=21.sp,fontWeight=FontWeight.Black,maxLines=1,overflow=TextOverflow.Ellipsis)
                        Row(verticalAlignment=Alignment.CenterVertically) { RankBadge(p.rank); Spacer(Modifier.width(7.dp)); Text("Niveau ${p.level}",color=Color(0xFFBFD2F5),fontWeight=FontWeight.Bold) }
                        Spacer(Modifier.height(7.dp)); Text("EXP  ${p.xp} / $maxXp",style=MaterialTheme.typography.labelSmall,color=Color(0xFFD8E5FB)); LinearProgressIndicator(progress={p.xp.toFloat().coerceAtMost(maxXp.toFloat())/maxXp},Modifier.fillMaxWidth().height(7.dp).clip(RoundedCornerShape(8.dp)),color=Color(0xFFFFC857),trackColor=Color(0xFF263D5B))
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text("PV  ${p.hp} / $maxHp",fontWeight=FontWeight.Black,color=Color(0xFFFFD6DB)); LinearProgressIndicator(progress={if(maxHp==0) 0f else p.hp.toFloat()/maxHp},Modifier.fillMaxWidth().height(11.dp).clip(RoundedCornerShape(10.dp)),color=Color(0xFFFF596D),trackColor=Color(0xFF54233A))
            }
        }
        item { GamingTitle("STATISTIQUES",Icons.Default.BarChart) }
        item {
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(9.dp)) {
                GamingPanel(Modifier.weight(1f)) { Text("OFFENSIVES",color=Color(0xFFFFC857),fontWeight=FontWeight.Black); CharacterStat("ATK",p.atk,bonusAtk); CharacterStat("CC",p.crit,0,"%"); CharacterStat("DC",p.critDamage,0,"%"); CharacterStat("PRE",p.accuracy,0,"%") }
                GamingPanel(Modifier.weight(1f)) { Text("DÉFENSIVES",color=Color(0xFF8BD3DD),fontWeight=FontWeight.Black); CharacterStat("DEF",0,0); CharacterStat("RC",0,0,"%"); CharacterStat("RDC",0,0,"%"); CharacterStat("ESQ",0,0,"%") }
            }
        }
        item { GamingTitle("ÉQUIPEMENTS",Icons.Default.Shield) }
        item {
            GamingPanel {
                Text("Bonus équipés : ATK +$bonusAtk · PV max +$bonusHp",style=MaterialTheme.typography.labelSmall,color=Color(0xFFC8D9F4))
                Spacer(Modifier.height(9.dp))
                val slots=listOf("Chapeau" to "CHAPEAU","Masque" to "MASQUE","Arme principale" to "ARME","Arme secondaire" to "SECONDAIRE","Gants" to "GANTS","Chaussures" to "CHAUSSURES","Costume" to "ARMURE","Anneau" to "ANNEAU","Bracelet" to "BRACELET","Collier" to "COLLIER","Talisman" to "TALISMAN")
                slots.chunked(2).forEach { row -> Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)) { row.forEach { (label,slot) -> val item=equipped.firstOrNull { it.slot==slot || (slot=="ARME" && it.slot=="ARME") }; EquipmentTile(label,item?.name,Modifier.weight(1f),onEquipment) }; if(row.size==1) Spacer(Modifier.weight(1f)) }; Spacer(Modifier.height(8.dp)) }
                OutlinedButton(onEquipment,Modifier.fillMaxWidth(),border=BorderStroke(1.dp,Brush.linearGradient(listOf(Color(0xFFFFD36D),Color(0xFF607EAF))))) { Icon(Icons.Default.Inventory2,null,tint=Color(0xFFFFD36D)); Spacer(Modifier.width(8.dp)); Text("GÉRER L’ÉQUIPEMENT",fontWeight=FontWeight.Black) }
            }
        }
    }
}

@Composable private fun GamingPanel(modifier:Modifier=Modifier,content:@Composable ColumnScope.()->Unit) = Column(modifier.border(1.dp,Color(0xFF6387BC),RoundedCornerShape(18.dp)).background(Brush.verticalGradient(listOf(Color(0xEE1B3557),Color(0xEE0D1C32))),RoundedCornerShape(18.dp)).padding(12.dp),content=content)
@Composable private fun GamingTitle(text:String,icon:androidx.compose.ui.graphics.vector.ImageVector) = Row(verticalAlignment=Alignment.CenterVertically) { Icon(icon,null,tint=Color(0xFFFFC857)); Spacer(Modifier.width(7.dp)); Text(text,fontWeight=FontWeight.Black,fontSize=15.sp,color=Color(0xFFFFE0A0),letterSpacing=1.sp); Spacer(Modifier.width(8.dp)); Box(Modifier.weight(1f).height(1.dp).background(Color(0xFF6B86B2))) }
@Composable private fun CharacterStat(label:String,base:Int,bonus:Int,suffix:String="") = Row(Modifier.fillMaxWidth().padding(top=7.dp),horizontalArrangement=Arrangement.SpaceBetween) { Text(label,fontWeight=FontWeight.Bold,color=Color(0xFFDCE8FA)); Text("${base+bonus}$suffix",fontWeight=FontWeight.Black); if(bonus!=0) Text("+$bonus",fontSize=10.sp,color=Color(0xFF7DDF9D)) }
@Composable private fun EquipmentTile(label:String,item:String?,modifier:Modifier,onClick:()->Unit) { val source=remember { MutableInteractionSource() }; val pressed by source.collectIsPressedAsState(); val scale by animateFloatAsState(if(pressed).94f else 1f,tween(100),label="equipmentPress"); Column(modifier.graphicsLayer { scaleX=scale; scaleY=scale }.border(1.dp,if(item==null) Color(0xFF496587) else Color(0xFFFFC857),RoundedCornerShape(11.dp)).background(Color(0xFF0B192C),RoundedCornerShape(11.dp)).clickable(interactionSource=source,indication=null,onClick=onClick).padding(8.dp),horizontalAlignment=Alignment.CenterHorizontally) { Icon(if(item==null) Icons.Default.AddCircle else Icons.Default.CheckCircle,null,Modifier.size(21.dp),tint=if(item==null) Color(0xFF7895BD) else Color(0xFFFFC857)); Text(label,maxLines=1,overflow=TextOverflow.Ellipsis,fontSize=10.sp,fontWeight=FontWeight.Bold); Text(item ?: "Vide",maxLines=1,overflow=TextOverflow.Ellipsis,fontSize=9.sp,color=if(item==null) Color(0xFF9FB1CD) else Color(0xFF8EE1A1)) } }
@Composable private fun SkillsView(skills:List<SkillEntity>,detail:(SkillEntity)->Unit,validate:(SkillEntity)->Unit) = Page("Compétences","Accomplis tes actions pour devenir plus fort") { if(skills.isEmpty()) Text("Aucune compétence") else skills.forEach { s -> Card(Modifier.fillMaxWidth().padding(vertical=4.dp).clickable{detail(s)}) { Column(Modifier.padding(14.dp)) { Row(verticalAlignment=Alignment.CenterVertically){ RankBadge(s.rank); Column(Modifier.padding(start=10.dp).weight(1f)){Text(s.name,fontWeight=FontWeight.Bold);Text(s.kind.lowercase().replaceFirstChar{it.uppercase()},style=MaterialTheme.typography.labelSmall)}; IconButton({detail(s)}){Icon(Icons.Default.ChevronRight,null)} }; XpBar(s.xp,ProgressionRules.xpToNext(s.level,Rank.valueOf(s.rank))); Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){Text("Niv. ${s.level} · +${s.rewardXp} XP",style=MaterialTheme.typography.labelMedium); Button({validate(s)}){Text("Valider")}} } } } }
@Composable private fun DungeonsView(dungeons:List<DungeonEntity>, monsters:List<MonsterEntity>,fight:(MonsterEntity)->Unit) = Page("Donjons","Défis de la vie réelle, affrontements successifs") { dungeons.forEach { d -> HeroCard(d.name,"Difficulté ${d.difficulty} · Étape ${d.currentStep+1} · ${if(d.complete) "Terminé" else "Disponible"}",Icons.Default.Castle,Color(0xFFFF8A65)); Text(d.description); Spacer(Modifier.height(6.dp)); monsters.forEachIndexed{ index,m -> ListItem(headlineContent={Text("${index+1}. ${m.name}")},supportingContent={Text("PV ${m.hp}/${m.maxHp} · Rang ${m.rank}")},trailingContent={FilledTonalButton({fight(m)}){Text("Combattre")}}) } } }
@Composable private fun BestiaryView(monsters:List<MonsterEntity>,detail:(MonsterEntity)->Unit) = Page("Bestiaire","Ennemis locaux administrables") { monsters.forEach { m -> Card(Modifier.fillMaxWidth().padding(vertical=4.dp).clickable{detail(m)}) { Row(Modifier.padding(14.dp),verticalAlignment=Alignment.CenterVertically){ Icon(Icons.Default.Pets,null,Modifier.size(38.dp),tint=Color(0xFFFFC857)); Column(Modifier.weight(1f).padding(start=12.dp)){Text(m.name,fontWeight=FontWeight.Bold);Text("Rang ${m.rank} · niveau ${m.level} · ${m.hp}/${m.maxHp} PV")};Icon(Icons.Default.ChevronRight,null) } } } }
@Composable private fun CombatView(g:GameSnapshot, chosenMonster:MonsterEntity?, chosenSkill:SkillEntity?, chooseMonster:(MonsterEntity)->Unit,chooseSkill:(SkillEntity)->Unit,fight:(SkillEntity,MonsterEntity)->Unit) { val p=g.player ?: return Loading(); val m=chosenMonster ?: g.monsters.firstOrNull(); val s=chosenSkill ?: g.skills.firstOrNull(); Page("Combat","Une compétence validée devient une action offensive") { if(m==null || s==null) Text("Les données de combat arrivent…") else { HeroCard("${p.name}  VS  ${m.name}","Tour du joueur · sélectionne une carte puis attaque",Icons.Default.SportsMartialArts,Color(0xFFE57373)); Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(10.dp)){ Column(Modifier.weight(1f)){Text(p.name,fontWeight=FontWeight.Bold);HpBar(p.hp,p.maxHp)};Column(Modifier.weight(1f)){Text(m.name,fontWeight=FontWeight.Bold);HpBar(m.hp,m.maxHp)} }; Section("Carte choisie") { Text("${s.name} · rang ${s.rank} · puissance ×${s.power}"); Button({fight(s,m)},Modifier.fillMaxWidth()){Icon(Icons.Default.FlashOn,null);Spacer(Modifier.width(6.dp));Text("Utiliser la compétence")}}; Section("Changer de cible") { g.monsters.forEach{ monster -> FilterChip(selected=m.id==monster.id,onClick={chooseMonster(monster)},label={Text(monster.name)}) } }; Section("Changer de compétence") { g.skills.forEach{ skill -> FilterChip(selected=s.id==skill.id,onClick={chooseSkill(skill)},label={Text(skill.name)},modifier=Modifier.padding(end=5.dp)) } }; Section("Journal") { g.history.take(6).forEach{ Text("• ${it.details}",style=MaterialTheme.typography.bodySmall) } } } }
}
@Composable private fun InventoryView(items:List<ItemEntity>,equip:(ItemEntity)->Unit,equipment:()->Unit) = Page("Inventaire","Objets persistants, filtres et équipement") { Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.End){TextButton(equipment){Text("Voir l’équipement")}}; items.forEach { item -> Card(Modifier.fillMaxWidth().padding(vertical=4.dp)) { Row(Modifier.padding(12.dp),verticalAlignment=Alignment.CenterVertically){ Icon(if(item.equipped) Icons.Default.CheckCircle else Icons.Default.Inventory2,null,tint=if(item.equipped) Color(0xFF7DDC8C) else Color(0xFFFFC857));Column(Modifier.weight(1f).padding(start=10.dp)){Text(item.name,fontWeight=FontWeight.Bold,maxLines=1,overflow=TextOverflow.Ellipsis);Text("${item.category} · ${item.rarity} · x${item.quantity}",style=MaterialTheme.typography.labelSmall);Text(item.description,style=MaterialTheme.typography.bodySmall,maxLines=2,overflow=TextOverflow.Ellipsis)}; if(item.slot!=null) OutlinedButton({equip(item)}){Text(if(item.equipped) "Porté" else "Équiper")} } } } }
@Composable private fun EquipmentView(items:List<ItemEntity>,equip:(ItemEntity)->Unit) = Page("Équipement","Un seul objet compatible par emplacement") { val slots=listOf("ARME","BIJOU","ARMURE","CHAUSSURES"); slots.forEach { slot -> val equipped=items.firstOrNull{it.equipped&&it.slot==slot}; Section(slot.lowercase().replaceFirstChar{it.uppercase()}) { if(equipped==null) Text("Emplacement vide") else { ItemLine(equipped); Text("Dégâts ${equipped.minDamage}–${equipped.maxDamage} · ATK +${equipped.atkBonus}") }; items.filter{it.slot==slot&&!it.equipped}.forEach { candidate -> TextButton({equip(candidate)}){Text("Équiper ${candidate.name}")} } } } }
@Composable private fun EffectsView(effects:List<EffectEntity>) = Page("Effets","Fenêtre des bons effets (BE) et mauvais effets (ME)") { effects.forEach { e -> Card(Modifier.fillMaxWidth().padding(vertical=4.dp)){Row(Modifier.padding(14.dp),verticalAlignment=Alignment.CenterVertically){AssistChip(onClick={},label={Text(e.type)},colors=AssistChipDefaults.assistChipColors(labelColor=if(e.type=="BE")Color(0xFF7DDC8C) else Color(0xFFFF8A80)));Column(Modifier.padding(start=10.dp)){Text(e.name,fontWeight=FontWeight.Bold);Text(e.description);Text("${if(e.active) "Actif" else "Inactif"} · ${e.turns} tour(s) · ${e.stat}",style=MaterialTheme.typography.labelSmall)}}} }}
@Composable private fun CollectionView(items:List<ItemEntity>) = Page("Collection","Éléments découverts et récompenses") { Text("${items.size} objets connus"); items.forEach{ ItemLine(it) } }
@Composable private fun ForgeView() = Page("Forge","Socle persistant prêt pour les prochaines règles") { HeroCard("Forge V0.1.0","Préparation d’amélioration d’équipement. Aucune économie fictive n’est simulée.",Icons.Default.Build,Color(0xFFFFC857));Text("Dans une version suivante, les recettes, matériaux et probabilités seront administrables.") }
@Composable private fun SettingsView(adminMode:Boolean,onModeChange:(Boolean)->Unit,openAdmin:()->Unit) = Page("Paramètres","Contrôle local de Gamilife") {
    HeroCard("Mode administrateur","Déverrouille l’édition directe et persistante des données du personnage.",Icons.Default.AdminPanelSettings,Color(0xFFC6A0F6))
    Section("Accès administrateur") {
        Row(Modifier.fillMaxWidth(), verticalAlignment=Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text("Mode administrateur",fontWeight=FontWeight.Bold); Text(if(adminMode) "Actif : les contrôles d’édition sont disponibles." else "Désactivé : protège les données de jeu.",style=MaterialTheme.typography.bodySmall) }; Switch(adminMode,onModeChange) }
        if(adminMode) Button(openAdmin,Modifier.fillMaxWidth().padding(top=10.dp)) { Icon(Icons.Default.Edit,null); Spacer(Modifier.width(8.dp)); Text("Ouvrir l’administration") }
    }
    Section("Données locales") { Text("Les réglages et les modifications sont enregistrés sur cet appareil, même après fermeture de l’application.") }
}

@Composable private fun AdminView(g:GameSnapshot,effects:()->Unit,irregular:(SkillEntity)->Unit,savePlayer:(PlayerEntity)->Unit) {
    val p=g.player ?: return Loading()
    var name by remember(p.name) { mutableStateOf(p.name) }; var level by remember(p.level) { mutableStateOf(p.level.toString()) }; var rank by remember(p.rank) { mutableStateOf(p.rank) }
    var xp by remember(p.xp) { mutableStateOf(p.xp.toString()) }; var hp by remember(p.hp) { mutableStateOf(p.hp.toString()) }; var maxHp by remember(p.maxHp) { mutableStateOf(p.maxHp.toString()) }
    var atk by remember(p.atk) { mutableStateOf(p.atk.toString()) }; var crit by remember(p.crit) { mutableStateOf(p.crit.toString()) }; var critDamage by remember(p.critDamage) { mutableStateOf(p.critDamage.toString()) }; var accuracy by remember(p.accuracy) { mutableStateOf(p.accuracy.toString()) }; var ame by remember(p.ame) { mutableStateOf(p.ame.toString()) }; var rme by remember(p.rme) { mutableStateOf(p.rme.toString()) }
    fun n(value:String, fallback:Int)=value.toIntOrNull() ?: fallback
    Page("Administration","Édition persistante des données de jeu") {
        HeroCard("Contrôle administrateur","Les valeurs saisies sont validées puis enregistrées dans la base locale.",Icons.Default.AdminPanelSettings,Color(0xFFC6A0F6))
        Section("Fiche personnage") {
            OutlinedTextField(name,{name=it},label={Text("Pseudonyme")},singleLine=true,modifier=Modifier.fillMaxWidth())
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)) { OutlinedTextField(level,{level=it},label={Text("Niveau")},singleLine=true,modifier=Modifier.weight(1f)); OutlinedTextField(rank,{rank=it.uppercase().take(1)},label={Text("Rang F–S")},singleLine=true,modifier=Modifier.weight(1f)) }
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)) { OutlinedTextField(xp,{xp=it},label={Text("EXP")},singleLine=true,modifier=Modifier.weight(1f)); OutlinedTextField(hp,{hp=it},label={Text("PV actuels")},singleLine=true,modifier=Modifier.weight(1f)); OutlinedTextField(maxHp,{maxHp=it},label={Text("PV max")},singleLine=true,modifier=Modifier.weight(1f)) }
            OutlinedTextField(atk,{atk=it},label={Text("ATK")},singleLine=true,modifier=Modifier.fillMaxWidth())
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)) { OutlinedTextField(crit,{crit=it},label={Text("CC %")},singleLine=true,modifier=Modifier.weight(1f)); OutlinedTextField(critDamage,{critDamage=it},label={Text("DC %")},singleLine=true,modifier=Modifier.weight(1f)); OutlinedTextField(accuracy,{accuracy=it},label={Text("PRE %")},singleLine=true,modifier=Modifier.weight(1f)) }
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)) { OutlinedTextField(ame,{ame=it},label={Text("AME %")},singleLine=true,modifier=Modifier.weight(1f)); OutlinedTextField(rme,{rme=it},label={Text("RME %")},singleLine=true,modifier=Modifier.weight(1f)) }
            Button({savePlayer(p.copy(name=name,level=n(level,p.level),rank=rank,xp=n(xp,p.xp),hp=n(hp,p.hp),maxHp=n(maxHp,p.maxHp),atk=n(atk,p.atk),crit=n(crit,p.crit),critDamage=n(critDamage,p.critDamage),accuracy=n(accuracy,p.accuracy),ame=n(ame,p.ame),rme=n(rme,p.rme)))},Modifier.fillMaxWidth().padding(top=10.dp)){Icon(Icons.Default.Save,null);Spacer(Modifier.width(8.dp));Text("Enregistrer le profil")}
        }
        Section("Règles et effets") { Text("Les règles d’irrégularité et les effets actifs restent accessibles pour vérification."); Button(effects){Text("Voir les effets")}}
        Section("Tester une irrégularité") { g.skills.forEach{ s -> OutlinedButton({irregular(s)},Modifier.fillMaxWidth().padding(vertical=3.dp)){Text("Appliquer ${s.irregularity} : ${s.name}")} } }
        Section("Historique persistant") { g.history.take(10).forEach{ Text("${it.title} — ${it.details}",style=MaterialTheme.typography.bodySmall,modifier=Modifier.padding(vertical=3.dp)) } }
    }
}

@Composable private fun SkillDetail(s:SkillEntity,onBack:()->Unit,onValidate:()->Unit) = DetailSheet("Détail d’une compétence",onBack) { RankBadge(s.rank); Text(s.name,style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold);Text(s.description);Section("Progression"){Text("Niveau ${s.level} · ${s.xp}/${ProgressionRules.xpToNext(s.level,Rank.valueOf(s.rank))} XP");XpBar(s.xp,ProgressionRules.xpToNext(s.level,Rank.valueOf(s.rank)))};Section("Règles"){Text("Catégorie : ${s.kind}");Text("Fréquence : toutes les ${s.cooldownHours} h");Text("Irrégularité : ${s.irregularity}");Text("Puissance : ×${s.power}")};Button(onValidate,Modifier.fillMaxWidth()){Text("Valider l’action réelle")}}
@Composable private fun MonsterDetail(m:MonsterEntity,onBack:()->Unit,onFight:()->Unit) = DetailSheet("Détail d’un monstre",onBack){RankBadge(m.rank);Text(m.name,style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold);Text(m.description);HpBar(m.hp,m.maxHp);Section("Statistiques"){Text("Niveau ${m.level} · ATK ${m.atk}");Text("Dégâts ${m.minDamage}–${m.maxDamage} · RME ${m.rme}");Text("Récompense : ${m.rewardItem}")};Button(onFight,Modifier.fillMaxWidth()){Text("Lancer le combat")}}
@Composable private fun DetailSheet(title:String,onBack:()->Unit,content:@Composable ColumnScope.()->Unit) = Surface(Modifier.fillMaxSize(),color=MaterialTheme.colorScheme.background){Column(Modifier.padding(20.dp)){Row(verticalAlignment=Alignment.CenterVertically){IconButton(onBack){Icon(Icons.AutoMirrored.Filled.ArrowBack,"Retour")};Text(title,style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Bold)};Spacer(Modifier.height(10.dp));content()}}
@Composable private fun Page(title:String,subtitle:String,content:@Composable ColumnScope.()->Unit)=LazyColumn(Modifier.fillMaxSize().padding(horizontal=16.dp),contentPadding=PaddingValues(bottom=90.dp)){item{Spacer(Modifier.height(16.dp));Text(title,style=MaterialTheme.typography.headlineMedium,fontWeight=FontWeight.Black);Text(subtitle,color=MaterialTheme.colorScheme.onSurfaceVariant);Spacer(Modifier.height(12.dp))};item{Column(content=content)}}
@Composable private fun Section(title:String,content:@Composable ColumnScope.()->Unit){Spacer(Modifier.height(12.dp));Text(title,style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Bold);Card(Modifier.fillMaxWidth().padding(top=6.dp)){Column(Modifier.padding(12.dp),content=content)}}
@Composable private fun HeroCard(title:String,subtitle:String,icon:androidx.compose.ui.graphics.vector.ImageVector,color:Color){Card(Modifier.fillMaxWidth()){Row(Modifier.padding(16.dp),verticalAlignment=Alignment.CenterVertically){Box(Modifier.size(52.dp).clip(RoundedCornerShape(16.dp)).background(color),contentAlignment=Alignment.Center){Icon(icon,null,tint=Color(0xFF172033))};Column(Modifier.padding(start=12.dp)){Text(title,fontWeight=FontWeight.Black);Text(subtitle,style=MaterialTheme.typography.bodySmall)}}}}
@Composable private fun HpBar(current:Int,max:Int){Text("PV $current / $max",style=MaterialTheme.typography.labelSmall);LinearProgressIndicator(progress={if(max==0)0f else current.toFloat()/max},Modifier.fillMaxWidth().height(8.dp),color=Color(0xFFE57373),trackColor=Color(0xFF5A2930))}
@Composable private fun XpBar(current:Int,max:Int){Text("XP $current / $max",style=MaterialTheme.typography.labelSmall);LinearProgressIndicator(progress={if(max==0)0f else current.toFloat()/max},Modifier.fillMaxWidth().height(6.dp),color=Color(0xFFFFC857),trackColor=Color(0xFF5D4A2E))}
@Composable private fun StatRow(name:String,base:Int,bonus:Int,final:Int){Row(Modifier.fillMaxWidth().padding(vertical=3.dp),horizontalArrangement=Arrangement.SpaceBetween){Text(name);Text("$base ${if(bonus>=0) "+" else ""}$bonus → $final",fontWeight=FontWeight.SemiBold)}}
@Composable private fun ItemLine(item:ItemEntity)=Row(Modifier.fillMaxWidth().padding(vertical=3.dp),horizontalArrangement=Arrangement.SpaceBetween){Text(item.name);Text(if(item.equipped)"Équipé" else "x${item.quantity}",color=MaterialTheme.colorScheme.primary)}
@Composable private fun RankBadge(rank:String){Surface(shape=RoundedCornerShape(8.dp),color=when(rank){"S","A"->Color(0xFFC6A0F6);"B","C"->Color(0xFF8BD3DD);else->Color(0xFFFFC857)}){Text("RANG $rank",Modifier.padding(horizontal=8.dp,vertical=4.dp),color=Color(0xFF17100A),style=MaterialTheme.typography.labelSmall,fontWeight=FontWeight.Black)}}
@Composable private fun Loading()=Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center){CircularProgressIndicator()}
