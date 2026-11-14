package com.tuneflow.core.network

import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.ResponseBody
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Headers
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.URI
import java.net.URISyntaxException
import java.security.MessageDigest
import java.util.Locale
import java.util.concurrent.TimeUnit

data class AppRelease(
    val version: String,
    val apkUrl: String,
    val apkSizeBytes: Long,
    val sha256: String?,
    val releaseNotes: String? = null,
)

interface AppUpdateRepository {
    suspend fun latestRelease(): AppRelease

    suspend fun downloadApk(
        release: AppRelease,
        destination: File,
        onProgress: (downloadedBytes: Long, totalBytes: Long) -> Unit,
    )
}

class AppUpdateException(message: String) : IOException(message)

object AppUpdateRepositoryFactory {
    fun create(): AppUpdateRepository {
        val client =
            NetworkFactory.sharedHttpClient()
                .newBuilder()
                .readTimeout(60, TimeUnit.SECONDS)
                .build()
        return GitHubAppUpdateRepository(
            api = createGitHubReleaseApi(GITHUB_API_BASE_URL, client),
            client = client,
        )
    }
}

fun isNewerAppVersion(
    latestVersion: String,
    currentVersion: String,
): Boolean =
    SemanticVersion.parse(latestVersion)?.let { latest ->
        SemanticVersion.parse(currentVersion)?.let { current -> latest > current }
    } ?: false

internal class GitHubAppUpdateRepository(
    private val api: GitHubReleaseApi,
    private val client: OkHttpClient,
) : AppUpdateRepository {
    override suspend fun latestRelease(): AppRelease {
        val response = api.latestRelease()
        if (response.draft || response.prerelease) {
            throw AppUpdateException("Latest GitHub release is not stable.")
        }

        val version = response.tagName.trim().removePrefix("v")
        if (SemanticVersion.parse(version) == null) {
            throw AppUpdateException("Latest GitHub release has an invalid version.")
        }

        val asset =
            response.assets.firstOrNull { candidate ->
                candidate.name.endsWith(APK_EXTENSION, ignoreCase = true) &&
                    candidate.contentType == APK_CONTENT_TYPE
            } ?: throw AppUpdateException("Latest GitHub release has no APK asset.")
        val downloadUrl =
            validatedDownloadUrl(asset.downloadUrl)
                ?: throw AppUpdateException("Latest GitHub release has an invalid APK URL.")
        if (asset.size !in 1..MAX_APK_SIZE_BYTES) {
            throw AppUpdateException("Latest GitHub release has an invalid APK size.")
        }
        val sha256 = parseSha256(asset.digest)
        if (asset.digest != null && sha256 == null) {
            throw AppUpdateException("Latest GitHub release has invalid checksum metadata.")
        }

        return AppRelease(
            version = version,
            apkUrl = downloadUrl,
            apkSizeBytes = asset.size,
            sha256 = sha256,
            releaseNotes = normalizeReleaseNotes(response.body),
        )
    }

    override suspend fun downloadApk(
        release: AppRelease,
        destination: File,
        onProgress: (downloadedBytes: Long, totalBytes: Long) -> Unit,
    ) = withContext(Dispatchers.IO) {
        val downloadUrl =
            validatedDownloadUrl(release.apkUrl)
                ?: throw AppUpdateException("Release APK URL is invalid.")

        val parent =
            destination.parentFile
                ?: throw AppUpdateException("Update cache directory is unavailable.")
        if (!parent.exists() && !parent.mkdirs()) {
            throw AppUpdateException("Update cache directory could not be created.")
        }
        val partial = File(parent, "${destination.name}.part")
        partial.delete()

        var completed = false
        try {
            val request =
                Request.Builder()
                    .url(downloadUrl)
                    .header("Accept", APK_CONTENT_TYPE)
                    .header("User-Agent", GITHUB_USER_AGENT)
                    .build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw AppUpdateException("APK download failed with HTTP ${response.code()}.")
                }
                val body =
                    response.body()
                        ?: throw AppUpdateException("APK download returned no content.")
                copyAndVerifyApk(body, release, partial, onProgress)
            }
            destination.delete()
            if (!partial.renameTo(destination)) {
                throw AppUpdateException("Downloaded APK could not be finalized.")
            }
            completed = true
        } finally {
            if (!completed) partial.delete()
        }
    }
}

