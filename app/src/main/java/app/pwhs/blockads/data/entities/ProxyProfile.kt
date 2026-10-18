package app.pwhs.blockads.data.entities

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.util.UUID

@Serializable
enum class ProxyProtocol(val value: String, val displayName: String) {
    SHADOWSOCKS("shadowsocks", "SS"),
    TROJAN("trojan", "Trojan"),
    VLESS("vless", "VLESS"),
    VMESS("vmess", "VMess"),
    HYSTERIA2("hysteria2", "Hysteria 2");

    companion object {
        fun fromValue(value: String): ProxyProtocol = when (value.lowercase()) {
            "trojan" -> TROJAN
            "vless" -> VLESS
            "vmess" -> VMESS
            "hysteria2", "hy2" -> HYSTERIA2
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
    // Shadowsocks & Trojan & Hysteria2 specific
    val password: String = "",
    // VLESS & VMess specific
    val uuid: String = "",
    val alterId: Int = 0,
    // TLS / Transport
    val sni: String = "",
    val allowInsecure: Boolean = false,
    val security: String = "tls", // "tls", "reality", or "none"
    val network: String = "tcp",  // "tcp" or "ws"
    val wsPath: String = "",
    val wsHost: String = "",
    // VLESS Reality specific
    val publicKey: String = "",
    val shortId: String = "",
    val spiderX: String = "",
    val fingerprint: String = "",
    // Hysteria2 specific
    val obfs: String = "",
    val obfsPassword: String = "",
    // Metadata
    val tag: String = "",
    val rawUri: String = ""
) {
    val displayName: String
        get() = name.ifEmpty { tag.ifEmpty { "$server:$port" } }

    /** Indicates whether this profile uses DPI-resistant stealth obfuscation */
    val isObfuscated: Boolean
        get() = security.equals("reality", ignoreCase = true) ||
                protocol == ProxyProtocol.TROJAN ||
                network.equals("ws", ignoreCase = true) ||
                (protocol == ProxyProtocol.HYSTERIA2 && obfs.isNotEmpty())

    val protocolBadgeText: String
        get() = when {
            security.equals("reality", ignoreCase = true) -> "REALITY"
            protocol == ProxyProtocol.TROJAN -> "Trojan"
            network.equals("ws", ignoreCase = true) -> "${protocol.displayName}+WS"
            protocol == ProxyProtocol.HYSTERIA2 -> "Hysteria 2"
            else -> protocol.displayName
        }

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
