package app.pwhs.blockads.data.entities

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class ShadowsocksProfile(
    val method: String,
    val password: String,
    val server: String,
    val port: Int,
    val tag: String = "",
    val rawUri: String = ""
) {
    fun toJson(): String = jsonInstance.encodeToString(serializer(), this)

    companion object {
        private val jsonInstance = Json { ignoreUnknownKeys = true }

        fun fromJson(jsonStr: String): ShadowsocksProfile =
            jsonInstance.decodeFromString(serializer(), jsonStr)
    }
}
