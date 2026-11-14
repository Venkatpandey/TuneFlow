package com.tuneflow.core.network

import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.ResponseBody.Companion.toResponseBody
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.File

class AppUpdateRepositoryTest {
    @Test
    fun semanticVersions_compareNumerically() {
        assertTrue(isNewerAppVersion("v1.10.0", "1.9.9"))
        assertTrue(isNewerAppVersion("2.0.0", "1.99.99"))
        assertFalse(isNewerAppVersion("1.2.0", "1.2.0"))
        assertFalse(isNewerAppVersion("1.1.9", "1.2.0"))
    }

    @Test
    fun semanticVersions_handlePrereleasesAndInvalidValues() {
        assertTrue(isNewerAppVersion("1.2.0", "1.2.0-beta.2"))
        assertTrue(isNewerAppVersion("1.2.0-beta.10", "1.2.0-beta.2"))
        assertFalse(isNewerAppVersion("1.2", "1.1.0"))
        assertFalse(isNewerAppVersion("latest", "1.1.0"))
    }

    @Test
    fun latestRelease_selectsVerifiedApkMetadata() =
        runTest {
            val server = MockWebServer()
            server.enqueue(
                MockResponse().setBody(
                    """
                    {
                      "tag_name": "v1.3.0",
                      "draft": false,
                      "prerelease": false,
                      "body": "Playback fixes and faster browsing.",
                      "assets": [
                        {
                          "name": "checksums.txt",
                          "content_type": "text/plain",
                          "browser_download_url": "https://github.com/example/checksums.txt",
                          "size": 100,
                          "digest": null
                        },
                        {
                          "name": "tuneflow-tv.apk",
                          "content_type": "application/vnd.android.package-archive",
                          "browser_download_url": "https://github.com/Venkatpandey/TuneFlow/releases/download/v1.3.0/tuneflow-tv.apk",
                          "size": 2048,
                          "digest": "sha256:aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
                        }
                      ]
                    }
                    """.trimIndent(),
                ),
            )
            server.start()
            try {
                val client = OkHttpClient()
                val repository =
                    GitHubAppUpdateRepository(
                        api = createGitHubReleaseApi(server.url("/").toString(), client),
                        client = client,
                    )

                val release = repository.latestRelease()

                assertEquals("1.3.0", release.version)
                assertEquals(2048L, release.apkSizeBytes)
                assertEquals("a".repeat(64), release.sha256)
                assertEquals("Playback fixes and faster browsing.", release.releaseNotes)
            } finally {
                server.shutdown()
            }
        }

    @Test
    fun latestRelease_rejectsMissingApk() =
        runTest {
            val server = MockWebServer()
            server.enqueue(
                MockResponse().setBody(
                    """
                    {
                      "tag_name": "v1.3.0",
                      "draft": false,
                      "prerelease": false,
                      "body": null,
                      "assets": []
                    }
                    """.trimIndent(),
                ),
            )
            server.start()
            try {
                val client = OkHttpClient()
                val repository =
                    GitHubAppUpdateRepository(
                        api = createGitHubReleaseApi(server.url("/").toString(), client),
                        client = client,
                    )

                try {
                    repository.latestRelease()
                    fail("Expected missing APK to fail")
                } catch (error: AppUpdateException) {
                    assertEquals("Latest GitHub release has no APK asset.", error.message)
                }
            } finally {
                server.shutdown()
            }
        }

    @Test
    fun copyAndVerifyApk_rejectsCorruptDownload() {
        val destination = File.createTempFile("tuneflow-update", ".apk")
        try {
            val release =
                AppRelease(
                    version = "1.3.0",
                    apkUrl = "https://github.com/example/app.apk",
                    apkSizeBytes = 3L,
                    sha256 = "a".repeat(64),
                )

            try {
                copyAndVerifyApk("apk".toResponseBody(), release, destination) { _, _ -> }
                fail("Expected corrupt APK to fail")
            } catch (error: AppUpdateException) {
                assertEquals("Downloaded APK checksum does not match release metadata.", error.message)
            }
        } finally {
            destination.delete()
        }
    }
}
