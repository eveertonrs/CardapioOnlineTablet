package com.helptech.abraham.settings

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.Flow

// Qual o papel deste dispositivo
enum class DeviceRole { MESA, BALCAO }

// DataStore (Preferences)
val Context.dataStore by preferencesDataStore(name = "app_settings")

object AppSettings {

    private val KEY_ROLE  = stringPreferencesKey("role")
    private val KEY_TABLE = intPreferencesKey("table")

    /** Salva como MESA (com número) */
    suspend fun saveMesa(ctx: Context, numeroMesa: Int) {
        ctx.dataStore.edit { p ->
            p[KEY_ROLE]  = DeviceRole.MESA.name
            p[KEY_TABLE] = numeroMesa
        }
    }

    /** Salva como BALCÃO (sem mesa) */
    suspend fun saveBalcao(ctx: Context) {
        ctx.dataStore.edit { p ->
            p[KEY_ROLE] = DeviceRole.BALCAO.name
            p.remove(KEY_TABLE)
        }
    }

    /** Observa o papel (MESA/BALCAO) em tempo real */
    fun observeRole(ctx: Context): Flow<DeviceRole?> =
        ctx.dataStore.data
            .map { p -> p[KEY_ROLE]?.let { runCatching { DeviceRole.valueOf(it) }.getOrNull() } }
            .distinctUntilChanged()

    /** Observa o número da mesa em tempo real (pode ser null no BALCÃO) */
    fun observeTable(ctx: Context): Flow<Int?> =
        ctx.dataStore.data
            .map { p -> p[KEY_TABLE] }
            .distinctUntilChanged()

    /** Útil para saber se já foi configurado no primeiro uso */
    suspend fun isConfigured(ctx: Context): Boolean {
        val p = ctx.dataStore.data.first()
        val role = p[KEY_ROLE]
        return role == DeviceRole.BALCAO.name ||
                (role == DeviceRole.MESA.name && p[KEY_TABLE] != null)
    }

    /** Limpa tudo (caso precise refazer configuração) */
    suspend fun clear(ctx: Context) {
        ctx.dataStore.edit { it.clear() }
    }
}
