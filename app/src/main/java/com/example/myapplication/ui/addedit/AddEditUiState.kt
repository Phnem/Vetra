package com.example.myapplication.ui.addedit

import android.net.Uri

enum class CommentMode { AddButton, Editing, Saved }

/**
 * Immutable UI state для AddEditScreen
 */
data class AddEditUiState(
    val animeId: String? = null,
    val title: String = "",
    val episodes: String = "",
    val rating: Int = 0,
    val selectedTags: List<String> = emptyList(),
    val categoryType: String = "",
    val imageUri: Uri? = null,
    val currentImageFileName: String? = null,
    val imageFilePath: String? = null,
    val orderIndex: Int = 0,
    val dateAdded: Long = 0L,
    val isFavorite: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null,
    val comment: String = "",
    val commentMode: CommentMode = CommentMode.AddButton
) {
    /**
     * Проверка наличия изменений
     */
    val hasChanges: Boolean
        get() = if (animeId == null) {
            title.isNotBlank()
        } else {
            // Для редактирования проверяем изменения относительно оригинала
            true // Упрощенно, в реальности сравниваем с оригиналом
        }
    
    /**
     * Валидация формы. Пустое episodes трактуется как 0.
     */
    val isValid: Boolean
        get() = title.isNotBlank() &&
                (episodes.isBlank() || (episodes.toIntOrNull()?.let { it >= 0 } ?: false))
}
