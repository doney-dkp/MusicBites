package com.dkp.musicbites.models.body

import com.dkp.musicbites.models.Context
import kotlinx.serialization.Serializable

@Serializable
data class GetSearchSuggestionsBody(
    val context: Context,
    val input: String,
)
