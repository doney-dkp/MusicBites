package com.dkp.musicbites.models.body

import com.dkp.musicbites.models.Context
import kotlinx.serialization.Serializable

@Serializable
data class SearchBody(
    val context: Context,
    val query: String?,
    val params: String?,
)
