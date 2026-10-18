package app.pwhs.blockads.data.entities

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Represents a parsed WireGuard configuration file.
 * Strictly separates the [Interface] and [Peer] sections.
 *
 * Serializable to JSON for persistence in DataStore and passing to Go engine.
 */
@Serializable
data class WireGuardConfig(
    val interfaceConfig: WireGuardInterface,
    val peers: List<WireGuardPeer>
) {
    val isAmnezia: Boolean
        get() = interfaceConfig.isAmnezia

    fun toJson(): String = json.encodeToString(this)

    companion object {
        private val json = Json { ignoreUnknownKeys = true }

        fun fromJson(jsonStr: String): WireGuardConfig =
            json.decodeFromString<WireGuardConfig>(jsonStr)
    }
}

/**
 * Represents the [Interface] section of a WireGuard / AmneziaWG .conf file.
 */
@Serializable
data class WireGuardInterface(
    val privateKey: String,
    val address: List<String>,
    val listenPort: Int? = null,
    val dns: List<String> = emptyList(),
    // AmneziaWG obfuscation parameters
    val jc: Int? = null,
    val jmin: Int? = null,
    val jmax: Int? = null,
    val s1: Int? = null,
    val s2: Int? = null,
    val s3: Int? = null,
    val s4: Int? = null,
    val h1: Long? = null,
    val h2: Long? = null,
    val h3: Long? = null,
    val h4: Long? = null
) {
    val isAmnezia: Boolean
        get() = jc != null || s1 != null || h1 != null
}

/**
 * Represents a [Peer] section of a WireGuard .conf file.
 * A config may contain multiple peers.
 */
@Serializable
data class WireGuardPeer(
    val publicKey: String,
    val presharedKey: String? = null,
    val endpoint: String? = null,
    @SerialName("allowedIPs")
    val allowedIPs: List<String> = emptyList(),
    val persistentKeepalive: Int? = null
)
