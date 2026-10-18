package app.pwhs.blockads.utils

import app.pwhs.blockads.data.entities.ProxyProtocol
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SubscriptionImporterTest {

    @Test
    fun parseWireGuardUri_extractsAllParameters() {
        val uri = "wireguard://eAIusKTFAsAqSrgnwwGy0SNhXvvd%2BYji9yQOJVAe43Q%3D@vpn.pwhs.app:57442?address=10.0.0.2%2F32&mtu=1420&publickey=LFYiC1ZLXNNdIoZ4z1XKuL3ctV6Z3D1O1NS%2Bl6GwA3E%3D#Wireguard"
        val profile = WireGuardConfigParser.parseUri(uri)

        assertEquals("Wireguard", profile.name)
        assertEquals("eAIusKTFAsAqSrgnwwGy0SNhXvvd+Yji9yQOJVAe43Q=", profile.config.interfaceConfig.privateKey)
        assertEquals(listOf("10.0.0.2/32"), profile.config.interfaceConfig.address)
        assertEquals(1, profile.config.peers.size)
        val peer = profile.config.peers[0]
        assertEquals("LFYiC1ZLXNNdIoZ4z1XKuL3ctV6Z3D1O1NS+l6GwA3E=", peer.publicKey)
        assertEquals("vpn.pwhs.app:57442", peer.endpoint)
        assertEquals(listOf("0.0.0.0/0", "::/0"), peer.allowedIPs)
    }

    @Test
    fun parseVmessUri_extractsJsonFields() {
        val uri = "vmess://ewogICJhZGQiOiAidnBuLnB3aHMuYXBwIiwKICAiYWxwbiI6ICJoMixodHRwLzEuMSIsCiAgImZwIjogImNocm9tZSIsCiAgImlkIjogImY1OTk1ODViLThkYzktNDY2OS1iMGZkLTMyMzBiMjU1MzdiZSIsCiAgIm5ldCI6ICJ0Y3AiLAogICJwb3J0IjogNTMyMTYsCiAgInBzIjogIlZNRVNTIiwKICAic2N5IjogImF1dG8iLAogICJzbmkiOiAidnBuLnB3aHMuYXBwIiwKICAidGxzIjogInRscyIsCiAgInR5cGUiOiAibm9uZSIsCiAgInYiOiAiMiIKfQ=="
        val result = ProxyLinkParser.parse(uri)

        assertTrue(result.isSuccess)
        val profile = result.getOrThrow()
        assertEquals(ProxyProtocol.VMESS, profile.protocol)
        assertEquals("VMESS", profile.name)
        assertEquals("vpn.pwhs.app", profile.server)
        assertEquals(53216, profile.port)
        assertEquals("f599585b-8dc9-4669-b0fd-3230b25537be", profile.uuid)
        assertEquals("tls", profile.security)
        assertEquals("vpn.pwhs.app", profile.sni)
        assertEquals("tcp", profile.network)
    }

    @Test
    fun parseSubscriptionContent_decodesBase64AndExtractsAllNodes() {
        val base64Sub = "dHJvamFuOi8vNFh1Yk1pRDZKOVN4dFQ5ajE3blhOY0NadXAlMkZEQ2V1OW0lMkJ2QXBnUVFJalElM0RAdnBuLnB3aHMuYXBwOjE5MDk1P2FscG49aDIlMkNodHRwJTJGMS4xJmZwPWNocm9tZSZzZWN1cml0eT10bHMmc25pPXZwbi5wd2hzLmFwcCZ0eXBlPXRjcCNUcm9qYW4tVlBOLW10bHBtYm1mN2cKdmxlc3M6Ly9mNTk5NTg1Yi04ZGM5LTQ2NjktYjBmZC0zMjMwYjI1NTM3YmVAdnBuLnB3aHMuYXBwOjUxODI3P2VuY3J5cHRpb249bm9uZSZmcD1jaHJvbWUmcGJrPVNUTWI3X1RaUDJWSXNjaUNMTDFZTzAzMGVHS2ZZRlN0S19KczJzamozaVEmc2VjdXJpdHk9cmVhbGl0eSZzaWQ9MTYwY2Y2ZjUmc25pPWl0dW5lcy5hcHBsZS5jb20mc3B4PSUyRmU1YTk4NWQ1OWNhYmE3NyZzdXBwb3J0LXgyNTUxOW1sa2VtNzY4PXRydWUmdHlwZT10Y3AjVkxFU1MtUmVhbGl0eQp2bWVzczovL2V3b2dJQ0poWkdRaU9pQWlkbkJ1TG5CM2FITXVZWEJ3SWl3S0lDQWlZV3h3YmlJNklDSm9NaXhvZEhSd0x6RXVNU0lzQ2lBZ0ltWndJam9nSW1Ob2NtOXRaU0lzQ2lBZ0ltbGtJam9nSW1ZMU9UazFPRFZpTFRoa1l6a3RORFkyT1MxaU1HWmtMVE15TXpCaU1qVTFNemRpWlNJc0NpQWdJbTVsZENJNklDSjBZM0FpTEFvZ0lDSndiM0owSWpvZ05UTXlNVFlzQ2lBZ0luQnpJam9nSWxaTlJWTlRJaXdLSUNBaWMyTjVJam9nSW1GMWRHOGlMQW9nSUNKemJta2lPaUFpZG5CdUxuQjNhSE11WVhCd0lpd0tJQ0FpZEd4eklqb2dJblJzY3lJc0NpQWdJblI1Y0dVaU9pQWlibTl1WlNJc0NpQWdJbllpT2lBaU1pSUtmUT09CnNzOi8vMjAyMi1ibGFrZTMtYWVzLTI1Ni1nY206czI5YW1TcG9laFd3NjRMU0YwSWk2dHYzRG5NR0FwTSUyRmtaSmRJZnR4YVhJJTNEOjRYdWJNaUQ2SjlTeHRUOWoxN25YTmNDWnVwJTJGRENldTltJTJCdkFwZ1FRSWpRJTNEQHZwbi5wd2hzLmFwcDoyMTczNj9hbHBuPWgyJTJDaHR0cCUyRjEuMSZmcD1jaHJvbWUmc2VjdXJpdHk9dGxzJnNuaT12cG4ucHdocy5hcHAmdHlwZT10Y3AjU2hhZG93c29ja3MKd2lyZWd1YXJkOi8vZUFJdXNLVEZBc0FxU3Jnbnd3R3kwU05oWHZ2ZCUyQllqaTl5UU9KVkFlNDNRJTNEQHZwbi5wd2hzLmFwcDo1NzQ0Mj9hZGRyZXNzPTEwLjAuMC4yJTJGMzImbXR1PTE0MjAmcHVibGlja2V5PUxGWWlDMVpMWE5OZElvWjR6MVhLdUwzY3RWNlozRDFPMU5TJTJCbDZHd0EzRSUzRCNXaXJlZ3VhcmQKaHlzdGVyaWEyOi8vbHJoYzFiMzIwNjA1aHBvaEB2cG4ucHdocy5hcHA6NDM4MjI/YWxwbj1oMyZvYmZzPXNhbGFtYW5kZXImb2Jmcy1wYXNzd29yZD1saWo2eTdxZmZmb3JmbWVxJnNlY3VyaXR5PXRscyZzbmk9dnBuLnB3aHMuYXBwI0h5c3RlcmlhCg=="
        val result = SubscriptionImporter.parseSubscriptionContent(base64Sub)

        assertEquals(1, result.wireGuardProfiles.size)
        assertEquals("Wireguard", result.wireGuardProfiles[0].name)

        assertEquals(5, result.proxyProfiles.size)
        val protocols = result.proxyProfiles.map { it.protocol }
        assertTrue(protocols.contains(ProxyProtocol.TROJAN))
        assertTrue(protocols.contains(ProxyProtocol.VLESS))
        assertTrue(protocols.contains(ProxyProtocol.VMESS))
        assertTrue(protocols.contains(ProxyProtocol.SHADOWSOCKS))
        assertTrue(protocols.contains(ProxyProtocol.HYSTERIA2))

        // Check VLESS Reality
        val vless = result.proxyProfiles.first { it.protocol == ProxyProtocol.VLESS }
        assertEquals("reality", vless.security)
        assertEquals("STMb7_TZP2VIsciCLL1YO030eGKfYFStK_Js2sjj3iQ", vless.publicKey)
        assertEquals("itunes.apple.com", vless.sni)
    }
}
