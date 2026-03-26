package com.doney_dkp.music

import android.app.Application
import android.os.Build
import android.widget.Toast
import android.widget.Toast.LENGTH_SHORT
import androidx.datastore.preferences.core.edit
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import com.dkp.musicbites.YouTube
import com.dkp.musicbites.models.YouTubeLocale
import com.doney_dkp.kugou.KuGou
import com.doney_dkp.music.constants.ContentCountryKey
import com.doney_dkp.music.constants.ContentLanguageKey
import com.doney_dkp.music.constants.CountryCodeToName
import com.doney_dkp.music.constants.DataSyncIdKey
import com.doney_dkp.music.constants.InnerTubeCookieKey
import com.doney_dkp.music.constants.LanguageCodeToName
import com.doney_dkp.music.constants.MaxImageCacheSizeKey
import com.doney_dkp.music.constants.ProxyEnabledKey
import com.doney_dkp.music.constants.ProxyTypeKey
import com.doney_dkp.music.constants.ProxyUrlKey
import com.doney_dkp.music.constants.SYSTEM_DEFAULT
import com.doney_dkp.music.constants.UseLoginForBrowse
import com.doney_dkp.music.constants.VisitorDataKey
import com.doney_dkp.music.extensions.toEnum
import com.doney_dkp.music.extensions.toInetSocketAddress
import com.doney_dkp.music.utils.dataStore
import com.doney_dkp.music.utils.get
import com.doney_dkp.music.utils.reportException
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import timber.log.Timber
import java.net.Proxy
import java.util.Locale

@HiltAndroidApp
class App : Application(), ImageLoaderFactory {
    companion object {
        lateinit var instance: App
            private set
    }

    @OptIn(DelicateCoroutinesApi::class)
    override fun onCreate() {
        super.onCreate()
        instance = this
        Timber.plant(Timber.DebugTree())

        val locale = Locale.getDefault()
        val languageTag = locale.toLanguageTag().replace("-Hant", "") // replace zh-Hant-* to zh-*
        YouTube.locale = YouTubeLocale(
            gl = dataStore[ContentCountryKey]?.takeIf { it != SYSTEM_DEFAULT }
                ?: locale.country.takeIf { it in CountryCodeToName }
                ?: "US",
            hl = dataStore[ContentLanguageKey]?.takeIf { it != SYSTEM_DEFAULT }
                ?: locale.language.takeIf { it in LanguageCodeToName }
                ?: languageTag.takeIf { it in LanguageCodeToName }
                ?: "en"
        )
        if (languageTag == "zh-TW") {
            KuGou.useTraditionalChinese = true
        }

        if (dataStore[ProxyEnabledKey] == true) {
            try {
                YouTube.proxy = Proxy(
                    dataStore[ProxyTypeKey].toEnum(defaultValue = Proxy.Type.HTTP),
                    dataStore[ProxyUrlKey]!!.toInetSocketAddress()
                )
            } catch (e: Exception) {
                Toast.makeText(this, "Failed to parse proxy url.", LENGTH_SHORT).show()
                reportException(e)
            }
        }

        if (dataStore[UseLoginForBrowse] == true) {
            YouTube.useLoginForBrowse = true
        }

        GlobalScope.launch {
            dataStore.data
                .map { it[VisitorDataKey] }
                .distinctUntilChanged()
                .collect { visitorData ->
                    YouTube.visitorData = visitorData
                        ?.takeIf { it != "null" } // Previously visitorData was sometimes saved as "null" due to a bug
                        ?: YouTube.visitorData().getOrNull()?.also { newVisitorData ->
                            dataStore.edit { settings ->
                                settings[VisitorDataKey] = newVisitorData
                            }
                        } ?: YouTube.DEFAULT_VISITOR_DATA
                }
        }
        GlobalScope.launch {
            dataStore.data
                .map { it[InnerTubeCookieKey] }
                .distinctUntilChanged()
                .collect { cookie ->
                    YouTube.cookie = cookie
                }
        }
        GlobalScope.launch {
            dataStore.data
                .map { it[DataSyncIdKey] }
                .distinctUntilChanged()
                .collect { dataSyncId ->
                    YouTube.dataSyncId = dataSyncId?.takeIf { it.isNotBlank() }
                }
        }
    }

    override fun newImageLoader() = ImageLoader.Builder(this)
        .crossfade(true)
        .respectCacheHeaders(false)
        .allowHardware(Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
        .diskCache(
            DiskCache.Builder()
                .directory(cacheDir.resolve("coil"))
                .maxSizeBytes((dataStore[MaxImageCacheSizeKey] ?: 512) * 1024 * 1024L)
                .build()
        )
        .build()
}