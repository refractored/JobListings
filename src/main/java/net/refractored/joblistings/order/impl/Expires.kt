package net.refractored.joblistings.order.impl

import java.time.LocalDateTime

/**
 * Interface representing an order that expires.
 */
interface Expires {
    var expireTime: LocalDateTime

    /**
     * @return true if the order has expired, false otherwise.
     */
    fun isOrderExpired(): Boolean = LocalDateTime.now().isAfter(expireTime)
}
