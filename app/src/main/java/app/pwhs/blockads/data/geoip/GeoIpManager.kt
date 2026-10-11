package app.pwhs.blockads.data.geoip

import android.content.Context
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsChannel
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import io.ktor.utils.io.jvm.javaio.toInputStream
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.util.zip.GZIPInputStream

/**
 * Manages downloading, caching, and auto-updating the binary GeoIP IPv4 database.
 *
 * Keeps the database off the initial APK footprint (benefiting F-Droid and Play Store size)
 * while ensuring offline, on-device lookup after download.
 */
class GeoIpManager(
    private val context: Context,
    private val client: HttpClient,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    companion object {
        const val GEOIP_FILE_NAME = "geoip_ipv4.bin"
        private const val GEOIP_TMP_NAME = "geoip_ipv4.bin.tmp"

        private const val PRIMARY_GZ_URL =
            "https://github.com/pass-with-high-score/blockads-android/releases/download/geoip-database/geoip_ipv4.bin.gz"
        private const val FALLBACK_BIN_URL =
            "https://github.com/pass-with-high-score/blockads-android/releases/download/geoip-database/geoip_ipv4.bin"

        private const val UPDATE_THRESHOLD_MS = 7L * 24 * 60 * 60 * 1000 // 7 days
    }

    private val _updateEvents = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val updateEvents: SharedFlow<Unit> = _updateEvents.asSharedFlow()

    fun getDatabaseFile(): File = File(context.filesDir, GEOIP_FILE_NAME)

    fun isDatabaseAvailable(): Boolean {
        val file = getDatabaseFile()
        return file.exists() && file.length() > 0
    }

    suspend fun checkAndUpdate(force: Boolean = false): Result<Boolean> = withContext(ioDispatcher) {
        val targetFile = getDatabaseFile()
        val isAvailable = targetFile.exists() && targetFile.length() > 0

        if (!force && isAvailable) {
            val age = System.currentTimeMillis() - targetFile.lastModified()
            if (age < UPDATE_THRESHOLD_MS) {
                Timber.d("GeoIP database is fresh (age: ${age / 1000 / 60 / 60}h), skipping download")
                return@withContext Result.success(false)
            }
        }

        Timber.i("Starting GeoIP database download (force=$force, available=$isAvailable)...")
        val tmpFile = File(context.filesDir, GEOIP_TMP_NAME)

        val downloaded = tryDownloadGz(tmpFile) || tryDownloadRaw(tmpFile)

        if (!downloaded) {
            tmpFile.delete()
            val err = "Failed to download GeoIP database from both compressed and raw URLs"
            Timber.w(err)
            return@withContext Result.failure(IllegalStateException(err))
        }

        if (tmpFile.length() < 1000) {
            tmpFile.delete()
            val err = "Downloaded GeoIP database is suspiciously small (${tmpFile.length()} bytes)"
            Timber.w(err)
            return@withContext Result.failure(IllegalStateException(err))
        }

        // Atomic replace
        if (targetFile.exists()) {
            targetFile.delete()
        }
        val renamed = tmpFile.renameTo(targetFile)
        if (!renamed) {
            // Fallback manual copy
            tmpFile.copyTo(targetFile, overwrite = true)
            tmpFile.delete()
        }

        Timber.i("GeoIP database successfully updated: ${targetFile.length()} bytes")
        GeoIpLookup.reload(context)
        _updateEvents.tryEmit(Unit)
        Result.success(true)
    }

    private suspend fun tryDownloadGz(tmpFile: File): Boolean {
        return runCatching {
            Timber.d("Attempting to download compressed GeoIP from $PRIMARY_GZ_URL")
            val response = client.get(PRIMARY_GZ_URL)
            if (!response.status.isSuccess()) {
                Timber.w("Failed to fetch $PRIMARY_GZ_URL: HTTP ${response.status.value}")
                return false
            }

            response.bodyAsChannel().toInputStream().use { networkStream ->
                GZIPInputStream(networkStream).use { gzStream ->
                    FileOutputStream(tmpFile).use { outStream ->
                        gzStream.copyTo(outStream)
                    }
                }
            }
            Timber.d("Decompressed GeoIP successfully: ${tmpFile.length()} bytes")
            true
        }.getOrElse { error ->
            Timber.w(error, "Failed to download/decompress GeoIP from $PRIMARY_GZ_URL")
            false
        }
    }

    private suspend fun tryDownloadRaw(tmpFile: File): Boolean {
        return runCatching {
            Timber.d("Attempting to download raw GeoIP from $FALLBACK_BIN_URL")
            val response = client.get(FALLBACK_BIN_URL)
            if (!response.status.isSuccess()) {
                Timber.w("Failed to fetch $FALLBACK_BIN_URL: HTTP ${response.status.value}")
                return false
            }

            response.bodyAsChannel().toInputStream().use { networkStream ->
                FileOutputStream(tmpFile).use { outStream ->
                    networkStream.copyTo(outStream)
                }
            }
            Timber.d("Downloaded raw GeoIP successfully: ${tmpFile.length()} bytes")
            true
        }.getOrElse { error ->
            Timber.w(error, "Failed to download raw GeoIP from $FALLBACK_BIN_URL")
            false
        }
    }
}
