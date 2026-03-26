package com.dkp.musicbites.models.body

import com.dkp.musicbites.models.Context
import kotlinx.serialization.Serializable

@Serializable
data class GetTranscriptBody(
    val context: Context,
    val params: String,
)
