package app.pwhs.blockads.data.datastore.prefs

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import app.pwhs.blockads.data.entities.ShadowsocksProfile
import app.pwhs.blockads.data.entities.ShadowsocksProfileList
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class ShadowsocksPreferences(private val dataStore: DataStore<Preferences>) {

    companion object {
        val KEY_SS_PROFILES_JSON = stringPreferencesKey("ss_profiles_json")
        val KEY_SS_ACTIVE_PROFILE_ID = stringPreferencesKey("ss_active_profile_id")
        val KEY_SS_LEGACY_PROFILE_JSON = stringPreferencesKey("ss_profile_json")
        val KEY_ACTIVE_PROXY_TYPE = stringPreferencesKey("active_proxy_vpn_type")

        const val PROXY_TYPE_WIREGUARD = "wireguard"
        const val PROXY_TYPE_SHADOWSOCKS = "shadowsocks"
        const val ROUTING_MODE_SHADOWSOCKS = "shadowsocks"
    }

    val activeProxyType: Flow<String> = dataStore.data.map { prefs ->
        prefs[KEY_ACTIVE_PROXY_TYPE] ?: PROXY_TYPE_WIREGUARD
    }

    val ssProfiles: Flow<List<ShadowsocksProfile>> = dataStore.data.map { prefs ->
        readProfiles(prefs)
    }

    val activeSsProfileId: Flow<String?> = dataStore.data.map { prefs ->
        prefs[KEY_SS_ACTIVE_PROFILE_ID] ?: readProfiles(prefs).firstOrNull()?.id
    }

    val ssProfile: Flow<ShadowsocksProfile?> = dataStore.data.map { prefs ->
        val list = readProfiles(prefs)
        val activeId = prefs[KEY_SS_ACTIVE_PROFILE_ID]
        list.firstOrNull { it.id == activeId } ?: list.firstOrNull()
    }

    private fun readProfiles(prefs: Preferences): List<ShadowsocksProfile> {
        prefs[KEY_SS_PROFILES_JSON]?.let { json ->
            val list = ShadowsocksProfileList.fromJson(json).profiles
            if (list.isNotEmpty()) return list
        }
        prefs[KEY_SS_LEGACY_PROFILE_JSON]?.let { legacy ->
            runCatching { ShadowsocksProfile.fromJson(legacy) }.getOrNull()?.let {
                return listOf(it)
            }
        }
        return emptyList()
    }

    suspend fun getSsProfileSnapshot(): ShadowsocksProfile? = ssProfile.first()

    suspend fun getActiveProxyTypeSnapshot(): String = activeProxyType.first()

    suspend fun setActiveProxyType(type: String) {
        dataStore.edit { prefs ->
            prefs[KEY_ACTIVE_PROXY_TYPE] = type
        }
    }

    suspend fun setActiveProfileId(id: String) {
        dataStore.edit { prefs ->
            prefs[KEY_SS_ACTIVE_PROFILE_ID] = id
            prefs[KEY_ACTIVE_PROXY_TYPE] = PROXY_TYPE_SHADOWSOCKS
        }
    }

    suspend fun addProfile(profile: ShadowsocksProfile) {
        dataStore.edit { prefs ->
            val current = readProfiles(prefs).toMutableList()
            current.removeAll { it.id == profile.id }
            current.add(0, profile)
            prefs[KEY_SS_PROFILES_JSON] = ShadowsocksProfileList(current).toJson()
            prefs[KEY_SS_ACTIVE_PROFILE_ID] = profile.id
            prefs[KEY_ACTIVE_PROXY_TYPE] = PROXY_TYPE_SHADOWSOCKS
        }
    }

    suspend fun deleteProfile(id: String) {
        dataStore.edit { prefs ->
            val current = readProfiles(prefs).filterNot { it.id == id }
            prefs[KEY_SS_PROFILES_JSON] = ShadowsocksProfileList(current).toJson()
            if (prefs[KEY_SS_ACTIVE_PROFILE_ID] == id) {
                val nextActive = current.firstOrNull()?.id
                if (nextActive != null) {
                    prefs[KEY_SS_ACTIVE_PROFILE_ID] = nextActive
                } else {
                    prefs.remove(KEY_SS_ACTIVE_PROFILE_ID)
                }
            }
        }
    }

    suspend fun renameProfile(id: String, newName: String) {
        dataStore.edit { prefs ->
            val current = readProfiles(prefs).map {
                if (it.id == id) it.copy(name = newName) else it
            }
            prefs[KEY_SS_PROFILES_JSON] = ShadowsocksProfileList(current).toJson()
        }
    }

    suspend fun setSsProfile(profile: ShadowsocksProfile?) {
        if (profile != null) {
            addProfile(profile)
        } else {
            dataStore.edit { prefs ->
                prefs.remove(KEY_SS_PROFILES_JSON)
                prefs.remove(KEY_SS_ACTIVE_PROFILE_ID)
                prefs.remove(KEY_SS_LEGACY_PROFILE_JSON)
            }
        }
    }
}
