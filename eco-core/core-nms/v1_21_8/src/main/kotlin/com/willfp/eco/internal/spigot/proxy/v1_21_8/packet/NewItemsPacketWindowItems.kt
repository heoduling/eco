package com.willfp.eco.internal.spigot.proxy.v1_21_8.packet

import com.willfp.eco.core.EcoPlugin
import com.willfp.eco.core.display.Display
import com.willfp.eco.core.packet.PacketEvent
import com.willfp.eco.internal.spigot.proxy.common.asBukkitStack
import com.willfp.eco.internal.spigot.proxy.common.asNMSStack
import com.willfp.eco.internal.spigot.proxy.common.packet.display.PacketWindowItems
import net.minecraft.network.protocol.game.ClientboundContainerSetContentPacket

class NewItemsPacketWindowItems(
    plugin: EcoPlugin
) : PacketWindowItems(plugin) {
    override fun onSend(event: PacketEvent) {
        val packet = event.handle as? ClientboundContainerSetContentPacket ?: return
        val player = event.player

        Display.display(packet.carriedItem.asBukkitStack(), player)

        val windowId = packet.containerId

        updateWindowID(player, windowId)

        val itemStacks = packet.items.mapTo(ArrayList(packet.items.size)) { it.asBukkitStack() }

        val newItems = modifyWindowItems(itemStacks, windowId, player)

        val newPacket = ClientboundContainerSetContentPacket(
            packet.containerId,
            packet.stateId,
            newItems.map { it.asNMSStack() },
            packet.carriedItem,
        )

        event.handle = newPacket
    }
}
