@file:Suppress("DEPRECATION")

package com.github.noonmaru.invcaptive.plugin

import com.google.common.collect.ImmutableList
import net.minecraft.core.NonNullList
import net.minecraft.world.entity.player.PlayerInventory
import net.minecraft.world.item.ItemBlock
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.Blocks
import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.Material
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.craftbukkit.v1_18_R2.entity.CraftPlayer
import org.bukkit.craftbukkit.v1_18_R2.inventory.CraftItemStack
import org.bukkit.entity.Player
import kotlin.math.min

object InvCaptive {
    private val items: NonNullList<ItemStack>

    private val armor: NonNullList<ItemStack>

    private val extraSlots: NonNullList<ItemStack>

    private val contents: List<NonNullList<ItemStack>>

    init {
        val inv = PlayerInventory(null)

        this.items = inv.h
        this.armor = inv.i
        this.extraSlots = inv.j
        this.contents = ImmutableList.of(items, armor, extraSlots)
    }

    private const val ITEMS = "h"
    private const val ARMOR = "i"
    private const val EXTRA_SLOTS = "j"

    fun load(yaml: YamlConfiguration) {
        yaml.loadItemStackList(ITEMS, items)
        yaml.loadItemStackList(ARMOR, armor)
        yaml.loadItemStackList(EXTRA_SLOTS, extraSlots)
    }

    @Suppress("UNCHECKED_CAST")
    private fun ConfigurationSection.loadItemStackList(name: String, list: NonNullList<ItemStack>) {
        val map = getMapList(name)  // List<Map<String, Object>>
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

    private fun Any.setField(name: String, value: Any) {
        val field = javaClass.getDeclaredField(name).apply {
            isAccessible = true
        }

        field.set(this, value)
    }

    fun patch(player: Player) {
        val entityplayer = (player as CraftPlayer).handle
        val playerInv = entityplayer.fr()

        playerInv.setField(ITEMS, items)
        playerInv.setField(ARMOR, armor)
        playerInv.setField(EXTRA_SLOTS, extraSlots)
        playerInv.setField("n", contents)

    }

     fun captive() {
        val item = ItemStack(Blocks.gB)
        items.replaceAll { item.n() }
        armor.replaceAll { item.n() }
        extraSlots.replaceAll { item.n() }
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
        val currentItem = current.c()

        if (currentItem is ItemBlock && currentItem.e() == Blocks.gB) {
            this[index] = item.n()
            return true
        }
        return false
    }
}