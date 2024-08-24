package net.refractored.joblistings.commands

import com.earth2me.essentials.Enchantments
import com.willfp.eco.core.Eco
import com.willfp.eco.core.items.Items
import com.willfp.eco.core.items.builder.EnchantedBookBuilder
import dev.lone.itemsadder.api.ItemsAdder
import net.refractored.joblistings.JobListings
import net.refractored.joblistings.config.Presets
import net.refractored.joblistings.exceptions.CommandErrorException
import net.refractored.joblistings.util.MessageReplacement
import net.refractored.joblistings.util.MessageUtil
import org.bukkit.Material
import org.bukkit.Registry
import org.bukkit.enchantments.Enchantment
import org.bukkit.inventory.ItemStack
import revxrsal.commands.annotation.AutoComplete
import revxrsal.commands.annotation.Command
import revxrsal.commands.annotation.Description
import revxrsal.commands.bukkit.BukkitCommandActor
import revxrsal.commands.bukkit.annotation.CommandPermission

class ImportItems {
    @CommandPermission("joblistings.admin.import")
    @Description("Imports items from other plugins.")
    @Command("joblistings preset import")
    @AutoComplete("eco|ItemsAdder|enchants")
    fun importItems(
        actor: BukkitCommandActor,
        import: String,
    ) {
        when (import) {
            "enchants" -> {
                actor.reply(
                    MessageUtil.getMessage("ImportItems.ImportingItems"),
                )
                importEnchantmentBooks()
            }
            "eco" -> {
                if (!JobListings.instance.ecoPlugin) {
                    throw CommandErrorException(
                        MessageUtil.getMessage(
                            "ImportItems.PluginNotLoaded",
                            listOf(
                                MessageReplacement(import),
                            ),
                        ),
                    )
                }
                actor.reply(
                    MessageUtil.getMessage("ImportItems.ImportingItems"),
                )
                importEcoItems()
            }
            "ItemsAdder" -> {
                if (!JobListings.instance.itemsAdder) {
                    throw CommandErrorException(
                        MessageUtil.getMessage(
                            "ImportItems.PluginNotLoaded",
                            listOf(
                                MessageReplacement(import),
                            ),
                        ),
                    )
                }
                actor.reply(
                    MessageUtil.getMessage("ImportItems.ImportingItems"),
                )
                importItemsAdderItems()
            }
            else -> throw CommandErrorException(
                MessageUtil.getMessage("ImportItems.UnsupportedPlugin"),
            )
        }

        actor.reply(
            MessageUtil.getMessage("ImportItems.DoneImportingItems"),
        )
    }

    private fun importEnchantmentBooks() {

        if (MessageUtil.getMessageUnformatted("ImportItems.EnchantmentBookPrefix") == "ImportItems.EnchantmentBookPrefix") throw IllegalStateException("Book prefix is not setup!")

        val enchants = Registry.ENCHANTMENT.iterator()

        val enchantedBooks = mutableMapOf<String, ItemStack>()

        for (enchant in enchants){
            for (level in 1..enchant.maxLevel){
                val enchantedBook = ItemStack(Material.ENCHANTED_BOOK)

                val enchantedBookMeta = enchantedBook.itemMeta

                enchantedBookMeta.addEnchant(enchant, level, true)

                enchantedBook.itemMeta = enchantedBookMeta

                enchantedBooks[MessageUtil.getMessageUnformatted("ImportItems.EnchantmentBookPrefix") + "${enchant.key.value()}_$level" ] = enchantedBook
            }

        }

        for ((name, item) in enchantedBooks) {
            JobListings.instance.logger.info("Creating preset for $name")

            try {
                Presets.createPreset(name, item)
            } catch (e: IllegalArgumentException) {
                JobListings.instance.logger.warning("Preset already exists for $name")
                continue
            }
        }
    }

    private fun importEcoItems() {
        val customItems = Items.getCustomItems()

        for (items in customItems) {
            var name = items.key.key
            name = name.removePrefix("set_")

            JobListings.instance.logger.info("Creating preset for $name")

            try {
                Presets.createPreset(name, items.item)
            } catch (e: IllegalArgumentException) {
                JobListings.instance.logger.warning("Preset already exists for $name")
                continue
            }
        }
    }

    private fun importItemsAdderItems() {
        val customItems = ItemsAdder.getAllItems()

        for (items in customItems) {
            val name = items.id

            JobListings.instance.logger.info("Creating preset for $name")

            try {
                Presets.createPreset(name, items.itemStack)
            } catch (e: IllegalArgumentException) {
                JobListings.instance.logger.warning("Preset already exists for $name")
                continue
            }
        }
    }
}
