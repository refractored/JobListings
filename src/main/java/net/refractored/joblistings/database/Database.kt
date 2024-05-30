package net.refractored.joblistings.database

import com.j256.ormlite.dao.Dao
import com.j256.ormlite.dao.DaoManager
import com.j256.ormlite.jdbc.JdbcConnectionSource
import com.j256.ormlite.jdbc.JdbcPooledConnectionSource
import com.j256.ormlite.table.TableUtils
import net.refractored.joblistings.JobListings
import net.refractored.joblistings.order.Order
import java.util.*

/**
 * A static class used for database operations.
 */
class Database {
    companion object {
        /**
         * The connection source for the database.
         */
        @JvmStatic
        lateinit var connectionSource: JdbcConnectionSource
            private set

        /**
         * The user DAO, used for database operations on users.
         */
        @JvmStatic
        lateinit var orderDao: Dao<Order, UUID>
            private set

        /**
         * Initializes the database with values from the config.
         * This should be called once.
         * Call before any other database operations, and after the config has been loaded.
         */
        @JvmStatic
        fun init() {
            connectionSource = JdbcPooledConnectionSource(
                JobListings.instance.config.getString("database.url"),
                JobListings.instance.config.getString("database.user"),
                JobListings.instance.config.getString("database.password")
            )

            orderDao = DaoManager.createDao(connectionSource, Order::class.java) as Dao<Order, UUID>

            TableUtils.createTableIfNotExists(connectionSource, Order::class.java)
        }
    }
}