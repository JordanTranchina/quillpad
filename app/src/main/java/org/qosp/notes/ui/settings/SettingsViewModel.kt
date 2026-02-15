package org.qosp.notes.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import me.msoul.datastore.EnumPreference
import org.qosp.notes.preferences.PreferenceRepository
import org.qosp.notes.preferences.SyncMode

class SettingsViewModel(
    private val preferenceRepository: PreferenceRepository,
) : ViewModel() {

    val appPreferences = preferenceRepository.getAll()

    fun <T> setPreference(pref: T) where T : Enum<T>, T : EnumPreference {
        viewModelScope.launch(Dispatchers.IO) {
            preferenceRepository.set(pref)
        }
    }

    suspend fun <T> setPreferenceSuspending(pref: T) where T : Enum<T>, T : EnumPreference {
        preferenceRepository.set(pref)
    }

    fun getEncryptedString(key: String): Flow<String> {
        return preferenceRepository.getEncryptedString(key)
    }

    fun setEncryptedString(key: String, value: String) =
        viewModelScope.launch { preferenceRepository.putEncryptedStrings(key to value) }
}
