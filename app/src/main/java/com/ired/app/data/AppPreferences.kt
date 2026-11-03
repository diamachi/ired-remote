package com.ired.app.data

import android.content.Context
import android.content.SharedPreferences
import com.ired.app.model.RemoteType
import com.ired.app.model.UserRemote
import org.json.JSONArray

class AppPreferences(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("ired_preferences", Context.MODE_PRIVATE)

    fun loadRemotes(): List<UserRemote> {
        val jsonStr = prefs.getString(KEY_REMOTES_LIST, null)
        if (jsonStr.isNullOrEmpty()) {
            val defaults = createDefaultRemotes()
            saveRemotes(defaults)
            return defaults
        }

        return try {
            val jsonArray = JSONArray(jsonStr)
            val list = mutableListOf<UserRemote>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.optJSONObject(i)
                if (obj != null) {
                    list.add(UserRemote.fromJson(obj))
                }
            }
            if (jsonStr == "[]") {
                emptyList()
            } else if (list.isEmpty()) {
                emptyList()
            } else {
                // Migrate only the known combined legacy remote; preserve user deletions.
                val hasCombined = list.any { it.id == "lg_tv_gtv" }
                if (hasCombined) {
                    val migrated = mutableListOf<UserRemote>()
                    for (remote in list) {
                        if (remote.id == "lg_tv_gtv") {
                            migrated.add(remote.copy(id = "lg_webos", name = "LG webOS TV", type = RemoteType.LG_TV))
                            migrated.add(UserRemote(id = "google_tv", name = "Google TV Streamer", type = RemoteType.GOOGLE_TV))
                        } else {
                            migrated.add(remote)
                        }
                    }
                    saveRemotes(migrated)
                    migrated
                } else {
                    list
                }
            }
        } catch (_: Exception) {
            val defaults = createDefaultRemotes()
            saveRemotes(defaults)
            defaults
        }
    }

    fun saveRemotes(remotes: List<UserRemote>) {
        val jsonArray = JSONArray()
        remotes.forEach { jsonArray.put(it.toJson()) }
        prefs.edit().putString(KEY_REMOTES_LIST, jsonArray.toString()).apply()
    }

    private fun createDefaultRemotes(): List<UserRemote> {
        return listOf(
            UserRemote(
                id = "lg_webos",
                name = "LG webOS TV",
                type = RemoteType.LG_TV
            ),
            UserRemote(
                id = "google_tv",
                name = "Google TV Streamer",
                type = RemoteType.GOOGLE_TV
            ),
            UserRemote(
                id = "panasonic_ac",
                name = "Panasonic AC",
                type = RemoteType.PANASONIC_AC
            ),
            UserRemote(
                id = "cherry_lights",
                name = "Cherry Lights",
                type = RemoteType.CHERRY_LIGHTS
            )
        )
    }

    private fun tvKey(remoteId: String, suffix: String) = "pref_gtv_${remoteId}_$suffix"

    fun getGoogleTvHost(remoteId: String): String? = prefs.getString(tvKey(remoteId, "host"), null)
    fun setGoogleTvHost(remoteId: String, host: String?) = prefs.edit().putString(tvKey(remoteId, "host"), host).apply()
    fun isGoogleTvPaired(remoteId: String): Boolean = prefs.getBoolean(tvKey(remoteId, "paired"), false)
    fun setGoogleTvPaired(remoteId: String, paired: Boolean) = prefs.edit().putBoolean(tvKey(remoteId, "paired"), paired).apply()
    fun getGoogleTvFingerprint(remoteId: String): String? = prefs.getString(tvKey(remoteId, "fingerprint"), null)
    fun setGoogleTvFingerprint(remoteId: String, value: String?) = prefs.edit().putString(tvKey(remoteId, "fingerprint"), value).apply()
    fun getGoogleTvName(remoteId: String): String = prefs.getString(tvKey(remoteId, "name"), "Google TV Streamer") ?: "Google TV Streamer"
    fun setGoogleTvName(remoteId: String, name: String) = prefs.edit().putString(tvKey(remoteId, "name"), name).apply()

    fun getGoogleTvUsesLgIrVolume(remoteId: String): Boolean = prefs.getBoolean(tvKey(remoteId, "lg_ir_volume"), false)
    fun setGoogleTvUsesLgIrVolume(remoteId: String, enabled: Boolean) = prefs.edit().putBoolean(tvKey(remoteId, "lg_ir_volume"), enabled).apply()

    fun clearGoogleTvProfile(remoteId: String) {
        prefs.edit().remove(tvKey(remoteId, "host")).remove(tvKey(remoteId, "paired"))
            .remove(tvKey(remoteId, "fingerprint")).remove(tvKey(remoteId, "name"))
            .remove(tvKey(remoteId, "lg_ir_volume")).apply()
    }

    companion object {
        private const val KEY_REMOTES_LIST = "pref_remotes_list_v5"
    }
}
