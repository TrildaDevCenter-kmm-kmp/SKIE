package co.touchlab.swiftgen.plugin.internal.util.reflection.reflectors

import co.touchlab.swiftgen.plugin.internal.util.reflection.Reflector
import org.jetbrains.kotlin.storage.MemoizedFunctionToNullable

internal class MapBasedMemoizedFunctionReflector<K, V : Any>(
    override val instance: MemoizedFunctionToNullable<K, V>,
) : Reflector("org.jetbrains.kotlin.storage.LockBasedStorageManager\$MapBasedMemoizedFunction") {

    val cache by declaredField<MutableMap<K, V>>()
}
