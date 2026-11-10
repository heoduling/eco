package com.willfp.eco.internal.spigot.proxies

import com.willfp.eco.core.data.ExtendedPersistentDataContainer
import org.bukkit.persistence.PersistentDataContainer
import org.bukkit.NamespacedKey

interface ExtendedPersistentDataContainerFactoryProxy {
    fun adapt(pdc: PersistentDataContainer): ExtendedPersistentDataContainer
    fun newPdc(): PersistentDataContainer

    fun keysInNamespace(pdc: PersistentDataContainer, namespace: String): Collection<NamespacedKey> =
        pdc.keys.filter { it.namespace == namespace }
}
