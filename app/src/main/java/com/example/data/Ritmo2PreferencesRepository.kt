package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.model.ExtremeVisualStyleConfig
import com.example.model.MusicalProfileConfig
import com.example.model.PlayerDesignerConfig
import com.example.model.SavedAmbientMix
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.ritmo2DataStore: DataStore<Preferences> by preferencesDataStore(name = "ritmolocal_v2_prefs")

class Ritmo2PreferencesRepository(context: Context) {

    private val dataStore = context.applicationContext.ritmo2DataStore

    private val extremeVisualStyleKey = stringPreferencesKey("extreme_visual_style_json")
    private val playerDesignerConfigKey = stringPreferencesKey("player_designer_config_json")
    private val musicalProfileConfigKey = stringPreferencesKey("musical_profile_config_json")
    private val savedAmbientMixesKey = stringPreferencesKey("saved_ambient_mixes_json")

    val extremeVisualStyle: Flow<ExtremeVisualStyleConfig> = dataStore.data.map { prefs ->
        ExtremeVisualStyleConfig.decode(prefs[extremeVisualStyleKey].orEmpty())
    }

    val playerDesignerConfig: Flow<PlayerDesignerConfig> = dataStore.data.map { prefs ->
        PlayerDesignerConfig.decode(prefs[playerDesignerConfigKey].orEmpty())
    }

    val musicalProfileConfig: Flow<MusicalProfileConfig> = dataStore.data.map { prefs ->
        MusicalProfileConfig.decode(prefs[musicalProfileConfigKey].orEmpty())
    }

    val savedAmbientMixes: Flow<List<SavedAmbientMix>> = dataStore.data.map { prefs ->
        SavedAmbientMix.decodeList(prefs[savedAmbientMixesKey].orEmpty())
    }

    suspend fun saveExtremeVisualStyle(config: ExtremeVisualStyleConfig) {
        dataStore.edit { it[extremeVisualStyleKey] = ExtremeVisualStyleConfig.encode(config) }
    }

    suspend fun restoreExtremeVisualDefaults() {
        dataStore.edit { it.remove(extremeVisualStyleKey) }
    }

    suspend fun savePlayerDesignerConfig(config: PlayerDesignerConfig) {
        dataStore.edit { it[playerDesignerConfigKey] = PlayerDesignerConfig.encode(config) }
    }

    suspend fun restorePlayerDesignerDefaults() {
        dataStore.edit { it.remove(playerDesignerConfigKey) }
    }

    suspend fun saveMusicalProfileConfig(config: MusicalProfileConfig) {
        dataStore.edit { it[musicalProfileConfigKey] = MusicalProfileConfig.encode(config) }
    }

    suspend fun saveAmbientMix(mix: SavedAmbientMix) {
        dataStore.edit { prefs ->
            val current = SavedAmbientMix.decodeList(prefs[savedAmbientMixesKey].orEmpty())
                .filterNot { it.id == mix.id || it.name.equals(mix.name, ignoreCase = true) }
            prefs[savedAmbientMixesKey] = SavedAmbientMix.encodeList((listOf(mix) + current).take(24))
        }
    }

    suspend fun deleteAmbientMix(mixId: String) {
        dataStore.edit { prefs ->
            val filtered = SavedAmbientMix.decodeList(prefs[savedAmbientMixesKey].orEmpty())
                .filterNot { it.id == mixId }
            prefs[savedAmbientMixesKey] = SavedAmbientMix.encodeList(filtered)
        }
    }
}
