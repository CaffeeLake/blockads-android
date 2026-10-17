package app.pwhs.blockads.data.datastore.prefs

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import app.pwhs.blockads.data.entities.ShadowsocksProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class ShadowsocksPreferences(private val dataStore: DataStore<Preferences>) {

    companion object {
        val KEY_SS_PROFILE_JSON = stringPreferencesKey("ss_profile_json")
        const val ROUTING_MODE_SHADOWSOCKS = "shadowsocks"
    }

    val ssProfile: Flow<ShadowsocksProfile?> = dataStore.data.map { prefs ->
        prefs[KEY_SS_PROFILE_JSON]?.let {
            runCatching { ShadowsocksProfile.fromJson(it) }.getOrNull()
        }
    }

    suspend fun getSsProfileSnapshot(): ShadowsocksProfile? = ssProfile.first()

    suspend fun setSsProfile(profile: ShadowsocksProfile?) {
        dataStore.edit { prefs ->
            if (profile != null) {
                prefs[KEY_SS_PROFILE_JSON] = profile.toJson()
            } else {
                prefs.remove(KEY_SS_PROFILE_JSON)
            }
        }
    }
}
