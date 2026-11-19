package com.tuneflow.feature.browse

import com.tuneflow.core.network.AlbumSummary
import com.tuneflow.core.network.ArtistDetail
import com.tuneflow.core.network.ArtistSummary
import com.tuneflow.core.network.SearchBundle
import com.tuneflow.core.network.SessionData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BrowseArtworkTest {
    private val session =
        SessionData(
            serverUrl = "https://navidrome.example.com",
            username = "testuser",
            token = "dummytoken",
            salt = "dummysalt",
        )

    @Test
    fun resolveArtistArtUrl_withAbsoluteUrl_returnsDirectly() {
        val httpUrl = "http://images.example.com/artist.png"
        val httpsUrl = "https://images.example.com/artist.jpg"

        assertEquals(httpUrl, resolveArtistArtUrl(session, "ar-1", httpUrl))
        assertEquals(httpsUrl, resolveArtistArtUrl(session, "ar-1", httpsUrl))
        assertEquals(httpsUrl, resolveArtistArtUrl(null, "ar-1", httpsUrl))
    }

    @Test
    fun resolveArtistArtUrl_withRelativePath_prependsServerUrl() {
        val relative = "/rest/artist/art/123.jpg"

        val resolved = resolveArtistArtUrl(session, "ar-1", relative)

        assertEquals("https://navidrome.example.com/rest/artist/art/123.jpg", resolved)
    }

    @Test
    fun resolveArtistArtUrl_withCoverArtId_buildsSubsonicCoverArtUrl() {
        val resolved = resolveArtistArtUrl(session, "ar-coldplay", null)

        assertTrue(resolved != null)
        assertTrue(resolved!!.startsWith("https://navidrome.example.com/rest/getCoverArt.view?id=ar-coldplay"))
        assertTrue(resolved.contains("u=testuser"))
        assertTrue(resolved.contains("t=dummytoken"))
        assertTrue(resolved.contains("s=dummysalt"))
    }

    @Test
    fun resolveArtistArtUrl_whenNoImageMetadata_returnsNull() {
        assertNull(resolveArtistArtUrl(session, null, null))
        assertNull(resolveArtistArtUrl(session, "", ""))
    }

    @Test
    fun artistSummary_withArtwork_resolvesArtistImage() {
        val summaryWithUrl =
            ArtistSummary(
                id = "art-1",
                name = "Coldplay",
                albumCount = 8,
                artistImageUrl = "https://images.example.com/coldplay.jpg",
            ).withArtwork(session)

        assertEquals("https://images.example.com/coldplay.jpg", summaryWithUrl.artUrl)

        val summaryWithCoverArt =
            ArtistSummary(
                id = "art-2",
                name = "Radiohead",
                albumCount = 9,
                coverArtId = "ar-radiohead",
            ).withArtwork(session)

        assertTrue(summaryWithCoverArt.artUrl?.contains("id=ar-radiohead") == true)

        val summaryWithFallback =
            ArtistSummary(
                id = "art-3",
                name = "Muse",
                albumCount = 7,
            ).withArtwork(session, fallbackUrl = "https://fallback.example.com/muse.jpg")

        assertEquals("https://fallback.example.com/muse.jpg", summaryWithFallback.artUrl)
    }

    @Test
    fun artistDetail_withArtwork_prefersArtistImageOverAlbumCover() {
        val detail =
            ArtistDetail(
                id = "art-1",
                name = "Coldplay",
                albumCount = 2,
                coverArtId = "ar-coldplay",
                albums =
                    listOf(
                        AlbumSummary(id = "alb-1", title = "Parachutes", artist = "Coldplay", coverArtId = "al-parachutes"),
                    ),
            ).withArtwork(session)

        assertTrue(detail.artUrl?.contains("id=ar-coldplay") == true)
        assertTrue(detail.albums.first().artUrl?.contains("id=al-parachutes") == true)
    }

    @Test
    fun artistDetail_withArtwork_fallsBackToAlbumCoverWhenNoArtistImage() {
        val detail =
            ArtistDetail(
                id = "art-1",
                name = "Coldplay",
                albumCount = 2,
                coverArtId = null,
                artistImageUrl = null,
                albums =
                    listOf(
                        AlbumSummary(id = "alb-1", title = "Parachutes", artist = "Coldplay", coverArtId = "al-parachutes"),
                    ),
            ).withArtwork(session)

        assertTrue(detail.artUrl?.contains("id=al-parachutes") == true)
    }

    @Test
    fun searchBundle_withArtwork_resolvesArtistArtwork() {
        val bundle =
            SearchBundle(
                artists =
                    listOf(
                        ArtistSummary(id = "art-1", name = "Daft Punk", albumCount = 4, coverArtId = "ar-daft"),
                        ArtistSummary(id = "art-2", name = "Justice", albumCount = 3),
                    ),
                albums =
                    listOf(
                        AlbumSummary(id = "alb-2", title = "Cross", artist = "Justice", coverArtId = "al-justice"),
                    ),
                tracks = emptyList(),
            ).withArtwork(session)

        assertTrue(bundle.artists[0].artUrl?.contains("id=ar-daft") == true)
        assertTrue(bundle.artists[1].artUrl?.contains("id=al-justice") == true)
    }
}
