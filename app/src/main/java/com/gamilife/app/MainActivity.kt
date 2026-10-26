package com.gamilife.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.gamilife.app.data.*
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class GameViewModel(private val repository: GameRepository) : ViewModel() {
    val game = repository.state.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), GameSnapshot(null,emptyList(),emptyList(),emptyList(),emptyList(),emptyList(),emptyList()))
    var message by mutableStateOf<String?>(null); private set
    init { viewModelScope.launch { repository.seed() } }
    fun dismiss() { message = null }
    fun equip(id: Long) = viewModelScope.launch { message = repository.equip(id) }
    fun validate(id: Long) = viewModelScope.launch { message = repository.validateSkill(id,System.currentTimeMillis()) }
    fun irregular(id: Long) = viewModelScope.launch { message = repository.applyIrregularity(id) }
    fun fight(skill: Long, monster: Long) = viewModelScope.launch { message = repository.fight(skill,monster)?.log ?: "Combat impossible" }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState)
        val repo = GameRepository(GameDatabase.create(applicationContext))
        setContent { MaterialTheme(colorScheme = GamilifeColors) { val vm: GameViewModel = androidx.lifecycle.viewmodel.compose.viewModel(factory = object: ViewModelProvider.Factory { override fun <T: ViewModel> create(modelClass: Class<T>): T { @Suppress("UNCHECKED_CAST") return GameViewModel(repo) as T } }); val game by vm.game.collectAsStateWithLifecycle(); GamilifeApp(game,vm) } }
    }
}
