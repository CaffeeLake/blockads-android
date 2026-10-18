package app.pwhs.blockads.data.entities

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.util.UUID

@Serializable
enum class ProxyProtocol(val value: String, val displayName: String) {
    SHADOWSOCKS("shadowsocks", "SS"),
    TROJAN("trojan", "Trojan"),
    VLESS("vless", "VLESS");

    companion object {
        fun fromValue(value: String): ProxyProtocol = when (value.lowercase()) {
            "trojan" -> TROJAN
            "vless" -> VLESS
            else -> SHADOWSOCKS
        }
    }
}

@Serializable
data class ProxyProfile(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val protocol: ProxyProtocol = ProxyProtocol.SHADOWSOCKS,
    val server: String = "",
    val port: Int = 443,
    // Shadowsocks specific
    val method: String = "",
    // Shadowsocks & Trojan specific
    val password: String = "",
    // VLESS specific
    val uuid: String = "",
    // TLS / Transport
    val sni: String = "",
    val allowInsecure: Boolean = false,
    val security: String = "tls", // "tls" or "none"
    val network: String = "tcp",  // "tcp" or "ws"
    val wsPath: String = "",
    val wsHost: String = "",
    // Metadata
    val tag: String = "",
    val rawUri: String = ""
) {
    val displayName: String
        get() = name.ifEmpty { tag.ifEmpty { "$server:$port" } }

    /** Serializes the profile into Go engine JSON config format */
    fun toEngineJson(): String = jsonInstance.encodeToString(serializer(), this)

    companion object {
        private val jsonInstance = Json { ignoreUnknownKeys = true }

        fun fromJson(jsonStr: String): ProxyProfile =
            jsonInstance.decodeFromString(serializer(), jsonStr)
    }
}

fun ShadowsocksProfile.toProxyProfile(): ProxyProfile = ProxyProfile(
    id = id,
    name = name,
    protocol = ProxyProtocol.SHADOWSOCKS,
    server = server,
    port = port,
    method = method,
    password = password,
    tag = tag,
    rawUri = rawUri
)

@Serializable
data class ProxyProfileList(
    val profiles: List<ProxyProfile> = emptyList(),
) {
    fun toJson(): String = jsonInstance.encodeToString(serializer(), this)

    companion object {
        private val jsonInstance = Json { ignoreUnknownKeys = true }

        fun fromJson(jsonStr: String): ProxyProfileList =
            try {
                jsonInstance.decodeFromString(serializer(), jsonStr)
            } catch (_: Exception) {
                ProxyProfileList()
            }
    }
}
