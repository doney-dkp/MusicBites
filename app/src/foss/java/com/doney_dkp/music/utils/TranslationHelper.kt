package com.doney_dkp.music.utils

import com.doney_dkp.music.db.entities.LyricsEntity

object TranslationHelper {
    suspend fun translate(lyrics: LyricsEntity): LyricsEntity = lyrics
    suspend fun clearModels() {}
}