package com.doney_dkp.music.lyrics

import android.content.Context
import com.doney_dkp.kugou.KuGou
import com.doney_dkp.music.constants.EnableKugouKey
import com.doney_dkp.music.utils.dataStore
import com.doney_dkp.music.utils.get

object KuGouLyricsProvider : LyricsProvider {
    override val name = "Kugou"
    override fun isEnabled(context: Context): Boolean =
        context.dataStore[EnableKugouKey] ?: true

    override suspend fun getLyrics(id: String, title: String, artist: String, duration: Int): Result<String> =
        KuGou.getLyrics(title, artist, duration)

    override suspend fun getAllLyrics(id: String, title: String, artist: String, duration: Int, callback: (String) -> Unit) {
        KuGou.getAllLyrics(title, artist, duration, callback)
    }
}
