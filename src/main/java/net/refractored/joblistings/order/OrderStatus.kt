package net.refractored.joblistings.order

/**
 * Represents the status of an order
 */
enum class OrderStatus {
    /**
     * The order is pending and has not been claimed
     */
    PENDING,
    /**
     * The order has claimed and is in progress
     */
    CLAIMED,
    /**
     * The order has been completed
     */
    COMPLETED,
    /**
     * The order was not completed in time by the assignee
     */
    INCOMPLETE,
    /**
     * The order was cancelled by the user
     */
    CANCELLED,
}