internal fun copyAndVerifyApk(
    body: ResponseBody,
    release: AppRelease,
    destination: File,
    onProgress: (downloadedBytes: Long, totalBytes: Long) -> Unit,
) {
    body.byteStream().use { input ->
        destination.outputStream().buffered().use { output ->
            val result = copyApkBytes(input, output, release.apkSizeBytes, onProgress)
            verifyApkCopy(result, release)
        }
    }
}

private fun copyApkBytes(
    input: InputStream,
    output: OutputStream,
    expectedBytes: Long,
    onProgress: (downloadedBytes: Long, totalBytes: Long) -> Unit,
): ApkCopyResult {
    val digest = MessageDigest.getInstance(SHA_256)
    var downloadedBytes = 0L
    val buffer = ByteArray(DOWNLOAD_BUFFER_SIZE)
    while (true) {
        val count = input.read(buffer)
        if (count < 0) break
        downloadedBytes += count
        if (downloadedBytes > expectedBytes || downloadedBytes > MAX_APK_SIZE_BYTES) {
            throw AppUpdateException("Downloaded APK is larger than release metadata.")
        }
        output.write(buffer, 0, count)
        digest.update(buffer, 0, count)
        onProgress(downloadedBytes, expectedBytes)
    }
    return ApkCopyResult(downloadedBytes, digest.digest().toHexString())
}

private fun verifyApkCopy(
    result: ApkCopyResult,
    release: AppRelease,
) {
    if (result.downloadedBytes != release.apkSizeBytes) {
        throw AppUpdateException("Downloaded APK size does not match release metadata.")
    }
    val expectedSha256 = release.sha256 ?: return
    if (!result.sha256.equals(expectedSha256, ignoreCase = true)) {
        throw AppUpdateException("Downloaded APK checksum does not match release metadata.")
    }
}

private fun ByteArray.toHexString(): String = joinToString(separator = "") { byte -> "%02x".format(byte) }

private data class ApkCopyResult(
    val downloadedBytes: Long,
    val sha256: String,
)

internal fun createGitHubReleaseApi(
    baseUrl: String,
    client: OkHttpClient,
): GitHubReleaseApi =
    Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(GitHubReleaseApi::class.java)

internal interface GitHubReleaseApi {
    @Headers(
        "Accept: application/vnd.github+json",
        "X-GitHub-Api-Version: 2022-11-28",
        "User-Agent: TuneFlow-Android",
    )
    @GET("repos/Venkatpandey/TuneFlow/releases/latest")
    suspend fun latestRelease(): GitHubReleaseResponse
}

internal data class GitHubReleaseResponse(
    @SerializedName("tag_name") val tagName: String,
    val draft: Boolean,
    val prerelease: Boolean,
    val body: String?,
    val assets: List<GitHubReleaseAsset>,
)

internal data class GitHubReleaseAsset(
    val name: String,
    @SerializedName("content_type") val contentType: String,
    @SerializedName("browser_download_url") val downloadUrl: String,
    val size: Long,
    val digest: String?,
)

