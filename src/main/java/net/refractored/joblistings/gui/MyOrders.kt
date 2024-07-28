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
import revxrsal.commands.bukkit.player
import java.time.Duration
import java.time.LocalDateTime
import kotlin.math.ceil

class MyOrders {
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
            loadOrders(inventory.currentPage)
        }
        loadNavButtons()
        loadCosmeticItems()
        loadOrders(0)
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
    private fun loadOrders(page: Int) {
        gui.clearAllButStickiedSlots()
        val orders = Order.getPendingOrders(orderSlots.count(), page * orderSlots.count())
        for ((index, slot) in orderSlots.withIndex()) {
            val button: SGButton = orders.getOrNull(index)?.let { getOrderButton(it) } ?: getFallbackButton()
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
                            MessageReplacement(order.status.toString()),
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
                            MessageReplacement(order.status.toString()),
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
                            MessageReplacement(order.status.toString()),
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
                gui.removeButton(event.slot)
                JobListings.instance.eco.depositPlayer(Bukkit.getOfflinePlayer(event.whoClicked.uniqueId), order.cost)
                orderDao.delete(order)
                gui.refreshInventory(event.whoClicked)
            }
            OrderStatus.CLAIMED -> {
                event.whoClicked.sendMessage(
                    MessageUtil.getMessage("MyOrders.OrderCancelled"),
                )
                gui.removeButton(event.slot)
                JobListings.instance.eco.depositPlayer(Bukkit.getOfflinePlayer(event.whoClicked.uniqueId), (order.cost / 2))
                order.incompleteOrder()
                val assigneeMessage =
                    MessageUtil.getMessage(
                        "MyOrders.AssigneeMessage",
                        listOf(
                            MessageReplacement(order.getItemInfo()),
                        ),
                    )
                order.messageAssignee(assigneeMessage)
                loadOrders(gui.currentPage)
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
                giveOrderItems(order, (event.whoClicked as Player))
                if (order.itemCompleted == order.itemsObtained) {
                    event.whoClicked.closeInventory()
                    event.whoClicked.sendMessage(
                        MessageUtil.getMessage("MyOrders.OrderAlreadyClaimed"),
                    )
                    gui.removeButton(event.slot + getOffset(gui.currentPage))
                    loadOrders(gui.currentPage)
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
                ItemStack.of(
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
                itemMeta.itemName(
                    MessageUtil.toComponent(it.getString("Name") ?: "null").decorationIfAbsent(
                        TextDecoration.ITALIC,
                        TextDecoration.State.FALSE,
                    ),
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
            itemMeta.itemName(
                MessageUtil
                    .toComponent(
                        section.getString("Name") ?: "null",
                    ).decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE),
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
    ) {
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
        }
    }

    companion object {
        /**
         * Creates an instance of the AllOrders class, and returns a working gui.
         * @return The gui.
         */
        fun getGUI(): SGMenu {
            val myOrders = MyOrders()
            return myOrders.gui
        }
    }
}

//    companion object {
//        fun openMyOrders(actor: BukkitCommandActor) {
//            val gui =
//                JobListings.instance.spiGUI.create(
//                    LegacyComponentSerializer.legacy(AMPERSAND_CHAR).serialize(
//                        MessageUtil.getMessage(
//                            "MyOrders.Title",
//                            listOf(
//                                // I only did this for consistency in the messages.yml
//                                MessageReplacement("{currentPage}"),
//                                MessageReplacement("{maxPage}"),
//                            ),
//                        ),
//                    ),
//                    5,
//                )
//
//            val pageCount =
//                if (ceil(orderDao.countOf().toDouble() / 21).toInt() > 0) {
//                    ceil(orderDao.countOf().toDouble() / 21).toInt()
//                } else {
//                    1
//                }
//
//            val borderItems =
//                listOf(
//                    0,
//                    1,
//                    2,
//                    3,
//                    4,
//                    5,
//                    6,
//                    7,
//                    8,
//                    9,
//                    17,
//                    18,
//                    26,
//                    27,
//                    35,
//                    37,
//                    38,
//                    39,
//                    40,
//                    41,
//                    42,
//                    43,
//                )
//
//            for (i in 0..<pageCount) {
//                val pageSlot = 45 * i
//                borderItems.forEach {
//                    gui.setButton(
//                        (it + pageSlot),
//                        SGButton(
//                            ItemBuilder(
//                                Material.valueOf(
//                                    MessageUtil.getMessageUnformatted("ClaimedOrders.BorderItem"),
//                                ),
//                            ).name(" ")
//                                .build(),
//                        ),
//                    )
//                    gui.stickSlot(it + pageSlot)
//                }
//            }
//
//            gui.setOnPageChange { inventory ->
// //                if (gui.getButton(44 + (inventory.currentPage * 45)) == null) {
// //                    loadItems(inventory, actor)
// //                }
//                reloadItems(gui, actor)
//                gui.refreshInventory(actor.player)
//            }
//
//            actor.player.openInventory(gui.inventory)
//            loadItems(gui, actor)
//        }
//
//        private fun reloadItems(
//            gui: SGMenu,
//            actor: BukkitCommandActor,
//        ) {
//            gui.clearAllButStickiedSlots()
//            loadItems(gui, actor)
//        }
//
//        private fun loadItems(
//            gui: SGMenu,
//            actor: BukkitCommandActor,
//        ) {
//            gui.setButton(
//                ((gui.currentPage * 45) + 44),
//                SGButton(
//                    ItemBuilder(Material.ARROW)
//                        .name(
//                            LegacyComponentSerializer.legacy(AMPERSAND_CHAR).serialize(
//                                MessageUtil.getMessage("MyOrders.NextPage"),
//                            ),
//                        ).build(),
//                ).withListener { event: InventoryClickEvent ->
//                    gui.nextPage(actor.player)
//                },
//            )
//            gui.setButton(
//                ((gui.currentPage * 45) + 36),
//                SGButton(
//                    ItemBuilder(Material.ARROW)
//                        .name(
//                            LegacyComponentSerializer.legacy(AMPERSAND_CHAR).serialize(
//                                MessageUtil.getMessage("MyOrders.PreviousPage"),
//                            ),
//                        ).build(),
//                ).withListener { event: InventoryClickEvent ->
//                    gui.previousPage(actor.player)
//                },
//            )
//
//            Order.getPlayerCreatedOrders(21, gui.currentPage * 21, actor.uniqueId).forEachIndexed { index, order ->
//                val item = order.item.clone()
//                item.amount =
//                    if (order.itemAmount <= item.maxStackSize) {
//                        order.itemAmount
//                    } else {
//                        item.maxStackSize
//                    }
//                val itemMetaCopy = item.itemMeta
//                val createdDuration = Duration.between(order.timeCreated, LocalDateTime.now())
//                val createdDurationText =
//                    MessageUtil.getMessage(
//                        "General.DatePastTense",
//                        listOf(
//                            MessageReplacement(createdDuration.toDays().toString()),
//                            MessageReplacement(createdDuration.toHoursPart().toString()),
//                            MessageReplacement(createdDuration.toMinutesPart().toString()),
//                        ),
//                    )
//                val infoLore: MutableList<Component> = mutableListOf()
//
//                when (order.status) {
//                    OrderStatus.PENDING -> {
//                        val expireDuration = Duration.between(LocalDateTime.now(), order.timeExpires)
//                        val expireDurationText =
//                            MessageUtil.getMessage(
//                                "General.DateFormat",
//                                listOf(
//                                    MessageReplacement(expireDuration.toDays().toString()),
//                                    MessageReplacement(expireDuration.toHoursPart().toString()),
//                                    MessageReplacement(expireDuration.toMinutesPart().toString()),
//                                ),
//                            )
//                        infoLore.addAll(
//                            MessageUtil.getMessageList(
//                                "MyOrders.OrderItemLore.Pending",
//                                listOf(
//                                    MessageReplacement(order.cost.toString()),
//                                    MessageReplacement(createdDurationText),
//                                    MessageReplacement(order.status.toString()),
//                                    MessageReplacement(order.itemAmount.toString()),
//                                    MessageReplacement(expireDurationText),
//                                ),
//                            ),
//                        )
//                    }
//                    OrderStatus.CLAIMED -> {
//                        val deadlineDuration = Duration.between(LocalDateTime.now(), order.timeDeadline)
//                        val deadlineDurationText =
//                            MessageUtil.getMessage(
//                                "General.DateFormat",
//                                listOf(
//                                    MessageReplacement(deadlineDuration.toDays().toString()),
//                                    MessageReplacement(deadlineDuration.toHoursPart().toString()),
//                                    MessageReplacement(deadlineDuration.toMinutesPart().toString()),
//                                ),
//                            )
//                        infoLore.addAll(
//                            MessageUtil.getMessageList(
//                                "MyOrders.OrderItemLore.Claimed",
//                                listOf(
//                                    MessageReplacement(order.cost.toString()),
//                                    MessageReplacement(createdDurationText),
//                                    MessageReplacement(order.status.toString()),
//                                    MessageReplacement(order.itemAmount.toString()),
//                                    MessageReplacement(deadlineDurationText),
//                                    MessageReplacement(order.assignee?.let { Bukkit.getOfflinePlayer(it).name } ?: "Unknown"),
//                                ),
//                            ),
//                        )
//                    }
//                    OrderStatus.COMPLETED -> {
//                        val completedDuration = Duration.between(order.timeCompleted, LocalDateTime.now())
//                        val completedDurationText =
//                            MessageUtil.getMessage(
//                                "General.DatePastTense",
//                                listOf(
//                                    MessageReplacement(completedDuration.toDays().toString()),
//                                    MessageReplacement(completedDuration.toHoursPart().toString()),
//                                    MessageReplacement(completedDuration.toMinutesPart().toString()),
//                                ),
//                            )
//                        infoLore.addAll(
//                            MessageUtil.getMessageList(
//                                "MyOrders.OrderItemLore.Completed",
//                                listOf(
//                                    MessageReplacement(order.cost.toString()),
//                                    MessageReplacement(createdDurationText),
//                                    MessageReplacement(order.status.toString()),
//                                    MessageReplacement(order.itemAmount.toString()),
//                                    MessageReplacement(completedDurationText),
//                                    MessageReplacement(order.assignee?.let { Bukkit.getOfflinePlayer(it).name } ?: "Unknown"),
//                                ),
//                            ),
//                        )
//                    }
//                    else -> {
//                        infoLore.add(MessageUtil.toComponent("<reset><red>Status: <white>${order.status}"))
//                        infoLore.add(MessageUtil.toComponent("<reset>This should not be seen!"))
//                    }
//                }
//
//                if (itemMetaCopy.hasLore()) {
//                    val itemLore = itemMetaCopy.lore()!!
//                    itemLore.addAll(infoLore)
//                    itemMetaCopy.lore(itemLore)
//                } else {
//                    itemMetaCopy.lore(infoLore)
//                }
//
//                item.itemMeta = itemMetaCopy
//
//                val button =
//                    SGButton(
//                        item,
//                    ).withListener { event: InventoryClickEvent ->
//                        when (order.status) {
//                            OrderStatus.PENDING -> {
//                                event.whoClicked.sendMessage(
//                                    MessageUtil.getMessage("MyOrders.OrderCancelled"),
//                                )
//                                gui.removeButton((index + 10) + (gui.currentPage * 45))
//                                JobListings.instance.eco.depositPlayer(actor.player, order.cost)
//                                orderDao.delete(order)
//                                reloadItems(gui, actor)
//                                gui.refreshInventory(actor.player)
//                            }
//                            OrderStatus.CLAIMED -> {
//                                event.whoClicked.sendMessage(
//                                    MessageUtil.getMessage("MyOrders.OrderCancelled"),
//                                )
//                                gui.removeButton((index + 10) + (gui.currentPage * 45))
//                                JobListings.instance.eco.depositPlayer(actor.player, (order.cost / 2))
//                                order.incompleteOrder()
//                                val assigneeMessage =
//                                    MessageUtil.getMessage(
//                                        "MyOrders.AssigneeMessage",
//                                        listOf(
//                                            MessageReplacement(order.getItemInfo()),
//                                        ),
//                                    )
//                                order.messageAssignee(assigneeMessage)
//                                reloadItems(gui, actor)
//                                gui.refreshInventory(actor.player)
//                            }
//                            OrderStatus.COMPLETED -> {
//                                val inventorySpaces =
//                                    actor.player.inventory.storageContents.count {
//                                        it == null || (it.isSimilar(order.item) && it.amount < it.maxStackSize)
//                                    }
//                                if (inventorySpaces == 0) {
//                                    actor.player.closeInventory()
//                                    event.whoClicked.sendMessage(
//                                        MessageUtil.getMessage("General.InventoryFull"),
//                                    )
//                                    return@withListener
//                                }
//                                giveOrderItems(order, actor)
//                                if (order.itemCompleted == order.itemsObtained) {
//                                    actor.player.closeInventory()
//                                    event.whoClicked.sendMessage(
//                                        MessageUtil.getMessage("MyOrders.OrderAlreadyClaimed"),
//                                    )
//                                    gui.removeButton((index + 10) + (gui.currentPage * 45))
//                                    reloadItems(gui, actor)
//                                    gui.refreshInventory(actor.player)
//                                    return@withListener
//                                }
//                                event.whoClicked.sendMessage(
//                                    MessageUtil.getMessage("MyOrders.OrderClaimed"),
//                                )
//                            }
//
//                            else -> return@withListener
//                        }
//                    }
//                val baseSlot = (index + 10) + (gui.currentPage * 45)
//
//                var slot = baseSlot
//
//                while (gui.getButton(slot) != null) {
//                    if (slot >= 46) {
//                        throw IndexOutOfBoundsException("No more slots available in page ${gui.currentPage}")
//                    }
//                    slot++
//                }
//
//                gui.setButton(slot, button)
//            }
//            gui.refreshInventory(actor.player)
//        }
//
//        /**
//         * Returns whether the given item matches the order
//         */
//        private fun isMatchingItem(
//            item: ItemStack,
//            order: Order,
//        ): Boolean {
//            JobListings.instance.ecoPlugin.let {
//                if (Items.isCustomItem(item)) {
//                    return Items.getCustomItem(order.item)!!.matches(item)
//                }
//            }
//            return order.item.isSimilar(item)
//        }
//
//        private fun giveOrderItems(
//            order: Order,
//            actor: BukkitCommandActor,
//        ) {
//            var itemsLeft = order.itemCompleted - order.itemsObtained
//            while (itemsLeft > 0) {
//                if (actor.player.inventory.storageContents.count {
//                        it == null || (it.isSimilar(order.item) && it.amount < it.maxStackSize)
//                    } == 0
//                ) {
//                    break
//                }
//                val itemAmount =
//                    if (itemsLeft < order.item.maxStackSize) {
//                        itemsLeft
//                    } else {
//                        order.item.maxStackSize
//                    }
//                itemsLeft -= itemAmount
//                val item =
//                    order.item.clone().apply {
//                        amount = itemAmount
//                    }
//                val unaddeditems = actor.player.inventory.addItem(item)
//                itemsLeft += unaddeditems.values.sumOf { it.amount }
//                if (itemsLeft == 0) break
//            }
//            order.itemsObtained += order.itemCompleted - itemsLeft
//            orderDao.update(order)
//            if (order.itemsObtained == order.itemCompleted) {
//                actor.reply(
//                    MessageUtil.toComponent("<green>Order Obtained!"),
//                )
//                actor.player.closeInventory()
//                orderDao.delete(order)
//            }
//        }
//    }
