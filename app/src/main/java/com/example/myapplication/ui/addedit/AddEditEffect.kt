package com.example.myapplication.ui.addedit

/**
 * Side Effects для одноразовых событий (навигация, тосты).
 * UI подписывается на effect и реагирует — без коллбеков в Event.
 */
sealed interface AddEditEffect {
    data object NavigateBack : AddEditEffect
    data class ShowError(val message: String) : AddEditEffect
}
