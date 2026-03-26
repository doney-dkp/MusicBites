package com.dkp.musicbites.models.body

import com.dkp.musicbites.models.Context
import kotlinx.serialization.Serializable

@Serializable
data class SubscribeBody(
    val channelIds: List<String>,
    val context: Context,
)
