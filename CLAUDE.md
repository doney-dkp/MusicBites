# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Overview

MusicBites is a Material 3 YouTube Music client for Android (minSdk 24, targetSdk 35). It plays audio from YouTube Music without ads, supports offline caching/downloading, synchronized lyrics, Android Auto, and personalized home feeds. Written in 100% Kotlin using Jetpack Compose.

## Build Commands

```bash
# Debug builds
./gradlew assembleDebug
./gradlew assembleFossDebug    # F-Droid compatible, no Firebase/Google services
./gradlew assembleFullDebug    # With Firebase Analytics, Crashlytics, ML Kit

# Release builds
./gradlew assembleRelease
./gradlew assembleFossRelease
./gradlew assembleFullRelease

# Install on connected device
./gradlew installDebug

# Tests and lint
./gradlew test
./gradlew lint
```

**Build flavors:**
- `foss` — F-Droid compatible; excludes Firebase, Crashlytics, ML Kit translate/language-id
- `full` — includes Firebase Analytics, Crashlytics, Performance Monitoring, and ML Kit

Release signing uses environment variables: `MUSIC_RELEASE_SIGNING_*`.

## Module Structure

Multi-module Gradle project with Kotlin DSL. Dependency versions are in `gradle/libs.versions.toml`.

```
app/          — Main Android application module
innertube/    — YouTube InnerTube API client (JVM library)
kugou/        — KuGou lyrics API client (JVM library)
lrclib/       — LrcLib lyrics API client (JVM library)
material-color-utilities/  — Google Material Color Utilities (JVM library)
```

The `app` module depends on all four library modules. The library modules are pure JVM (no Android dependencies) and use Ktor + Kotlin Serialization.

## Architecture

**Pattern:** MVVM + Hilt DI + Jetpack Compose + Media3

### Layers

1. **UI Layer** — Jetpack Compose screens in `ui/screens/`. `MainActivity.kt` hosts the Compose NavHost and the bottom sheet player.
2. **ViewModel Layer** — Hilt-injected ViewModels in `viewmodels/`. Each screen has a corresponding ViewModel using `StateFlow`/`Flow`.
3. **Playback Layer** — `MusicService` (extends `MediaLibraryService`) wraps ExoPlayer. `PlayerConnection` exposes player state as flows to the UI. `ExoDownloadService` handles offline downloads.
4. **Data Layer:**
   - `YouTube.kt` (innertube module) — singleton wrapping the InnerTube HTTP API
   - `MusicDatabase.kt` (Room) — local cache; accessed via `DatabaseDao`
   - `KuGou.kt` / `LrcLib.kt` — lyrics providers
   - DataStore — user preferences (cookies, theme, playback settings)

### Key Files

| File | Purpose |
|------|---------|
| `App.kt` | Application class; initializes Hilt, Timber, Coil, YouTube locale/proxy |
| `MainActivity.kt` | Main Activity; ~4300 lines; entire Compose UI + navigation |
| `playback/MusicService.kt` | MediaLibraryService; ExoPlayer setup, MediaSession, notifications |
| `playback/PlayerConnection.kt` | Bridges MusicService to UI via flows |
| `db/MusicDatabase.kt` | Room DB (v12); entities for songs, artists, albums, playlists, lyrics, events |
| `db/DatabaseDao.kt` | Single DAO for all database access |
| `di/AppModule.kt` | Hilt module wiring the app together |
| `innertube/.../YouTube.kt` | All YouTube API calls (search, browse, streams, auth) |

### Database

Room database `song.db` at version 12. Core entities: `SongEntity`, `ArtistEntity`, `AlbumEntity`, `PlaylistEntity`, `SongArtistMap`, `SongAlbumMap`, `PlaylistSongMap`, `LyricsEntity`, `Event` (playback history), `FormatEntity`. Migrations are auto-migrations with custom specs in `MusicDatabase.kt`.

### Lyrics

`LyricsHelper` coordinates two providers: KuGou (Chinese lyrics) and LrcLib (international). Lyrics are cached in `LyricsEntity`.

### Dynamic Theming

`material-color-utilities` module extracts a seed color from album art; `ui/theme/` applies it as a Material 3 dynamic color scheme.

### Authentication

YouTube session state (visitorData, cookie, dataSyncId) stored in DataStore. `YouTube.kt` uses these for authenticated requests. Po-token generation lives in `utils/potoken/`.

## Constants & Preferences

App-wide DataStore preference keys are defined as `Preferences.Key<T>` objects in `constants/`. There are 30+ keys covering theme, playback (repeat, normalization, tempo/pitch, skip silence), network (proxy, content country/language), and privacy settings.

## Compile Options

- JVM target: 17
- Context receivers enabled (`-Xcontext-receivers`)
- Core library desugaring enabled
- ProGuard + resource shrinking enabled for release builds
- Lint config in `app/lint.xml` (suppresses missing-translation warnings, allows unstable Media3 APIs)
