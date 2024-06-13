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
import net.refractored.joblistings.serializers.ComponentSerializers
import net.refractored.joblistings.serializers.ItemstackSerializers
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
         * The order DAO, used for database operations on orders.
         */
        @JvmStatic
        lateinit var orderDao: Dao<Order, UUID>
            private set

        /**
         * The order DAO, used for database operations on orders.
         */
        @JvmStatic
        lateinit var mailDao: Dao<Mail, UUID>
            private set

        /**
         * Initializes the database with values from the config.
         * This should be called once.
         * Call before any other database operations, and after the config has been loaded.
         */
        @JvmStatic
        fun init() {
            JobListings.instance.logger.info("Initializing database...")
            LoggerFactory.setLogBackendFactory(NullLogBackendFactory())

            if (JobListings.instance.config.getString("Database.url") == "jdbc:mysql://DATABASE_IP:PORT/DATABASE_NAME") {
                JobListings.instance.logger.severe("Database not setup in config. Disabling plugin.")
                JobListings.instance.server.pluginManager.disablePlugin(JobListings.instance)
                return
            }

            connectionSource = if (JobListings.instance.config.getString("Database.url").equals("file", true)){
                JdbcConnectionSource(
                    "jdbc:sqlite:" + JobListings.instance.dataFolder.toPath() + "/database.db"
                )
            } else {
                JdbcPooledConnectionSource(
                    JobListings.instance.config.getString("Database.url"),
                    JobListings.instance.config.getString("Database.user"),
                    JobListings.instance.config.getString("Database.password")
                )
            }

            orderDao = DaoManager.createDao(connectionSource, Order::class.java) as Dao<Order, UUID>

            TableUtils.createTableIfNotExists(connectionSource, Order::class.java)

            mailDao = DaoManager.createDao(connectionSource, Mail::class.java) as Dao<Mail, UUID>

            TableUtils.createTableIfNotExists(connectionSource, Mail::class.java)

            DataPersisterManager.registerDataPersisters(ItemstackSerializers.getSingleton())

            DataPersisterManager.registerDataPersisters(ComponentSerializers.getSingleton())

            System.setProperty("com.j256.ormlite.logger.type", "LOCAL")
            System.setProperty("com.j256.ormlite.logger.level", "ERROR")
            System.setProperty(LoggerFactory.LOG_TYPE_SYSTEM_PROPERTY, "LOCAL")
            JobListings.instance.logger.info("Database initialized")
        }
    }
}