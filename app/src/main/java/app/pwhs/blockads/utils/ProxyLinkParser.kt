package app.pwhs.blockads.utils

import app.pwhs.blockads.data.entities.ProxyProfile
import app.pwhs.blockads.data.entities.ProxyProtocol
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

object ProxyLinkParser {
    private val json = Json { ignoreUnknownKeys = true }

    fun parse(rawUri: String): Result<ProxyProfile> = runCatching {
        val trimmed = rawUri.trim()
        when {
            trimmed.startsWith("trojan://", ignoreCase = true) -> {
                val jsonStr = tunnel.Tunnel.parseTrojanURI(trimmed)
                val obj = json.parseToJsonElement(jsonStr).jsonObject
                val server = obj["server"]?.jsonPrimitive?.content.orEmpty()
                val port = obj["port"]?.jsonPrimitive?.intOrNull ?: 443
                val password = obj["password"]?.jsonPrimitive?.content.orEmpty()
                val sni = obj["sni"]?.jsonPrimitive?.content.orEmpty()
                val allowInsecure = obj["allowInsecure"]?.jsonPrimitive?.booleanOrNull ?: false
                val network = obj["network"]?.jsonPrimitive?.content ?: "tcp"
                val wsPath = obj["wsPath"]?.jsonPrimitive?.content.orEmpty()
                val wsHost = obj["wsHost"]?.jsonPrimitive?.content.orEmpty()
                val tag = obj["tag"]?.jsonPrimitive?.content.orEmpty()

                ProxyProfile(
                    name = tag.ifEmpty { "$server:$port" },
                    protocol = ProxyProtocol.TROJAN,
                    server = server,
                    port = port,
                    password = password,
                    sni = sni,
                    allowInsecure = allowInsecure,
                    network = network,
                    wsPath = wsPath,
                    wsHost = wsHost,
                    tag = tag,
                    rawUri = trimmed
                )
            }
            trimmed.startsWith("vless://", ignoreCase = true) -> {
                val jsonStr = tunnel.Tunnel.parseVlessURI(trimmed)
                val obj = json.parseToJsonElement(jsonStr).jsonObject
                val server = obj["server"]?.jsonPrimitive?.content.orEmpty()
                val port = obj["port"]?.jsonPrimitive?.intOrNull ?: 443
                val uuid = obj["uuid"]?.jsonPrimitive?.content.orEmpty()
                val sni = obj["sni"]?.jsonPrimitive?.content.orEmpty()
                val security = obj["security"]?.jsonPrimitive?.content ?: "tls"
                val allowInsecure = obj["allowInsecure"]?.jsonPrimitive?.booleanOrNull ?: false
                val network = obj["network"]?.jsonPrimitive?.content ?: "tcp"
                val wsPath = obj["wsPath"]?.jsonPrimitive?.content.orEmpty()
                val wsHost = obj["wsHost"]?.jsonPrimitive?.content.orEmpty()
                val tag = obj["tag"]?.jsonPrimitive?.content.orEmpty()

                ProxyProfile(
                    name = tag.ifEmpty { "$server:$port" },
                    protocol = ProxyProtocol.VLESS,
                    server = server,
                    port = port,
                    uuid = uuid,
                    sni = sni,
                    security = security,
                    allowInsecure = allowInsecure,
                    network = network,
                    wsPath = wsPath,
                    wsHost = wsHost,
                    tag = tag,
                    rawUri = trimmed
                )
            }
            trimmed.startsWith("ss://", ignoreCase = true) -> {
                val jsonStr = tunnel.Tunnel.parseShadowsocksURI(trimmed)
                val obj = json.parseToJsonElement(jsonStr).jsonObject
                val server = obj["server"]?.jsonPrimitive?.content.orEmpty()
                val port = obj["port"]?.jsonPrimitive?.intOrNull ?: 8388
                val method = obj["method"]?.jsonPrimitive?.content.orEmpty()
                val password = obj["password"]?.jsonPrimitive?.content.orEmpty()
                val tag = obj["tag"]?.jsonPrimitive?.content.orEmpty()

                ProxyProfile(
                    name = tag.ifEmpty { "$server:$port" },
                    protocol = ProxyProtocol.SHADOWSOCKS,
                    server = server,
                    port = port,
                    method = method,
                    password = password,
                    tag = tag,
                    rawUri = trimmed
                )
            }
            else -> throw IllegalArgumentException("Unsupported proxy URI. Expected trojan://, vless://, or ss://")
        }
    }
}
