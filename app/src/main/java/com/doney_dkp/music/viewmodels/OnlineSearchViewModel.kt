package com.doney_dkp.music.viewmodels

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dkp.musicbites.YouTube
import com.dkp.musicbites.models.filterExplicit
import com.doney_dkp.music.constants.HideExplicitKey
import com.doney_dkp.music.models.ItemsPage
import com.doney_dkp.music.utils.dataStore
import com.doney_dkp.music.utils.get
import com.doney_dkp.music.utils.reportException
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.collections.set

@HiltViewModel
class OnlineSearchViewModel @Inject constructor(
    @ApplicationContext context: Context,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    val query = savedStateHandle.get<String>("query")!!
    val filter = MutableStateFlow<YouTube.SearchFilter?>(YouTube.SearchFilter.FILTER_SONG)
    var allItemsPage by mutableStateOf<ItemsPage?>(null)
    val viewStateMap = mutableStateMapOf<String, ItemsPage?>()

    init {
        viewModelScope.launch {
            filter.collect { filter ->
                if (filter == null) {
                    if (allItemsPage == null) {
                        YouTube.searchAll(query)
                            .onSuccess { result ->
                                allItemsPage = ItemsPage(
                                    result.items
                                        .distinctBy { it.id }
                                        .filterExplicit(context.dataStore.get(HideExplicitKey, false)),
                                    result.continuation
                                )
                            }
                            .onFailure {
                                reportException(it)
                            }
                    }
                } else {
                    if (viewStateMap[filter.value] == null) {
                        YouTube.search(query, filter)
                            .onSuccess { result ->
                                viewStateMap[filter.value] = ItemsPage(
                                    result.items
                                        .distinctBy { it.id }
                                        .filterExplicit(context.dataStore.get(HideExplicitKey, false)),
                                    result.continuation
                                )
                            }
                            .onFailure {
                                reportException(it)
                            }
                    }
                }
            }
        }
    }

    fun loadMore() {
        viewModelScope.launch {
            val filterValue = filter.value?.value
            if (filterValue == null) {
                val viewState = allItemsPage ?: return@launch
                val continuation = viewState.continuation ?: return@launch
                val searchResult = YouTube.searchContinuation(continuation).getOrNull() ?: return@launch
                allItemsPage = ItemsPage((viewState.items + searchResult.items).distinctBy { it.id }, searchResult.continuation)
                return@launch
            }
            val viewState = viewStateMap[filterValue] ?: return@launch
            val continuation = viewState.continuation ?: return@launch
            val searchResult = YouTube.searchContinuation(continuation).getOrNull() ?: return@launch
            viewStateMap[filterValue] = ItemsPage((viewState.items + searchResult.items).distinctBy { it.id }, searchResult.continuation)
        }
    }
}
