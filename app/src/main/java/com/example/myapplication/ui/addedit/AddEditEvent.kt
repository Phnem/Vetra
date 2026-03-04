package com.example.myapplication.ui.addedit

import android.net.Uri

sealed interface AddEditEvent {
    data class OnTitleChanged(val title: String) : AddEditEvent
    data class OnEpisodesChanged(val episodes: String) : AddEditEvent
    data class OnRatingChanged(val rating: Int) : AddEditEvent
    data class OnImageUriChanged(val uri: Uri?) : AddEditEvent
    data class OnTagsChanged(val tags: List<String>, val categoryType: String) : AddEditEvent
    data class OnCommentModeChanged(val mode: CommentMode) : AddEditEvent
    data class OnSaveComment(val comment: String) : AddEditEvent
    data object OnSave : AddEditEvent
}
