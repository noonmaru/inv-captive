@file:Suppress("DEPRECATION")

package com.github.noonmaru.invcaptive.plugin

import com.google.common.collect.ImmutableList
import net.minecraft.server.v1_16_R3.*
import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.Material
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.craftbukkit.v1_16_R3.entity.CraftPlayer
import org.bukkit.craftbukkit.v1_16_R3.inventory.CraftItemStack
import org.bukkit.entity.Player
import kotlin.math.min

object InvCaptive {
    private val items: NonNullList<ItemStack>

    private val armor: NonNullList<ItemStack>

    private val extraSlots: NonNullList<ItemStack>

    private val contents: List<NonNullList<ItemStack>>

    init {
        val inv = PlayerInventory(null)

        this.items = inv.items
        this.armor = inv.armor
        this.extraSlots = inv.extraSlots
        this.contents = ImmutableList.of(items, armor, extraSlots)
    }

    private const val ITEMS = "items"
    private const val ARMOR = "armor"
    private const val EXTRA_SLOTS = "extraSlots"

    fun load(yaml: YamlConfiguration) {
        yaml.loadItemStackList(ITEMS, items)
        yaml.loadItemStackList(ARMOR, armor)
        yaml.loadItemStackList(EXTRA_SLOTS, extraSlots)
    }

    @Suppress("UNCHECKED_CAST")
    private fun ConfigurationSection.loadItemStackList(name: String, list: NonNullList<ItemStack>) {
        val map = getMapList(name)
        val items = map.map { CraftItemStack.asNMSCopy(CraftItemStack.deserialize(it as Map<String, Any>)) }

        for (i in 0 until min(list.count(), items.count())) {
            list[i] = items[i]
        }
    }

    fun save(): YamlConfiguration {
        val yaml = YamlConfiguration()

        yaml.setItemStackList(ITEMS, items)
        yaml.setItemStackList(ARMOR, armor)
        yaml.setItemStackList(EXTRA_SLOTS, extraSlots)

        return yaml
    }

    private fun ConfigurationSection.setItemStackList(name: String, list: NonNullList<ItemStack>) {
        set(name, list.map { CraftItemStack.asCraftMirror(it).serialize() })
    }

    fun patch(player: Player) {
        val entityplayer = (player as CraftPlayer).handle
        val playerInv = entityplayer.inventory

        playerInv.setField("items", items)
        playerInv.setField("armor", armor)
        playerInv.setField("extraSlots", extraSlots)
        playerInv.setField("f", contents)
    }

    private fun Any.setField(name: String, value: Any) {
        val field = javaClass.getDeclaredField(name).apply {
            isAccessible = true
        }

        field.set(this, value)
    }

    fun captive() {
        val item = ItemStack(Blocks.BARRIER)
        items.replaceAll { item.cloneItemStack() }
        armor.replaceAll { item.cloneItemStack() }
        extraSlots.replaceAll { item.cloneItemStack() }
        items[0] = ItemStack.b

        for (player in Bukkit.getOnlinePlayers()) {
            player.updateInventory()
        }
    }

    private val releaseSlotItem = CraftItemStack.asNMSCopy(org.bukkit.inventory.ItemStack(Material.GOLDEN_APPLE).apply {
        itemMeta = itemMeta?.apply {
            setDisplayName("${ChatColor.GOLD}새로운 인벤토리")
        }
    })

    fun release(slot: Int): Boolean {
        return when {
            slot < 36 -> {
                items.replaceBarrier(slot, releaseSlotItem)
            }
            slot < 40 -> {
                armor.replaceBarrier(slot - 36, releaseSlotItem)
            }
            else -> {
                extraSlots.replaceBarrier(slot - 40, releaseSlotItem)
            }
        }
    }

    private fun NonNullList<ItemStack>.replaceBarrier(index: Int, item: ItemStack): Boolean {
        val current = this[index]
        val currentItem = current.item

        if (currentItem is ItemBlock && currentItem.block== Blocks.BARRIER) {
            this[index] = item.cloneItemStack()
            return true
        }
        return false
    }
}