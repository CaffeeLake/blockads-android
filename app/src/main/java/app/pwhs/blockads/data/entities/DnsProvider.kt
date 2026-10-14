package app.pwhs.blockads.data.entities

import androidx.annotation.DrawableRes
import app.pwhs.blockads.R

data class DnsProvider(
    val id: String,
    val name: String,
    val category: DnsCategory,
    val ipAddress: String,
    val dohUrl: String? = null,
    val description: String,
    @DrawableRes val iconRes: Int? = null,
    val odohRelayUrl: String? = null
)

enum class DnsCategory {
    STANDARD,
    PRIVACY,
    FAMILY,
    CUSTOM
}

object DnsProviders {
    val SYSTEM = DnsProvider(
        id = "system",
        name = "System Default",
        category = DnsCategory.STANDARD,
        ipAddress = "0.0.0.0", // Dynamically resolved at runtime
        dohUrl = null,
        description = "Use your network's default DNS servers",
        iconRes = R.drawable.ic_dns_system
    )

    val GOOGLE = DnsProvider(
        id = "google",
        name = "Google DNS",
        category = DnsCategory.STANDARD,
        ipAddress = "8.8.8.8",
        dohUrl = "https://dns.google/dns-query",
        description = "Fast and reliable DNS service by Google",
        iconRes = R.drawable.ic_brand_google
    )

    val CLOUDFLARE = DnsProvider(
        id = "cloudflare",
        name = "Cloudflare DNS",
        category = DnsCategory.PRIVACY,
        ipAddress = "1.1.1.1",
        dohUrl = "https://cloudflare-dns.com/dns-query",
        description = "Privacy-focused, fastest DNS resolver",
        iconRes = R.drawable.ic_dns_cloudflare
    )

    val CLOUDFLARE_ODOH = DnsProvider(
        id = "cloudflare_odoh",
        name = "Cloudflare (ODoH)",
        category = DnsCategory.PRIVACY,
        ipAddress = "1.1.1.1",
        dohUrl = "https://odoh.cloudflare-dns.com/dns-query",
        odohRelayUrl = "https://odoh-relay.edgecompute.app/",
        description = "Oblivious DoH separating IP from DNS queries via Fastly relay",
        iconRes = R.drawable.ic_dns_cloudflare
    )

    val ADGUARD = DnsProvider(
        id = "adguard",
        name = "AdGuard DNS",
        category = DnsCategory.PRIVACY,
        ipAddress = "94.140.14.14",
        dohUrl = "https://dns.adguard-dns.com/dns-query",
        description = "Blocks ads and trackers at DNS level",
        iconRes = R.drawable.ic_dns_adguard
    )

    val QUAD9 = DnsProvider(
        id = "quad9",
        name = "Quad9",
        category = DnsCategory.PRIVACY,
        ipAddress = "9.9.9.9",
        dohUrl = "https://dns.quad9.net/dns-query",
        description = "Security and privacy-focused DNS",
        iconRes = R.drawable.ic_dns_quad9
    )

    val QUAD9_DOQ = DnsProvider(
        id = "quad9_doq",
        name = "Quad9 (DoQ)",
        category = DnsCategory.PRIVACY,
        ipAddress = "9.9.9.9",
        dohUrl = "quic://dns.quad9.net",
        description = "Security and privacy-focused DNS over QUIC",
        iconRes = R.drawable.ic_dns_quad9
    )

    val OPENDNS = DnsProvider(
        id = "opendns",
        name = "OpenDNS",
        category = DnsCategory.STANDARD,
        ipAddress = "208.67.222.222",
        dohUrl = null,
        description = "Reliable DNS with optional content filtering",
        iconRes = R.drawable.ic_dns_opendns
    )

    val OPENDNS_FAMILY = DnsProvider(
        id = "opendns_family",
        name = "OpenDNS Family Shield",
        category = DnsCategory.FAMILY,
        ipAddress = "208.67.222.123",
        dohUrl = null,
        description = "Family-safe DNS blocking adult content",
        iconRes = R.drawable.ic_dns_opendns
    )

    val CLOUDFLARE_FAMILY = DnsProvider(
        id = "cloudflare_family",
        name = "Cloudflare Family",
        category = DnsCategory.FAMILY,
        ipAddress = "1.1.1.3",
        dohUrl = "https://family.cloudflare-dns.com/dns-query",
        description = "Cloudflare DNS with malware and adult content blocking",
        iconRes = R.drawable.ic_dns_cloudflare
    )

    val DNSBUNKER = DnsProvider(
        id = "dnsbunker",
        name = "DNSBunker",
        category = DnsCategory.PRIVACY,
        ipAddress = "185.250.250.61",
        dohUrl = "https://dnsbunker.org/dns-query",
        description = "Encrypted DNS from Germany with Hagezi ad, tracker & threat blocking",
        iconRes = R.drawable.ic_dns_dnsbunker
    )

    val DNSBUNKER_DOQ = DnsProvider(
        id = "dnsbunker_doq",
        name = "DNSBunker (DoQ)",
        category = DnsCategory.PRIVACY,
        ipAddress = "185.250.250.61",
        dohUrl = "quic://dnsbunker.org",
        description = "DNS over QUIC from Germany with Hagezi ad & threat blocking",
        iconRes = R.drawable.ic_dns_dnsbunker
    )

    val ALL_PROVIDERS = listOf(
        SYSTEM,
        ADGUARD,
        CLOUDFLARE,
        CLOUDFLARE_ODOH,
        CLOUDFLARE_FAMILY,
        DNSBUNKER,
        DNSBUNKER_DOQ,
        GOOGLE,
        OPENDNS,
        OPENDNS_FAMILY,
        QUAD9,
        QUAD9_DOQ
    )

    fun getById(id: String): DnsProvider? = ALL_PROVIDERS.find { it.id == id }

    fun getByIp(ip: String): DnsProvider? = ALL_PROVIDERS.find { it.ipAddress == ip }

    fun getSecondaryIp(provider: DnsProvider): String? = when (provider.id) {
        QUAD9.id, QUAD9_DOQ.id -> "149.112.112.112"
        CLOUDFLARE.id, CLOUDFLARE_ODOH.id -> "1.0.0.1"
        CLOUDFLARE_FAMILY.id -> "1.0.0.3"
        ADGUARD.id -> "94.140.14.15"
        GOOGLE.id -> "8.8.4.4"
        OPENDNS.id -> "208.67.220.220"
        OPENDNS_FAMILY.id -> "208.67.220.123"
        DNSBUNKER.id, DNSBUNKER_DOQ.id -> "185.250.250.62"
        else -> null
    }
}
