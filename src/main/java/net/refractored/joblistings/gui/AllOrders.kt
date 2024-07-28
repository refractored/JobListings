package net.refractored.joblistings.gui

import com.j256.ormlite.stmt.QueryBuilder
import com.samjakob.spigui.buttons.SGButton
import com.samjakob.spigui.menu.SGMenu
import com.willfp.eco.core.gui.player
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.AMPERSAND_CHAR
import net.refractored.joblistings.JobListings
import net.refractored.joblistings.database.Database.Companion.orderDao
import net.refractored.joblistings.order.Order
import net.refractored.joblistings.order.OrderStatus
import net.refractored.joblistings.util.MessageReplacement
import net.refractored.joblistings.util.MessageUtil
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.ItemStack
import java.time.Duration
import java.time.LocalDateTime
import java.util.*
import kotlin.math.ceil

class AllOrders {
    val gui: SGMenu =
        JobListings.instance.spiGUI.create(
            // Me when no component support :((((
            LegacyComponentSerializer.legacy(AMPERSAND_CHAR).serialize(
                MessageUtil.getMessage(
                    "AllOrders.Title",
                    listOf(
                        // I only did this for consistency in the messages.yml
                        MessageReplacement("{currentPage}"),
                        MessageReplacement("{maxPage}"),
                    ),
                ),
            ),
            JobListings.instance.gui.getInt("AllOrders.Rows", 6),
        )

    init {
        gui.setOnPageChange { inventory ->
            inventory.clearAllButStickiedSlots()
            loadOrders(inventory.currentPage)
        }
        loadNavButtons()
        loadCosmeticItems()
        loadOrders(0)
    }

    private val config = JobListings.instance.gui.getConfigurationSection("AllOrders")!!

    private val rows = config.getInt("Rows", 6)

    private val orderSlots: List<Int> = config.getIntegerList("OrderSlots")

    private val pageCount =
        if (ceil(orderDao.countOf().toDouble() / orderSlots.count()).toInt() > 0) {
            ceil(orderDao.countOf().toDouble() / orderSlots.count()).toInt()
        } else {
            1
        }

    /**
     * Gets the offset for the page number, based on the rows set in config.
     * @param page The page.
     * @return The number of slots required for the page.
     */
    private fun getOffset(page: Int): Int = page * (rows * 9)

    /**
     * Clears all non-stickied slots, and loads the orders for the requested page.
     * @param page The page to load orders for.
     */
    fun loadOrders(page: Int) {
        gui.clearAllButStickiedSlots()
        val orders = Order.getPendingOrders(orderSlots.count(), page * orderSlots.count())
        for (slot in orderSlots) {
            val button: SGButton = orders.getOrNull(slot)?.let { getOrderButton(it) } ?: getFallbackButton()
            gui.setButton(slot + getOffset(page), button)
        }
    }

    private fun getFallbackButton(): SGButton {
        val fallbackConfig = config.getConfigurationSection("FallbackItem")!!
        val item =
            ItemStack.of(
                Material.valueOf(
                    fallbackConfig.getString("Material") ?: "BEDROCK",
                ),
            )
        item.amount = fallbackConfig.getInt("Amount")
        val itemMeta = item.itemMeta
        itemMeta.itemName()
        itemMeta.setCustomModelData(
            fallbackConfig.getInt("ModelData"),
        )
        item.itemMeta = itemMeta
        item.lore(
            fallbackConfig.getStringList("Amount").map { line -> MessageUtil.toComponent(line) },
        )
        return SGButton(item)
    }

