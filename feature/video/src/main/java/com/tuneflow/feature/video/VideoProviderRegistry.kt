package com.tuneflow.feature.video

class VideoProviderRegistry(providers: List<VideoProvider>) {
    private val approvedProviders = setOf(VideoProviderId.YouTube)
    private val providersById =
        providers
            .filter { it.id in approvedProviders }
            .associateBy(VideoProvider::id)

    fun enabledProviders(): List<VideoProvider> =
        providersById.values
            .filter(VideoProvider::configured)
            .sortedBy { it.id.name }

    fun provider(id: VideoProviderId): VideoProvider? = providersById[id]?.takeIf(VideoProvider::configured)
}

internal object VideoDomainPolicy {
    private val resourceDomains =
        setOf(
            "youtube.com",
            "youtube-nocookie.com",
            "googlevideo.com",
            "ytimg.com",
            "ggpht.com",
            "googleusercontent.com",
            "google.com",
            "googleapis.com",
            "gstatic.com",
            "doubleclick.net",
            "googleadservices.com",
            "googlesyndication.com",
        )

    fun isAllowedResourceHost(host: String?): Boolean = host != null && resourceDomains.any { host == it || host.endsWith(".$it") }

    fun isApprovedExternalHost(host: String?): Boolean =
        host != null &&
            listOf("youtube.com", "youtu.be", "google.com").any { host == it || host.endsWith(".$it") }
}
