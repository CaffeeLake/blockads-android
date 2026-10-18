package app.pwhs.blockads.utils

import app.pwhs.blockads.data.entities.WireGuardConfig
import app.pwhs.blockads.data.entities.WireGuardInterface
import app.pwhs.blockads.data.entities.WireGuardPeer
import app.pwhs.blockads.data.entities.WireGuardProfile
import java.net.URI
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

/**
 * Robust parser for standard WireGuard .conf files and wireguard:// URIs.
 *
 * Handles:
 * - [Interface] and multiple [Peer] sections
 * - Comments (lines starting with #)
 * - Empty / blank lines
 * - Whitespace around keys and values
 * - wireguard:// URIs
 */
object WireGuardConfigParser {

    private enum class Section { NONE, INTERFACE, PEER }

    private val sectionRegex = Regex("""^\[(\w+)]$""", RegexOption.IGNORE_CASE)

    /**
     * Parse the raw text content of a WireGuard .conf file.
     *
     * @throws IllegalArgumentException if required fields are missing.
     */
    fun parse(raw: String): WireGuardConfig {
        var currentSection = Section.NONE

        // Interface fields
        var privateKey: String? = null
        val addresses = mutableListOf<String>()
        var listenPort: Int? = null
        val dns = mutableListOf<String>()

        // Peer accumulator
        data class PeerBuilder(
            var publicKey: String? = null,
            var presharedKey: String? = null,
            var endpoint: String? = null,
            val allowedIPs: MutableList<String> = mutableListOf(),
            var persistentKeepalive: Int? = null,
        )

        val peers = mutableListOf<PeerBuilder>()
        var currentPeer: PeerBuilder? = null

        for (line in raw.lines()) {
            val trimmed = line.trim()

            // Skip empty lines and comments
            if (trimmed.isEmpty() || trimmed.startsWith("#")) continue

            // Check for section headers
            val sectionMatch = sectionRegex.find(trimmed)
            if (sectionMatch != null) {
                val sectionName = sectionMatch.groupValues[1]
                when (sectionName.lowercase()) {
                    "interface" -> {
                        currentSection = Section.INTERFACE
                    }
                    "peer" -> {
                        // Finalize previous peer if any
                        currentPeer?.let { peers.add(it) }
                        currentPeer = PeerBuilder()
                        currentSection = Section.PEER
                    }
                    else -> {
                        // Unknown section, skip
                        currentSection = Section.NONE
                    }
                }
                continue
            }

            // Parse key = value
            val eqIndex = trimmed.indexOf('=')
            if (eqIndex == -1) continue

            val key = trimmed.substring(0, eqIndex).trim()
            val value = trimmed.substring(eqIndex + 1).trim()

            when (currentSection) {
                Section.INTERFACE -> {
                    when (key.lowercase()) {
                        "privatekey" -> privateKey = value
                        "address" -> addresses.addAll(
                            value.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                        )
                        "listenport" -> listenPort = value.toIntOrNull()
                        "dns" -> dns.addAll(
                            value.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                        )
                    }
                }
                Section.PEER -> {
                    val peer = currentPeer ?: continue
                    when (key.lowercase()) {
                        "publickey" -> peer.publicKey = value
                        "presharedkey" -> peer.presharedKey = value
                        "endpoint" -> peer.endpoint = value
                        "allowedips" -> peer.allowedIPs.addAll(
                            value.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                        )
                        "persistentkeepalive" -> peer.persistentKeepalive = value.toIntOrNull()
                    }
                }
                Section.NONE -> { /* ignore lines outside any section */ }
            }
        }

        // Finalize last peer
        currentPeer?.let { peers.add(it) }

        // Validate required fields
        requireNotNull(privateKey) { "Missing required field: PrivateKey in [Interface]" }
        require(addresses.isNotEmpty()) { "Missing required field: Address in [Interface]" }

        val parsedPeers = peers.map { pb ->
            requireNotNull(pb.publicKey) { "Missing required field: PublicKey in [Peer]" }
            WireGuardPeer(
                publicKey = pb.publicKey!!,
                presharedKey = pb.presharedKey,
                endpoint = pb.endpoint,
                allowedIPs = pb.allowedIPs.toList(),
                persistentKeepalive = pb.persistentKeepalive
            )
        }

        return WireGuardConfig(
            interfaceConfig = WireGuardInterface(
                privateKey = privateKey,
                address = addresses.toList(),
                listenPort = listenPort,
                dns = dns.toList()
            ),
            peers = parsedPeers
        )
    }

    /**
     * Parses a wireguard:// URI into a [WireGuardProfile].
     *
     * Example:
     * wireguard://<private_key>@<host>:<port>?address=<ip>&mtu=<mtu>&publickey=<pub_key>#<name>
     */
    fun parseUri(rawUri: String): WireGuardProfile {
        val trimmed = rawUri.trim()
        val uri = URI(trimmed)
        require(uri.scheme.equals("wireguard", ignoreCase = true)) { "Invalid scheme: ${uri.scheme}" }

        val rawUserInfo = uri.rawUserInfo ?: throw IllegalArgumentException("Missing private key in userInfo")
        val privateKey = URLDecoder.decode(rawUserInfo, StandardCharsets.UTF_8.name())

        val host = uri.host ?: throw IllegalArgumentException("Missing host in wireguard URI")
        val port = if (uri.port != -1) uri.port else 51820
        val endpoint = "$host:$port"

        val queryParams = mutableMapOf<String, String>()
        uri.rawQuery?.split("&")?.forEach { part ->
            val eq = part.indexOf('=')
            if (eq != -1) {
                val k = URLDecoder.decode(part.substring(0, eq), StandardCharsets.UTF_8.name()).lowercase()
                val v = URLDecoder.decode(part.substring(eq + 1), StandardCharsets.UTF_8.name())
                queryParams[k] = v
            }
        }

        val addresses = queryParams["address"]?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() }
            ?: listOf("10.0.0.2/32")
        val publicKey = queryParams["publickey"] ?: throw IllegalArgumentException("Missing publickey query param")
        val presharedKey = queryParams["presharedkey"]
        val allowedIps = queryParams["allowedips"]?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() }
            ?: listOf("0.0.0.0/0", "::/0")
        val dns = queryParams["dns"]?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() } ?: emptyList()
        val keepalive = queryParams["persistentkeepalive"]?.toIntOrNull()

        val rawFragment = uri.rawFragment
        val name = if (!rawFragment.isNullOrEmpty()) {
            URLDecoder.decode(rawFragment, StandardCharsets.UTF_8.name())
        } else {
            "WireGuard-${host}"
        }

        val config = WireGuardConfig(
            interfaceConfig = WireGuardInterface(
                privateKey = privateKey,
                address = addresses,
                dns = dns
            ),
            peers = listOf(
                WireGuardPeer(
                    publicKey = publicKey,
                    presharedKey = presharedKey,
                    endpoint = endpoint,
                    allowedIPs = allowedIps,
                    persistentKeepalive = keepalive
                )
            )
        )

        return WireGuardProfile(
            id = WireGuardProfile.newId(),
            name = name,
            config = config
        )
    }
}