    private fun getOrderButton(order: Order): SGButton {
        val item = order.item.clone()
        item.amount =
            if (order.itemAmount <= item.maxStackSize) {
                order.itemAmount
            } else {
                item.maxStackSize
            }
        val itemMetaCopy = item.itemMeta
        val expireDuration = Duration.between(LocalDateTime.now(), order.timeExpires)
        val expireDurationText =
            MessageUtil.getMessage(
                "AllOrders.ExpireDuration",
                listOf(
                    MessageReplacement(expireDuration.toDays().toString()),
                    MessageReplacement(expireDuration.toHoursPart().toString()),
                    MessageReplacement(expireDuration.toMinutesPart().toString()),
                ),
            )
        val createdDuration = Duration.between(order.timeCreated, LocalDateTime.now())
        val createdDurationText =
            MessageUtil.getMessage(
                "AllOrders.CreatedDuration",
                listOf(
                    MessageReplacement(createdDuration.toDays().toString()),
                    MessageReplacement(createdDuration.toHoursPart().toString()),
                    MessageReplacement(createdDuration.toMinutesPart().toString()),
                ),
            )

        val orderItemLore =
            MessageUtil.getMessageList(
                "AllOrders.OrderItemLore",
                listOf(
                    MessageReplacement(order.cost.toString()),
                    MessageReplacement(order.getOwner().name ?: "Unknown"),
                    MessageReplacement(createdDurationText),
                    MessageReplacement(expireDurationText),
                    MessageReplacement(order.itemAmount.toString()),
                ),
            )

        if (itemMetaCopy.hasLore()) {
            val itemLore = itemMetaCopy.lore()!!
            itemLore.addAll(orderItemLore)
            itemMetaCopy.lore(itemLore)
        } else {
            itemMetaCopy.lore(orderItemLore)
        }

        item.itemMeta = itemMetaCopy

        val button =
            SGButton(
                item,
            ).withListener { event: InventoryClickEvent ->
                if (order.user == event.player.uniqueId) {
                    event.whoClicked.closeInventory()
                    event.player.sendMessage(
                        MessageUtil.getMessage("General.CannotAcceptOwnOrder"),
                    )
                    return@withListener
                }
                if (order.status != OrderStatus.PENDING) {
                    event.whoClicked.closeInventory()
                    event.player.sendMessage(
                        MessageUtil.getMessage("General.OrderAlreadyClaimed"),
                    )
                    return@withListener
                }
                if (order.isOrderExpired()) {
                    event.whoClicked.closeInventory()
                    event.player.sendMessage(
                        MessageUtil.getMessage("General.OrderExpired"),
                    )
                    return@withListener
                }
                JobListings.instance.essentials?.let {
                    if (JobListings.instance.config.getBoolean("Essentials.UseIgnoreList")) {
                        val player =
                            it.users.load(
                                event.player.uniqueId,
                            )
                        val owner =
                            it.users.load(
                                Bukkit.getOfflinePlayer(order.user).uniqueId,
                            )
                        if (owner.isIgnoredPlayer(player) || player.isIgnoredPlayer(owner)) {
                            event.whoClicked.closeInventory()
                            event.player.sendMessage(
                                MessageUtil.getMessage("General.Ignored"),
                            )
                            return@withListener
                        }
                    }
                }
                val queryBuilder: QueryBuilder<Order, UUID> = orderDao.queryBuilder()
                queryBuilder
                    .where()
                    .eq("assignee", event.player.uniqueId)
                    .and()
                    .eq("status", OrderStatus.CLAIMED)
                val orders = orderDao.query(queryBuilder.prepare())
                if (orders.count() > JobListings.instance.config.getInt("Orders.MaxOrdersAccepted")) {
                    event.whoClicked.closeInventory()
                    event.player.sendMessage(
                        MessageUtil.getMessage(
                            "AllOrders.OrderItemLore",
                            listOf(
                                MessageReplacement(
                                    JobListings.instance.config
                                        .getInt("Orders.MaxOrdersAccepted")
                                        .toString(),
                                ),
                            ),
                        ),
                    )
                }

                order.acceptOrder(event.player)
                event.whoClicked.closeInventory()
            }
        return button
    }

    private fun loadNavButtons() {
        val navKeys =
            listOf(
                config.getConfigurationSection("NextPage")!!,
                config.getConfigurationSection("PreviousPage")!!,
            )
        navKeys.forEach {
            val item =
                ItemStack.of(
                    Material.valueOf(
                        it.getString("Material") ?: "BEDROCK",
                    ),
                )
            item.amount = it.getInt("Amount")
            val itemMeta = item.itemMeta
            itemMeta.itemName()
            itemMeta.setCustomModelData(
                it.getInt("ModelData"),
            )
            item.itemMeta = itemMeta
            item.lore(
                it.getStringList("Amount").map { line -> MessageUtil.toComponent(line) },
            )
            for (i in 0..<pageCount) {
                val offset = getOffset(i)
                it.getIntegerList("Slots").forEach { slot ->
                    gui.setButton(
                        slot + offset,
                        SGButton(item),
                    )
                    gui.stickSlot(slot + offset)
                }
            }
        }
    }

    /**
     * Loads all of the "cosmetic" items in the Items scetion of the config.
     */
    fun loadCosmeticItems() {
        val section = config.getConfigurationSection("Items")!!
        val keys = section.getKeys(false)
        for (key in keys) {
            val item =
                ItemStack.of(
                    Material.valueOf(
                        section.getString("$key.Material") ?: "BEDROCK",
                    ),
                )
            item.amount = section.getInt("$key.Amount")
            val itemMeta = item.itemMeta
            itemMeta.itemName()
            itemMeta.setCustomModelData(
                section.getInt("$key.ModelData"),
            )
            item.itemMeta = itemMeta
            item.lore(section.getStringList("$key.Amount").map { MessageUtil.toComponent(it) })
            for (i in 0..<pageCount) {
                val offset = getOffset(i)
                section.getIntegerList("$key.Slots").forEach {
                    gui.setButton(
                        it + offset,
                        SGButton(item),
                    )
                    gui.stickSlot(it + offset)
                }
            }
        }
    }

    companion object {
        /**
         * Creates an instance of the AllOrders class, and returns a working gui.
         * @return The gui.
         */
        fun getGUI(): SGMenu {
            val allOrders = AllOrders()
            return allOrders.gui
        }
    }
}
