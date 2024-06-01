package net.refractored.joblistings.order

enum class OrderStatus {
    PENDING,
    CLAIMED,
    COMPLETED,
    INCOMPLETE, // Used if order was not completed in time
    EXPIRED, // Used if order was not claimed in time
}