private data class SemanticVersion(
    val major: Int,
    val minor: Int,
    val patch: Int,
    val prerelease: List<String>,
) : Comparable<SemanticVersion> {
    override fun compareTo(other: SemanticVersion): Int {
        val stableVersionComparison =
            compareValuesBy(
                this,
                other,
                SemanticVersion::major,
                SemanticVersion::minor,
                SemanticVersion::patch,
            )
        return if (stableVersionComparison != 0) {
            stableVersionComparison
        } else {
            comparePrereleaseVersions(prerelease, other.prerelease)
        }
    }

    companion object {
        fun parse(value: String): SemanticVersion? {
            val match = SEMANTIC_VERSION_REGEX.matchEntire(value.trim())
            return match?.let {
                val major = match.groupValues[1].toIntOrNull()
                val minor = match.groupValues[2].toIntOrNull()
                val patch = match.groupValues[3].toIntOrNull()
                if (major == null || minor == null || patch == null) {
                    null
                } else {
                    SemanticVersion(
                        major = major,
                        minor = minor,
                        patch = patch,
                        prerelease = match.groupValues[4].takeIf(String::isNotEmpty)?.split('.').orEmpty(),
                    )
                }
            }
        }
    }
}

private fun comparePrereleaseVersions(
    left: List<String>,
    right: List<String>,
): Int =
    when {
        left.isEmpty() && right.isNotEmpty() -> 1
        left.isNotEmpty() && right.isEmpty() -> -1
        else ->
            left.zip(right)
                .asSequence()
                .map { (leftPart, rightPart) -> comparePrereleasePart(leftPart, rightPart) }
                .firstOrNull { it != 0 }
                ?: compareValues(left.size, right.size)
    }

private fun comparePrereleasePart(
    left: String,
    right: String,
): Int {
    val leftNumber = left.toIntOrNull()
    val rightNumber = right.toIntOrNull()
    return when {
        leftNumber != null && rightNumber != null -> compareValues(leftNumber, rightNumber)
        leftNumber != null -> -1
        rightNumber != null -> 1
        else -> left.compareTo(right)
    }
}

private fun parseSha256(digest: String?): String? =
    digest
        ?.trim()
        ?.takeIf { it.substringBefore(':').equals("sha256", ignoreCase = true) }
        ?.substringAfter(':', missingDelimiterValue = "")
        ?.lowercase(Locale.ROOT)
        ?.takeIf { it.length == SHA_256_HEX_LENGTH && it.all(Char::isHexDigit) }

private fun normalizeReleaseNotes(body: String?): String? =
    body
        ?.trim()
        ?.takeIf(String::isNotEmpty)
        ?.let { notes ->
            if (notes.length <= MAX_RELEASE_NOTES_LENGTH) {
                notes
            } else {
                notes.take(MAX_RELEASE_NOTES_LENGTH).trimEnd() + "\u2026"
            }
        }

private fun validatedDownloadUrl(value: String): String? {
    val uri =
        try {
            URI(value)
        } catch (_: URISyntaxException) {
            return null
        }
    return value.takeIf {
        uri.scheme.equals("https", ignoreCase = true) &&
            uri.host.equals(GITHUB_DOWNLOAD_HOST, ignoreCase = true) &&
            uri.rawUserInfo == null
    }
}

private fun Char.isHexDigit(): Boolean = this in '0'..'9' || this in 'a'..'f'

private val SEMANTIC_VERSION_REGEX =
    Regex("^v?(0|[1-9]\\d*)\\.(0|[1-9]\\d*)\\.(0|[1-9]\\d*)(?:-([0-9A-Za-z.-]+))?(?:\\+[0-9A-Za-z.-]+)?$")
private const val GITHUB_API_BASE_URL = "https://api.github.com/"
private const val GITHUB_DOWNLOAD_HOST = "github.com"
private const val GITHUB_USER_AGENT = "TuneFlow-Android"
private const val APK_EXTENSION = ".apk"
private const val APK_CONTENT_TYPE = "application/vnd.android.package-archive"
private const val SHA_256 = "SHA-256"
private const val SHA_256_HEX_LENGTH = 64
private const val MAX_RELEASE_NOTES_LENGTH = 4_000
private const val DOWNLOAD_BUFFER_SIZE = 64 * 1024
private const val MAX_APK_SIZE_BYTES = 512L * 1024L * 1024L
