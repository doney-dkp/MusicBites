package com.dkp.musicbites.pages

import com.dkp.musicbites.models.SongItem

data class PlaylistContinuationPage(
    val songs: List<SongItem>,
    val continuation: String?,
)
