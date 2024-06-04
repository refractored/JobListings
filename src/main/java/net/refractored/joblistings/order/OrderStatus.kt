package net.refractored.joblistings.order

enum class OrderStatus {
    /**
     * The order is pending and has not been claimed
     */
    PENDING,
    /**
     * The order has claimed & is in progress
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
     * The order was never claimed and has expired
     */
    EXPIRED,
}