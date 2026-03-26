package com.doney_dkp.music.models

import com.dkp.musicbites.models.YTItem

data class ItemsPage(
    val items: List<YTItem>,
    val continuation: String?,
)
