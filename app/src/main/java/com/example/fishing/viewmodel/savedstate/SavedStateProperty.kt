package com.example.fishing.viewmodel.savedstate

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.SavedStateHandle
import org.osmdroid.util.GeoPoint
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

/**
 * Property delegate that keeps a form value in [SavedStateHandle] so it survives
 * process death, while exposing it through [mutableStateOf] so Compose still
 * observes assignments.
 *
 * The value is restored once when the ViewModel is constructed. Later reads are
 * plain snapshot reads; every write updates both the snapshot state and the
 * handle, which is what makes the delegate safe to use as a normal `var`.
 */
@Suppress("UNCHECKED_CAST")
internal class SavedStateProperty<T>(
    private val handle: SavedStateHandle,
    private val key: String,
    defaultValue: () -> T,
    private val save: (T) -> Any? = { it },
    restore: (Any?) -> T? = { it as? T },
) : ReadWriteProperty<Any?, T> {

    private val state = mutableStateOf(restore(handle.get<Any?>(key)) ?: defaultValue())

    override fun getValue(thisRef: Any?, property: KProperty<*>): T = state.value

    override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        state.value = value
        handle[key] = save(value)
    }
}

/**
 * Two-key variant for [GeoPoint]: the coordinates are persisted as plain
 * doubles, and clearing the value removes both keys instead of writing nulls.
 */
internal class GeoPointSavedStateProperty(
    private val handle: SavedStateHandle,
    private val latitudeKey: String,
    private val longitudeKey: String,
) : ReadWriteProperty<Any?, GeoPoint?> {

    private val state = mutableStateOf(restore())

    override fun getValue(thisRef: Any?, property: KProperty<*>): GeoPoint? = state.value

    override fun setValue(thisRef: Any?, property: KProperty<*>, value: GeoPoint?) {
        state.value = value
        if (value == null) {
            handle.remove<Double>(latitudeKey)
            handle.remove<Double>(longitudeKey)
        } else {
            handle[latitudeKey] = value.latitude
            handle[longitudeKey] = value.longitude
        }
    }

    private fun restore(): GeoPoint? {
        val latitude = handle.get<Double>(latitudeKey) ?: return null
        val longitude = handle.get<Double>(longitudeKey) ?: return null
        return GeoPoint(latitude, longitude)
    }
}

internal fun SavedStateHandle.stringProperty(
    key: String,
    defaultValue: () -> String = { "" },
): SavedStateProperty<String> = SavedStateProperty(this, key, defaultValue)

internal fun SavedStateHandle.booleanProperty(
    key: String,
    defaultValue: () -> Boolean,
): SavedStateProperty<Boolean> = SavedStateProperty(this, key, defaultValue)

internal fun SavedStateHandle.intProperty(
    key: String,
    defaultValue: () -> Int,
): SavedStateProperty<Int> = SavedStateProperty(this, key, defaultValue)

internal fun SavedStateHandle.floatProperty(
    key: String,
    defaultValue: () -> Float,
): SavedStateProperty<Float> = SavedStateProperty(this, key, defaultValue)

internal inline fun <reified T : Enum<T>> SavedStateHandle.enumProperty(
    key: String,
    noinline defaultValue: () -> T,
): SavedStateProperty<T> {
    val values = enumValues<T>()
    return SavedStateProperty(
        handle = this,
        key = key,
        defaultValue = defaultValue,
        save = { it.name },
        restore = { saved ->
            (saved as? String)?.let { name -> values.firstOrNull { it.name == name } }
        },
    )
}

internal fun <T> SavedStateHandle.listProperty(
    key: String,
    defaultValue: () -> List<T>,
    save: (List<T>) -> ArrayList<String>,
    restore: (Any?) -> List<T>,
): SavedStateProperty<List<T>> = SavedStateProperty(
    handle = this,
    key = key,
    defaultValue = defaultValue,
    save = save,
    restore = restore,
)

internal fun SavedStateHandle.geoPointProperty(
    latitudeKey: String,
    longitudeKey: String,
): GeoPointSavedStateProperty = GeoPointSavedStateProperty(this, latitudeKey, longitudeKey)
