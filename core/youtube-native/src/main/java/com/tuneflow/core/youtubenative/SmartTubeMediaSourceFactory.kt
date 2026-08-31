package com.tuneflow.core.youtubenative

import android.content.Context
import android.net.Uri
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory
import com.google.android.exoplayer2.source.ExtractorMediaSource
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.dash.DashMediaSource
import com.google.android.exoplayer2.source.dash.DefaultDashChunkSource
import com.google.android.exoplayer2.source.dash.manifest.DashManifestParser2
import com.google.android.exoplayer2.source.hls.HlsMediaSource
import com.google.android.exoplayer2.source.sabr.DefaultSabrChunkSource
import com.google.android.exoplayer2.source.sabr.SabrMediaSource
import com.google.android.exoplayer2.source.sabr.manifest.SabrManifestParser
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory
import com.liskovsoft.mediaserviceinterfaces.data.MediaItemFormatInfo

internal class SmartTubeMediaSourceFactory(context: Context) {
    private val dataSourceFactory =
        DefaultDataSourceFactory(
            context.applicationContext,
            null,
            DefaultHttpDataSourceFactory(USER_AGENT, null, CONNECT_TIMEOUT_MS, READ_TIMEOUT_MS, true),
        )

    fun create(resolved: ResolvedYouTubeVideo): MediaSource =
        when (resolved.sourceKind) {
            YouTubeSourceKind.Dash -> createDash(resolved.formatInfo)
            YouTubeSourceKind.Sabr ->
                SabrMediaSource.Factory(DefaultSabrChunkSource.Factory(dataSourceFactory, MAX_SEGMENTS_PER_LOAD), null)
                    .createMediaSource(SabrManifestParser().parse(resolved.formatInfo))
            YouTubeSourceKind.Hls ->
                HlsMediaSource.Factory(dataSourceFactory).createMediaSource(Uri.parse(resolved.formatInfo.hlsManifestUrl))
            YouTubeSourceKind.Direct ->
                ExtractorMediaSource.Factory(dataSourceFactory)
                    .setExtractorsFactory(DefaultExtractorsFactory())
                    .createMediaSource(Uri.parse(resolved.formatInfo.createUrlList().first()))
        }

    private fun createDash(info: MediaItemFormatInfo): MediaSource =
        if (info.containsDashFormats()) {
            DashMediaSource.Factory(DefaultDashChunkSource.Factory(dataSourceFactory, MAX_SEGMENTS_PER_LOAD), null)
                .createMediaSource(DashManifestParser2().parse(info))
        } else {
            DashMediaSource.Factory(DefaultDashChunkSource.Factory(dataSourceFactory, MAX_SEGMENTS_PER_LOAD), dataSourceFactory)
                .createMediaSource(Uri.parse(info.dashManifestUrl))
        }

    private companion object {
        const val MAX_SEGMENTS_PER_LOAD = 1
        const val CONNECT_TIMEOUT_MS = 15_000
        const val READ_TIMEOUT_MS = 20_000
        const val USER_AGENT = "Mozilla/5.0 (Linux; Android TV) AppleWebKit/537.36 Chrome/120 Safari/537.36"
    }
}
