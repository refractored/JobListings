package net.refractored.joblistings.database

import com.j256.ormlite.dao.Dao
import com.j256.ormlite.dao.DaoManager
import com.j256.ormlite.field.DataPersisterManager
import com.j256.ormlite.jdbc.JdbcConnectionSource
import com.j256.ormlite.jdbc.JdbcPooledConnectionSource
import com.j256.ormlite.logger.LoggerFactory
import com.j256.ormlite.logger.NullLogBackend.NullLogBackendFactory
import com.j256.ormlite.table.TableUtils
import net.refractored.joblistings.JobListings
import net.refractored.joblistings.mail.Mail
import net.refractored.joblistings.order.Order
import net.refractored.joblistings.order.tables.ClaimedOrder
import net.refractored.joblistings.order.tables.CompletedOrder
import net.refractored.joblistings.order.tables.FailedOrder
import net.refractored.joblistings.order.tables.PendingOrder
import net.refractored.joblistings.serializers.ComponentSerializers
import net.refractored.joblistings.serializers.ItemstackSerializers
import net.refractored.joblistings.serializers.LocalDateTimeSerializers
import java.util.*

/**
 * A static class used for database operations.
 */
object Database {
    /**
     * The connection source for the database.
     */
    @JvmStatic
    var connectionSource: JdbcConnectionSource
        private set

    /**
     * The order DAO, used for database operations on orders.
     */
    @JvmStatic
    var orderDao: Dao<Order, UUID>
        private set

    /**
     * The pending order DAO, used for database operations on orders.
     */
    @JvmStatic
    var pendingOrderDao: Dao<PendingOrder, UUID>
        private set

    /**
     * The claimed order DAO, used for database operations on orders.
     */
    @JvmStatic
    var claimedOrderDao: Dao<ClaimedOrder, UUID>
        private set

    /**
     * The claimed order DAO, used for database operations on orders.
     */
    @JvmStatic
    var failedOrderDao: Dao<FailedOrder, UUID>
        private set

    /**
     * The claimed order DAO, used for database operations on orders.
     */
    @JvmStatic
    var completedOrderDao: Dao<CompletedOrder, UUID>
        private set

    /**
     * The order DAO, used for database operations on orders.
     */
    @JvmStatic
    var mailDao: Dao<Mail, UUID>
        private set

    init {
        JobListings.instance.logger.info("Initializing database...")
        LoggerFactory.setLogBackendFactory(NullLogBackendFactory())

        if (JobListings.instance.config.getString("database.url") == "jdbc:mysql://DATABASE_IP:PORT/DATABASE_NAME" &&
            !JobListings.instance.config.getBoolean("database.sqlite")
        ) {
            JobListings.instance.logger.severe("Database not setup in config.")
            throw Exception("Database not setup in config.")
        }

        connectionSource =
            if (JobListings.instance.config.getBoolean("database.sqlite")) {
                JdbcPooledConnectionSource(
                    "jdbc:sqlite:" + JobListings.instance.dataFolder.toPath() + "/database.db",
                )
            } else {
                JdbcPooledConnectionSource(
                    JobListings.instance.config.getString("database.url"),
                    JobListings.instance.config.getString("database.user"),
                    JobListings.instance.config.getString("database.password"),
                )
            }

        @Suppress("UNCHECKED_CAST")
        orderDao = DaoManager.createDao(connectionSource, Order::class.java) as Dao<Order, UUID>

        TableUtils.createTableIfNotExists(connectionSource, Order::class.java)

        @Suppress("UNCHECKED_CAST")
        pendingOrderDao = DaoManager.createDao(connectionSource, PendingOrder::class.java) as Dao<PendingOrder, UUID>

        TableUtils.createTableIfNotExists(connectionSource, PendingOrder::class.java)

        @Suppress("UNCHECKED_CAST")
        claimedOrderDao = DaoManager.createDao(connectionSource, ClaimedOrder::class.java) as Dao<ClaimedOrder, UUID>

        TableUtils.createTableIfNotExists(connectionSource, ClaimedOrder::class.java)

        @Suppress("UNCHECKED_CAST")
        completedOrderDao = DaoManager.createDao(connectionSource, CompletedOrder::class.java) as Dao<CompletedOrder, UUID>

        TableUtils.createTableIfNotExists(connectionSource, CompletedOrder::class.java)

        @Suppress("UNCHECKED_CAST")
        failedOrderDao = DaoManager.createDao(connectionSource, FailedOrder::class.java) as Dao<FailedOrder, UUID>

        TableUtils.createTableIfNotExists(connectionSource, FailedOrder::class.java)

        @Suppress("UNCHECKED_CAST")
        mailDao = DaoManager.createDao(connectionSource, Mail::class.java) as Dao<Mail, UUID>

        TableUtils.createTableIfNotExists(connectionSource, Mail::class.java)

        DataPersisterManager.registerDataPersisters(ItemstackSerializers.getSingleton())

        DataPersisterManager.registerDataPersisters(ComponentSerializers.getSingleton())

        DataPersisterManager.registerDataPersisters(LocalDateTimeSerializers.getSingleton())

        System.setProperty("com.j256.ormlite.logger.type", "LOCAL")
        System.setProperty("com.j256.ormlite.logger.level", "ERROR")
        System.setProperty(LoggerFactory.LOG_TYPE_SYSTEM_PROPERTY, "LOCAL")

        JobListings.instance.logger.info("Database initialized")
    }
}
