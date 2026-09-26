package com.mraphaelpy.terriflow.presentation.territories

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mraphaelpy.terriflow.core.util.KmlParser
import com.mraphaelpy.terriflow.domain.usecase.territory.ImportTerritoriesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ImportItem(
    val parsed: KmlParser.ParsedTerritory,
    val isSelected: Boolean = true
)

data class ImportKmlUiState(
    val items: List<ImportItem> = emptyList(),
    val fileName: String = "",
    val isParsing: Boolean = false,
    val isImporting: Boolean = false,
    val importedCount: Int = 0,
    val error: String? = null,
    val isDone: Boolean = false
) {
    val selectedCount get() = items.count { it.isSelected }
}

@HiltViewModel
class ImportKmlViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val importUseCase: ImportTerritoriesUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ImportKmlUiState())
    val uiState: StateFlow<ImportKmlUiState> = _uiState.asStateFlow()

    fun parseFile(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isParsing = true, error = null, items = emptyList()) }
            try {
                val fileName = resolveFileName(uri)
                val isKmz = fileName.endsWith(".kmz", ignoreCase = true)
                val inputStream = context.contentResolver.openInputStream(uri)
                    ?: throw IllegalArgumentException("Não foi possível abrir o arquivo")

                val parsed = KmlParser.parse(inputStream, isKmz)

                if (parsed.isEmpty()) {
                    _uiState.update {
                        it.copy(isParsing = false, error = "Nenhum território com polígono encontrado no arquivo")
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            isParsing = false,
                            fileName = fileName,
                            items = parsed.map { t -> ImportItem(t) }
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isParsing = false, error = "Erro ao ler arquivo: ${e.message}")
                }
            }
        }
    }

    fun toggleItem(index: Int) {
        _uiState.update { state ->
            state.copy(
                items = state.items.mapIndexed { i, item ->
                    if (i == index) item.copy(isSelected = !item.isSelected) else item
                }
            )
        }
    }

    fun toggleAll(selected: Boolean) {
        _uiState.update { state ->
            state.copy(items = state.items.map { it.copy(isSelected = selected) })
        }
    }

    fun import() {
        val selected = _uiState.value.items.filter { it.isSelected }.map { it.parsed }
        if (selected.isEmpty()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isImporting = true, error = null) }
            try {
                val count = importUseCase(selected)
                _uiState.update { it.copy(isImporting = false, isDone = true, importedCount = count) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isImporting = false, error = "Erro ao importar: ${e.message}")
                }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    private fun resolveFileName(uri: Uri): String {
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        return cursor?.use {
            val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            it.moveToFirst()
            if (nameIndex >= 0) it.getString(nameIndex) else ""
        } ?: uri.lastPathSegment ?: "arquivo"
    }
}
