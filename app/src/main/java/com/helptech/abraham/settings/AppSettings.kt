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

    /* ===================== Gravação ===================== */

    /** Define o papel explicitamente. */
    suspend fun setRole(ctx: Context, role: DeviceRole) {
        ctx.dataStore.edit { p ->
            p[KEY_ROLE] = role.name
            // Se virar BALCÃO, removemos mesa
            if (role == DeviceRole.BALCAO) p.remove(KEY_TABLE)
        }
    }

    /** Ajusta o número da mesa (e garante que o papel seja MESA). */
    suspend fun setTable(ctx: Context, numeroMesa: Int) {
        val mesa = numeroMesa.coerceIn(1, 999)
        ctx.dataStore.edit { p ->
            p[KEY_ROLE] = DeviceRole.MESA.name
            p[KEY_TABLE] = mesa
        }
    }

    /** Atalhos compatíveis com o que você já tinha. */
    suspend fun saveMesa(ctx: Context, numeroMesa: Int) = setTable(ctx, numeroMesa)
    suspend fun saveBalcao(ctx: Context) = setRole(ctx, DeviceRole.BALCAO)

    /* ===================== Observação ===================== */

    /** Observa o papel como *String* (nome do enum) — sempre não-nulo. */
    fun observeRole(ctx: Context): Flow<String> =
        ctx.dataStore.data
            .map { p -> p[KEY_ROLE] ?: DeviceRole.MESA.name }
            .distinctUntilChanged()

    /** Observa a mesa como *Int* — sempre não-nulo (default = 1). */
    fun observeTable(ctx: Context): Flow<Int> =
        ctx.dataStore.data
            .map { p -> p[KEY_TABLE] ?: 1 }
            .distinctUntilChanged()

    /* ======= Utilidades síncronas (uma vez) ======= */

    suspend fun getRoleOnce(ctx: Context): DeviceRole {
        val name = ctx.dataStore.data.first()[KEY_ROLE] ?: DeviceRole.MESA.name
        return runCatching { DeviceRole.valueOf(name) }.getOrDefault(DeviceRole.MESA)
    }

    suspend fun getTableOnce(ctx: Context): Int {
        return ctx.dataStore.data.first()[KEY_TABLE] ?: 1
    }

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
