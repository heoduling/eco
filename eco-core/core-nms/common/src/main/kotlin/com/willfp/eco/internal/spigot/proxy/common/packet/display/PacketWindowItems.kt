package com.willfp.eco.internal.spigot.proxy.common.packet.display

import com.willfp.eco.core.EcoPlugin
import com.willfp.eco.core.display.Display
import com.willfp.eco.core.items.HashedItem
import com.willfp.eco.core.packet.PacketEvent
import com.willfp.eco.core.packet.PacketListener
import com.willfp.eco.internal.spigot.proxy.common.asBukkitStack
import com.willfp.eco.internal.spigot.proxy.common.asNMSStack
import com.willfp.eco.internal.spigot.proxy.common.toNMS
import com.willfp.eco.internal.spigot.proxy.common.packet.display.frame.DisplayFrame
import com.willfp.eco.internal.spigot.proxy.common.packet.display.frame.lastDisplayFrame
import io.netty.util.AttributeKey
import net.minecraft.network.protocol.game.ClientboundContainerSetContentPacket
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

open class PacketWindowItems(
    private val plugin: EcoPlugin
) : PacketListener {
    private val windowIdKey = AttributeKey.valueOf<Int>("eco:last_window_id")

    private val field = ClientboundContainerSetContentPacket::class.java
        .declaredFields
        .first { it.type == List::class.java }
        .apply { isAccessible = true }

    override fun getSendPacketClasses(): Collection<Class<*>> =
        listOf(ClientboundContainerSetContentPacket::class.java)

    override fun onSend(event: PacketEvent) {
        val packet = event.handle as? ClientboundContainerSetContentPacket ?: return
        val player = event.player

        Display.display(packet.carriedItem.asBukkitStack(), player)

        val windowId = packet.containerId

        updateWindowID(player, windowId)

        val itemStacks = packet.items.mapTo(ArrayList(packet.items.size)) { it.asBukkitStack() }

        val newItems = modifyWindowItems(itemStacks, windowId, player)

        field.set(packet, newItems.map { it.asNMSStack() })
    }


    protected fun updateWindowID(player: Player, windowId: Int) {
        // A channel belongs to one connection. No UUID history survives on this
        // long-lived listener, and an old connection cannot overwrite a reconnect.
        val channel = player.toNMS().connection.connection.channel
        val lastKnownID = channel.attr(windowIdKey).getAndSet(windowId)
        if (lastKnownID != windowId || windowId != 0) {
            player.lastDisplayFrame = DisplayFrame.EMPTY
        }
    }

    protected fun modifyWindowItems(
        itemStacks: MutableList<ItemStack>,
        windowId: Int,
        player: Player
    ): MutableList<ItemStack> {
        if (plugin.configYml.getBool("use-display-frame") && windowId == 0) {
            val frameMap = mutableMapOf<Byte, HashedItem>()

            for (index in itemStacks.indices) {
                frameMap[index.toByte()] = HashedItem.of(itemStacks[index])
            }

            val newFrame = DisplayFrame(frameMap)

            val lastFrame = player.lastDisplayFrame

            player.lastDisplayFrame = newFrame

            // Decide all changes before display modules can mutate any item.
            val changes = BooleanArray(itemStacks.size)
            for ((slot, data) in newFrame.items) {
                changes[slot.toInt()] = lastFrame.items[slot]?.hash != data.hash
            }

            for (index in itemStacks.indices) {
                if (changes[index]) {
                    Display.display(itemStacks[index], player)
                }
            }

            for (index in itemStacks.indices) {
                if (!changes[index]) {
                    itemStacks[index] = lastFrame.getItem(index.toByte()) ?: itemStacks[index]
                }
            }
        } else {
            itemStacks.forEach { Display.display(it, player) }
        }

        return itemStacks
    }
}
