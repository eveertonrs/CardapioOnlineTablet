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
import com.helptech.abraham.BuildConfig

enum class DeviceRole { MESA, BALCAO }

val Context.dataStore by preferencesDataStore(name = "app_settings")

object AppSettings {

    private val KEY_ROLE       = stringPreferencesKey("role")
    private val KEY_TABLE      = intPreferencesKey("table")
    private val KEY_EMPRESA    = stringPreferencesKey("empresa")
    private val KEY_API_TOKEN  = stringPreferencesKey("api_token")
    private val KEY_USUARIO    = stringPreferencesKey("usuario")
    // >>> NOVO: base dinâmica (online x local)
    private val KEY_DEVICE_SERIAL = stringPreferencesKey("device_serial")
    private val KEY_BASE_URL   = stringPreferencesKey("base_url")

    /* ====== gravação ====== */
    suspend fun setRole(ctx: Context, role: DeviceRole) {
        ctx.dataStore.edit { p ->
            p[KEY_ROLE] = role.name
            if (role == DeviceRole.BALCAO) p.remove(KEY_TABLE)
        }
    }

    suspend fun setTable(ctx: Context, numeroMesa: Int) {
        ctx.dataStore.edit { p ->
            p[KEY_ROLE] = DeviceRole.MESA.name
            p[KEY_TABLE] = numeroMesa.coerceIn(1, 999)
        }
    }

    suspend fun saveMesa(ctx: Context, numeroMesa: Int) = setTable(ctx, numeroMesa)
    suspend fun saveBalcao(ctx: Context) = setRole(ctx, DeviceRole.BALCAO)

    suspend fun saveEmpresa(ctx: Context, empresa: String) {
        ctx.dataStore.edit { it[KEY_EMPRESA] = empresa.lowercase() }
    }

    suspend fun saveApiToken(ctx: Context, token: String) {
        ctx.dataStore.edit { it[KEY_API_TOKEN] = token }
    }

    suspend fun saveUsuario(ctx: Context, usuario: String) {
        ctx.dataStore.edit { it[KEY_USUARIO] = usuario }
    }

    // >>> NOVO: gravar serial do dispositivo
    suspend fun saveDeviceSerial(ctx: Context, serial: String) {
        ctx.dataStore.edit { it[KEY_DEVICE_SERIAL] = serial }
    }
    // >>> NOVO: gravar base URL (ex.: https://painel.tolon.com.br/ ou http://192.168.0.10/)
    suspend fun saveBaseUrl(ctx: Context, url: String) {
        ctx.dataStore.edit { it[KEY_BASE_URL] = url.trim() }
    }

    /* ====== observação ====== */
    fun observeRole(ctx: Context): Flow<DeviceRole> =
        ctx.dataStore.data
            .map { p ->
                val name = p[KEY_ROLE] ?: DeviceRole.MESA.name
                runCatching { DeviceRole.valueOf(name) }.getOrDefault(DeviceRole.MESA)
            }.distinctUntilChanged()

    fun observeTable(ctx: Context): Flow<Int> =
        ctx.dataStore.data.map { it[KEY_TABLE] ?: 1 }.distinctUntilChanged()

    fun observeEmpresa(ctx: Context): Flow<String?> =
        ctx.dataStore.data.map { it[KEY_EMPRESA] }.distinctUntilChanged()

    fun observeApiToken(ctx: Context): Flow<String?> =
        ctx.dataStore.data.map { it[KEY_API_TOKEN] }.distinctUntilChanged()

    fun observeUsuario(ctx: Context): Flow<String?> =
        ctx.dataStore.data.map { it[KEY_USUARIO] }.distinctUntilChanged()

    fun observeDeviceSerial(ctx: Context): Flow<String?> =
        ctx.dataStore.data.map { it[KEY_DEVICE_SERIAL] }.distinctUntilChanged()

    // >>> NOVO: observar base URL atual
    fun observeBaseUrl(ctx: Context): Flow<String?> =
        ctx.dataStore.data.map { it[KEY_BASE_URL] }.distinctUntilChanged()

    /* ====== leituras uma vez ====== */
    suspend fun getRoleOnce(ctx: Context): DeviceRole {
        val name = ctx.dataStore.data.first()[KEY_ROLE] ?: DeviceRole.MESA.name
        return runCatching { DeviceRole.valueOf(name) }.getOrDefault(DeviceRole.MESA)
    }

    suspend fun getTableOnce(ctx: Context): Int =
        ctx.dataStore.data.first()[KEY_TABLE] ?: 1

    suspend fun getEmpresaOnce(ctx: Context): String =
        ctx.dataStore.data.first()[KEY_EMPRESA] ?: BuildConfig.DEFAULT_EMPRESA

    suspend fun getApiTokenOnce(ctx: Context): String? =
        ctx.dataStore.data.first()[KEY_API_TOKEN]

    suspend fun getUsuarioOnce(ctx: Context): String? =
        ctx.dataStore.data.first()[KEY_USUARIO]

    suspend fun getDeviceSerialOnce(ctx: Context): String? =
        ctx.dataStore.data.first()[KEY_DEVICE_SERIAL]
    // >>> NOVO: ler base URL (fallback: BuildConfig.API_BASE_URL)
    suspend fun getBaseUrlOnce(ctx: Context): String =
        ctx.dataStore.data.first()[KEY_BASE_URL] ?: BuildConfig.API_BASE_URL

    suspend fun isConfigured(ctx: Context): Boolean {
        val p = ctx.dataStore.data.first()
        val role = p[KEY_ROLE]
        return role == DeviceRole.BALCAO.name ||
                (role == DeviceRole.MESA.name && p[KEY_TABLE] != null)
    }

    suspend fun isAuthenticated(ctx: Context): Boolean {
        val p = ctx.dataStore.data.first()
        val emp = p[KEY_EMPRESA]
        val tok = p[KEY_API_TOKEN]
        return !emp.isNullOrBlank() && !tok.isNullOrBlank()
    }

    suspend fun clear(ctx: Context) { ctx.dataStore.edit { it.clear() } }

    suspend fun clearAuth(ctx: Context) {
        ctx.dataStore.edit {
            it.remove(KEY_EMPRESA)
            it.remove(KEY_API_TOKEN)
            it.remove(KEY_USUARIO)
            it.remove(KEY_DEVICE_SERIAL)
        }
    }
}
