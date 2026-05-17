@file:Suppress("MatchingDeclarationName")

package com.tuneflow.tv

import com.tuneflow.feature.browse.HomeCategoryKind

internal enum class NavSection { Home, Albums, Playlists, Search }

internal const val NOW_PLAYING_SCREEN_KEY = "nowPlaying"
internal const val EXIT_CONFIRM_TIMEOUT_MS = 2000L

internal fun homeCategoryScreenKey(category: HomeCategoryKind): String = "homeCategory:${category.name}"
