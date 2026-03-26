package com.dkp.musicbites.pages

import com.dkp.musicbites.models.YTItem

data class ArtistItemsContinuationPage(
    val items: List<YTItem>,
    val continuation: String?,
)
