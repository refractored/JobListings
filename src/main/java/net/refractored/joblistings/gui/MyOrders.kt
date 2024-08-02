package net.refractored.joblistings.gui

import com.samjakob.spigui.buttons.SGButton
import com.samjakob.spigui.menu.SGMenu
import com.willfp.eco.core.items.Items
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextDecoration
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
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.ItemStack
import java.time.Duration
import java.time.LocalDateTime
import kotlin.math.ceil

class MyOrders(
    player: Player,
) {
    val gui: SGMenu =
        JobListings.instance.spiGUI.create(
            // Me when no component support :((((
            LegacyComponentSerializer.legacy(AMPERSAND_CHAR).serialize(
                MessageUtil.replaceMessage(
                    config.getString("Title")!!,
                    listOf(
                        // I only did this for consistency in the messages.yml
                        MessageReplacement("{currentPage}"),
                        MessageReplacement("{maxPage}"),
                    ),
                ),
            ),
            config.getInt("Rows", 6),
        )

    init {
        gui.setOnPageChange { inventory ->
            inventory.clearAllButStickiedSlots()
            loadOrders(inventory.currentPage, player)
        }
        loadNavButtons()
        loadCosmeticItems()
        loadOrders(0, player)
    }

    private val config
        get() = JobListings.instance.gui.getConfigurationSection("MyOrders")!!

    private val rows
        get() = config.getInt("Rows", 6)

    private val orderSlots: List<Int>
        get() = config.getIntegerList("OrderSlots")

    private val pageCount
        get() =
            ceil(orderDao.countOf().toDouble() / orderSlots.count()).toInt().let {
                if (it > 0) it else 1
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
    private fun loadOrders(
        page: Int,
        player: Player,
    ) {
        gui.clearAllButStickiedSlots()
        val orders = Order.getPlayerCreatedOrders(orderSlots.count(), page * orderSlots.count(), player.uniqueId)
        for ((index, slot) in orderSlots.withIndex()) {
            val button: SGButton = orders.getOrNull(index)?.let { getOrderButton(it) } ?: getFallbackButton()
            gui.setButton(slot + getOffset(page), button)
        }
    }

    private fun getFallbackButton(): SGButton {
        val fallbackConfig = config.getConfigurationSection("FallbackItem")!!
        val item =
            ItemStack(
                Material.valueOf(
                    fallbackConfig.getString("Material") ?: "BEDROCK",
                ),
            )
        if (item.type == Material.AIR) return SGButton(item)
        item.amount = fallbackConfig.getInt("Amount")
        val itemMeta = item.itemMeta
        itemMeta.setCustomModelData(
            fallbackConfig.getInt("ModelData"),
        )
        itemMeta.displayName(
            MessageUtil
                .toComponent(
                    fallbackConfig.getString("Name") ?: "null",
                ).decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE),
        )
        item.itemMeta = itemMeta
        item.lore(
            fallbackConfig.getStringList("Amount").map { line ->
                MessageUtil.toComponent(line).decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE)
            },
        )
        return SGButton(item)
    }

    private fun getOrderButton(order: Order): SGButton {
        val item = order.item.clone()
        item.amount = minOf(order.itemAmount, item.maxStackSize)
        val itemMetaCopy = item.itemMeta
        val expireDuration = Duration.between(LocalDateTime.now(), order.timeExpires)
        val expireDurationText =
            MessageUtil.getMessage(
                "General.DateFormat",
                listOf(
                    MessageReplacement(expireDuration.toDays().toString()),
                    MessageReplacement(expireDuration.toHoursPart().toString()),
                    MessageReplacement(expireDuration.toMinutesPart().toString()),
                ),
            )
        val createdDuration = Duration.between(order.timeCreated, LocalDateTime.now())
        val createdDurationText =
            MessageUtil.getMessage(
                "General.DatePastTense",
                listOf(
                    MessageReplacement(createdDuration.toDays().toString()),
                    MessageReplacement(createdDuration.toHoursPart().toString()),
                    MessageReplacement(createdDuration.toMinutesPart().toString()),
                ),
            )

        val infoLore: MutableList<Component> = mutableListOf()

        when (order.status) {
            OrderStatus.PENDING -> {
                val expireDuration = Duration.between(LocalDateTime.now(), order.timeExpires)
                val expireDurationText =
                    MessageUtil.getMessage(
                        "General.DateFormat",
                        listOf(
                            MessageReplacement(expireDuration.toDays().toString()),
                            MessageReplacement(expireDuration.toHoursPart().toString()),
                            MessageReplacement(expireDuration.toMinutesPart().toString()),
                        ),
                    )
                infoLore.addAll(
                    MessageUtil.getMessageList(
                        "MyOrders.OrderItemLore.Pending",
                        listOf(
                            MessageReplacement(order.cost.toString()),
                            MessageReplacement(createdDurationText),
                            MessageReplacement(order.status.getComponent()),
                            MessageReplacement(order.itemAmount.toString()),
                            MessageReplacement(expireDurationText),
                        ),
                    ),
                )
            }

            OrderStatus.CLAIMED -> {
                val deadlineDuration = Duration.between(LocalDateTime.now(), order.timeDeadline)
                val deadlineDurationText =
                    MessageUtil.getMessage(
                        "General.DateFormat",
                        listOf(
                            MessageReplacement(deadlineDuration.toDays().toString()),
                            MessageReplacement(deadlineDuration.toHoursPart().toString()),
                            MessageReplacement(deadlineDuration.toMinutesPart().toString()),
                        ),
                    )
                infoLore.addAll(
                    MessageUtil.getMessageList(
                        "MyOrders.OrderItemLore.Claimed",
                        listOf(
                            MessageReplacement(order.cost.toString()),
                            MessageReplacement(createdDurationText),
                            MessageReplacement(order.status.getComponent()),
                            MessageReplacement(order.itemAmount.toString()),
                            MessageReplacement(deadlineDurationText),
                            MessageReplacement(order.assignee?.let { Bukkit.getOfflinePlayer(it).name } ?: "Unknown"),
                        ),
                    ),
                )
            }

            OrderStatus.COMPLETED -> {
                val completedDuration = Duration.between(order.timeCompleted, LocalDateTime.now())
                val completedDurationText =
                    MessageUtil.getMessage(
                        "General.DatePastTense",
                        listOf(
                            MessageReplacement(completedDuration.toDays().toString()),
                            MessageReplacement(completedDuration.toHoursPart().toString()),
                            MessageReplacement(completedDuration.toMinutesPart().toString()),
                        ),
                    )
                infoLore.addAll(
                    MessageUtil.getMessageList(
                        "MyOrders.OrderItemLore.Completed",
                        listOf(
                            MessageReplacement(order.cost.toString()),
                            MessageReplacement(createdDurationText),
                            MessageReplacement(order.status.getComponent()),
                            MessageReplacement(order.itemAmount.toString()),
                            MessageReplacement(completedDurationText),
                            MessageReplacement(order.assignee?.let { Bukkit.getOfflinePlayer(it).name } ?: "Unknown"),
                        ),
                    ),
                )
            }

            else -> {
                infoLore.add(MessageUtil.toComponent("<reset><red>Status: <white>${order.status}"))
                infoLore.add(MessageUtil.toComponent("<reset>This should not be seen!"))
            }
        }

        if (itemMetaCopy.hasLore()) {
            val itemLore = itemMetaCopy.lore()!!
            itemLore.addAll(infoLore)
            itemMetaCopy.lore(itemLore)
        } else {
            itemMetaCopy.lore(infoLore)
        }

        item.itemMeta = itemMetaCopy

        val button = SGButton(item)

        button.setListener { event: InventoryClickEvent ->
            clickOrder(event, order)
        }
        return button
    }

    /**
     * Handles the click event for an order.
     * @param event The click event.
     * @param order The order.
     */
    private fun clickOrder(
        event: InventoryClickEvent,
        order: Order,
    ) {
        when (order.status) {
            OrderStatus.PENDING -> {
                event.whoClicked.sendMessage(
                    MessageUtil.getMessage("MyOrders.OrderCancelled"),
                )
                gui.removeButton(event.slot + getOffset(gui.currentPage))
                order.removeOrder()
                loadOrders(gui.currentPage, event.whoClicked as Player)
                gui.refreshInventory(event.whoClicked)
            }
            OrderStatus.CLAIMED -> {
                event.whoClicked.sendMessage(
                    MessageUtil.getMessage("MyOrders.OrderCancelled"),
                )
                gui.removeButton(event.slot + getOffset(gui.currentPage))
                order.cancelOrder()
                val assigneeMessage =
                    MessageUtil.getMessage(
                        "MyOrders.AssigneeMessage",
                        listOf(
                            MessageReplacement(order.getItemInfo()),
                        ),
                    )
                order.messageAssignee(assigneeMessage)
                loadOrders(gui.currentPage, event.whoClicked as Player)
                gui.refreshInventory(event.whoClicked)
            }
            OrderStatus.COMPLETED -> {
                val inventorySpaces =
                    event.whoClicked.inventory.storageContents.count {
                        it == null || (it.isSimilar(order.item) && it.amount < it.maxStackSize)
                    }
                if (inventorySpaces == 0) {
                    event.whoClicked.closeInventory()
                    event.whoClicked.sendMessage(
                        MessageUtil.getMessage("General.InventoryFull"),
                    )
                    return
                }
                if (giveOrderItems(order, (event.whoClicked as Player))) {
                    event.whoClicked.closeInventory()
                    event.whoClicked.sendMessage(
                        MessageUtil.getMessage("MyOrders.OrderAlreadyClaimed"),
                    )
                    gui.removeButton(event.slot + getOffset(gui.currentPage))
                    loadOrders(gui.currentPage, event.whoClicked as Player)
                    gui.refreshInventory(event.whoClicked)
                    return
                }
                event.whoClicked.sendMessage(
                    MessageUtil.getMessage("MyOrders.OrderClaimed"),
                )
            }

            else -> return
        }
    }

    private fun loadNavButtons() {
        val navKeys =
            listOf(
                config.getConfigurationSection("NextPage")!!,
                config.getConfigurationSection("PreviousPage")!!,
            )
        navKeys.forEach {
            val item =
                ItemStack(
                    Material.valueOf(
                        it.getString("Material") ?: "BEDROCK",
                    ),
                )
            if (item.type != Material.AIR) {
                item.amount = it.getInt("Amount")
                val itemMeta = item.itemMeta
                itemMeta.setCustomModelData(
                    it.getInt("ModelData"),
                )
                itemMeta.displayName(
                    MessageUtil
                        .toComponent(
                            it.getString("Name") ?: "null",
                        ).decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE),
                )
                item.itemMeta = itemMeta
                item.lore(
                    it.getStringList("Amount").map { line ->
                        MessageUtil.toComponent(line).decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE)
                    },
                )
            }
            val button = SGButton(item)
            when (it.name) {
                "NextPage" -> {
                    button.setListener { event -> gui.nextPage(event.whoClicked) }
                }
                "PreviousPage" -> {
                    button.setListener { event -> gui.previousPage(event.whoClicked) }
                }
            }
            for (i in 0..<pageCount) {
                val offset = getOffset(i)
                it.getIntegerList("Slots").forEach { slot ->
                    gui.setButton(
                        slot + offset,
                        button,
                    )
                    gui.stickSlot(slot + offset)
                }
            }
        }
    }

    /**
     * Loads all the "cosmetic" items in the Items section of the config.
     */
    private fun loadCosmeticItems() {
        val section = config.getConfigurationSection("Items")!!
        val keys = section.getKeys(false)
        for (key in keys) {
            val item =
                ItemStack(
                    Material.valueOf(
                        section.getString("$key.Material") ?: "BEDROCK",
                    ),
                )
            item.amount = section.getInt("$key.Amount")
            val itemMeta = item.itemMeta
            itemMeta.setCustomModelData(
                section.getInt("$key.ModelData"),
            )
            itemMeta.displayName(
                MessageUtil
                    .toComponent(
                        section.getString("$key.Name") ?: "null",
                    ).decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE),
            )
            item.itemMeta = itemMeta
            item.lore(
                section.getStringList("$key.Amount").map {
                    MessageUtil.toComponent(it).decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE)
                },
            )
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

    private fun isMatchingItem(
        item: ItemStack,
        order: Order,
    ): Boolean {
        JobListings.instance.ecoPlugin.let {
            if (Items.isCustomItem(item)) {
                return Items.getCustomItem(order.item)!!.matches(item)
            }
        }
        return order.item.isSimilar(item)
    }

    private fun giveOrderItems(
        order: Order,
        player: Player,
    ): Boolean {
        var itemsLeft = order.itemCompleted - order.itemsObtained
        while (itemsLeft > 0) {
            if (player.inventory.storageContents.count {
                    it == null || (it.isSimilar(order.item) && it.amount < it.maxStackSize)
                } == 0
            ) {
                break
            }
            val itemAmount =
                if (itemsLeft < order.item.maxStackSize) {
                    itemsLeft
                } else {
                    order.item.maxStackSize
                }
            itemsLeft -= itemAmount
            val item =
                order.item.clone().apply {
                    amount = itemAmount
                }
            val unaddeditems = player.inventory.addItem(item)
            itemsLeft += unaddeditems.values.sumOf { it.amount }
            if (itemsLeft == 0) break
        }
        order.itemsObtained += order.itemCompleted - itemsLeft
        orderDao.update(order)
        if (order.itemsObtained == order.itemCompleted) {
            player.sendMessage(
                MessageUtil.toComponent("<green>Order Obtained!"),
            )
            player.closeInventory()
            orderDao.delete(order)
            return true
        }
        return false
    }

    companion object {
        /**
         * Creates an instance of the MyOrders class, and returns a working gui.
         * @return The gui.
         */
        fun getGUI(player: Player): SGMenu {
            val myOrders = MyOrders(player)
            return myOrders.gui
        }
    }
}
