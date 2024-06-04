package net.refractored.joblistings.serializers

import com.j256.ormlite.field.FieldType
import com.j256.ormlite.field.SqlType
import com.j256.ormlite.field.types.BaseDataType
import com.j256.ormlite.support.DatabaseResults
import org.bukkit.inventory.ItemStack
import org.bukkit.util.io.BukkitObjectInputStream
import org.bukkit.util.io.BukkitObjectOutputStream
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.*



class ItemstackSerializers private constructor() : BaseDataType(SqlType.BYTE_ARRAY, arrayOf<Class<*>>(ItemStack::class.java)) {

    override fun parseDefaultString(fieldType: FieldType?, defaultStr: String?): Any? {
        return null
    }

    override fun javaToSqlArg(fieldType: FieldType?, javaObject: Any?): ByteArray? {
        if (javaObject == null) {
            return null
        }
        return try {
            ByteArrayOutputStream().use { byteOut ->
                BukkitObjectOutputStream(byteOut).use { bukkitOut ->
                    bukkitOut.writeObject(javaObject)
                }
                byteOut.toByteArray()
            }
        } catch (e: IOException) {
            throw RuntimeException("Failed to serialize ItemStack", e)
        }
    }

    override fun resultToSqlArg(fieldType: FieldType?, results: DatabaseResults?, columnPos: Int): Any {
        return results!!.getBytes(columnPos)
    }

    override fun sqlArgToJava(fieldType: FieldType?, sqlArg: Any?, columnPos: Int): Any? {
        if (sqlArg == null) {
            return null
        }
        return try {
            ByteArrayInputStream(sqlArg as ByteArray).use { byteIn ->
                BukkitObjectInputStream(byteIn).use { bukkitIn ->
                    bukkitIn.readObject() as ItemStack
                }
            }
        } catch (e: IOException) {
            throw RuntimeException("Failed to deserialize ItemStack", e)
        } catch (e: ClassNotFoundException) {
            throw RuntimeException("Failed to find class during ItemStack deserialization", e)
        }
    }


    companion object {
        private val singleTon = ItemstackSerializers()

        @JvmStatic
        fun getSingleton(): ItemstackSerializers {
            return singleTon
        }
    }
}