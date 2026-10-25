package app.pwhs.blockads.utils

import app.pwhs.blockads.data.entities.WireGuardProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.net.Socket
import java.net.SocketTimeoutException

object WireGuardPingTester {

    private const val DEFAULT_TIMEOUT_MS = 5000
    private const val MAX_CONCURRENT_PINGS = 5

    /**
     * Measures round-trip handshake delay to the first peer of a WireGuard profile.
     * Uses Go tunnel engine's testWireGuardDelayProtected with VPN socket protection.
     * Falls back to raw connection check in JVM test environments.
     */
    suspend fun pingProfile(
        profile: WireGuardProfile,
        timeoutMs: Int = DEFAULT_TIMEOUT_MS
    ): NodePingResult = withContext(Dispatchers.IO) {
        val peer = profile.config.peers.firstOrNull { !it.endpoint.isNullOrBlank() }
        if (peer == null) {
            return@withContext NodePingResult.Error("No endpoint")
        }

        val protector = app.pwhs.blockads.service.AdBlockVpnService.tunnelSocketProtector
        try {
            val resolvedConfig = profile.config.resolveEndpoints()
            val targetEndpoint = resolvedConfig.peers.firstOrNull()?.endpoint
            timber.log.Timber.d("pingProfile: testing ${profile.name} at resolved endpoint $targetEndpoint")
            val delayMs = tunnel.Tunnel.testWireGuardDelayProtected(
                resolvedConfig.toJson(),
                timeoutMs.toLong(),
                protector
            )
            if (delayMs >= 0) {
                timber.log.Timber.d("pingProfile: success for ${profile.name} latency=${delayMs}ms")
                NodePingResult.Success(delayMs)
            } else {
                timber.log.Timber.w("pingProfile: negative delay returned for ${profile.name}")
                NodePingResult.Error("Test failed")
            }
        } catch (t: Throwable) {
            timber.log.Timber.w(t, "pingProfile: failed for ${profile.name}: ${t.message}")
            val msg = t.message.orEmpty()
            if (t is LinkageError) {
                fallbackPing(peer.endpoint, timeoutMs)
            } else if (msg.contains("timeout", ignoreCase = true) || msg.contains("deadline", ignoreCase = true)) {
                NodePingResult.Timeout
            } else {
                NodePingResult.Error(msg.ifEmpty { "Failed" })
            }
        }
    }

    private suspend fun fallbackPing(endpoint: String?, timeoutMs: Int): NodePingResult {
        if (endpoint.isNullOrBlank()) return NodePingResult.Error("Invalid endpoint")
        val lastColon = endpoint.lastIndexOf(':')
        if (lastColon <= 0) return NodePingResult.Error("Invalid endpoint")
        val host = endpoint.substring(0, lastColon).trim().removeSurrounding("[", "]")
        val port = endpoint.substring(lastColon + 1).trim().toIntOrNull() ?: return NodePingResult.Error("Invalid port")

        val resolvedHost = DnsResolverUtils.resolveHost(host) ?: host
        val socket = Socket()
        app.pwhs.blockads.service.AdBlockVpnService.socketProtector?.invoke(socket)
        val start = System.nanoTime()
        return try {
            socket.connect(InetSocketAddress(resolvedHost, port), timeoutMs)
            val latency = (System.nanoTime() - start) / 1_000_000
            NodePingResult.Success(latency)
        } catch (_: SocketTimeoutException) {
            NodePingResult.Timeout
        } catch (e: Exception) {
            NodePingResult.Error(e.message ?: "Failed")
        } finally {
            runCatching { socket.close() }
        }
    }

    suspend fun pingAll(
        profiles: List<WireGuardProfile>,
        onResult: suspend (id: String, NodePingResult) -> Unit
    ) = coroutineScope {
        val semaphore = Semaphore(MAX_CONCURRENT_PINGS)
        profiles.map { profile ->
            async {
                semaphore.withPermit {
                    onResult(profile.id, NodePingResult.Testing)
                    val result = pingProfile(profile)
                    onResult(profile.id, result)
                }
            }
        }.awaitAll()
    }
}
