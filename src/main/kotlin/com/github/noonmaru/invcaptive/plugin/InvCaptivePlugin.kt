package com.github.noonmaru.invcaptive.plugin

import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.Material
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Firework
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.entity.ItemSpawnEvent
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.inventory.InventoryAction
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.player.*
import org.bukkit.event.world.WorldSaveEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.java.JavaPlugin
import java.io.File
import java.util.*

/**
 * @author Noonmaru
 */
class InvCaptivePlugin : JavaPlugin(), Listener {

    private lateinit var slotsByType: EnumMap<Material, Int>

    override fun onEnable() {
        saveDefaultConfig()

        val seed = config.getLong("seed")

        server.pluginManager.registerEvents(this, this)
        load()

        val list = Material.values().filter { it.isBlock && !it.isAir }.shuffled(Random(seed))
        val count = 9 * 4 + 5

        val map = EnumMap<Material, Int>(Material::class.java)

        for (i in 0 until count) {
            map[list[i]] = i
        }

        this.slotsByType = map

        for (player in Bukkit.getOnlinePlayers()) {
            InvCaptive.patch(player)
        }
    }

    override fun onDisable() {
        save()
    }

    private fun load() {
        val file = File(dataFolder, "inventory.yml").also { if (!it.exists()) return }
        val yaml = YamlConfiguration.loadConfiguration(file)

        InvCaptive.load(yaml)
    }

    private fun save() {
        val yaml = InvCaptive.save()
        val dataFolder = dataFolder.also { it.mkdirs() }
        val file = File(dataFolder, "inventory.yml")

        yaml.save(file)
    }

    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {
        InvCaptive.patch(event.player)
    }

    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        InvCaptive.save()
    }

    @EventHandler
    fun onWorldSave(event: WorldSaveEvent) {
        save()
    }

    @EventHandler
    fun onInventoryClick(event: InventoryClickEvent) {
        event.currentItem?.let {
            if (it.type == Material.BARRIER) {
                event.isCancelled = true
                return
            }
        }

        if (event.action == InventoryAction.HOTBAR_SWAP) {
            val item = event.whoClicked.inventory.getItem(event.hotbarButton)
            if (item != null && item.type == Material.BARRIER) {
                event.isCancelled = true
            }
        }
    }

    @EventHandler
    fun onDropItem(event: PlayerDropItemEvent) {
        if (event.itemDrop.itemStack.type == Material.BARRIER) {
            event.isCancelled = true
        }
    }

    @EventHandler(ignoreCancelled = true)
    fun onBlockBreak(event: BlockBreakEvent) {
        slotsByType[event.block.type]?.let {
            if (InvCaptive.release(it)) {
                for (player in Bukkit.getOnlinePlayers()) {
                    player.world.spawn(player.location, Firework::class.java)
                }

                Bukkit.broadcastMessage(
                    "${ChatColor.RED}${event.player.name}${ChatColor.RESET}님이 ${ChatColor.GOLD}${
                        event.block.translationKey.removePrefix("block.minecraft.")
                    } ${ChatColor.RESET}블록을 파괴하여 인벤토리 잠금이 한칸 해제되었습니다!"
                )
            }
        }
    }

    @EventHandler
    fun onInteract(event: PlayerInteractEvent) {
        if (event.item?.type == Material.BARRIER) {
            event.isCancelled = true
        }
    }

    @EventHandler
    fun onItemSpawn(event: ItemSpawnEvent) {
        val item = event.entity.itemStack

        if (item.type == Material.BARRIER) {
            event.isCancelled = true
        }
    }

    @EventHandler
    fun onSwap(event: PlayerSwapHandItemsEvent) {
        if (event.offHandItem?.type == Material.BARRIER || event.mainHandItem?.type == Material.BARRIER) {
            event.isCancelled = true
        }
    }

    @EventHandler
    fun onPlayerDeath(event: PlayerDeathEvent) {
        event.keepInventory = true

        val drops = event.drops
        drops.clear()

        val inventory = event.entity.inventory
        for (i in 0 until inventory.count()) {
            inventory.getItem(i)?.let { itemStack ->
                if (itemStack.type != Material.BARRIER) {
                    event.drops += itemStack
                    inventory.setItem(i, null)
                }
            }
        }
    }

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        InvCaptive.captive()

        return true
    }
}