package net.refractored.joblistings.order

import net.kyori.adventure.text.Component
import net.refractored.joblistings.util.MessageUtil

/**
 * Represents the status of an order
 */
enum class OrderStatus {
    /**
     * The order is pending and has not been claimed.
     */
    PENDING,

    /**
     * The order has claimed and is in progress.
     */
    CLAIMED,

    /**
     * The order has been completed.
     */
    COMPLETED,

    /**
     * The order was not either not completed in time, or was cancelled by the assignee
     */
    INCOMPLETE,

    /**
     * The order was cancelled by the owner.
     */
    CANCELLED, ;

    fun getComponent(): Component = MessageUtil.getMessage("OrderStatus.$name")
}
