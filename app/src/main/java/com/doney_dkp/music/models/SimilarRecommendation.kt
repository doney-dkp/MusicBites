package com.doney_dkp.music.models

import com.dkp.musicbites.models.YTItem
import com.doney_dkp.music.db.entities.LocalItem

data class SimilarRecommendation(
    val title: LocalItem,
    val items: List<YTItem>,
)
