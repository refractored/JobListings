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



class ItemstackSerializers private constructor() : BaseDataType(SqlType.LONG_STRING, arrayOf<Class<*>>(ItemStack::class.java)) {


    override fun parseDefaultString(fieldType: FieldType?, defaultStr: String?): Any? {
        return deserialize(defaultStr)
    }

    override fun javaToSqlArg(fieldType: FieldType?, javaObject: Any?): Any? {
        return serialize(javaObject as ItemStack)
    }

    override fun sqlArgToJava(fieldType: FieldType?, sqlArg: Any?, columnPos: Int): ItemStack? {
        return deserialize(sqlArg as String?)
    }
    override fun resultStringToJava(fieldType: FieldType?, stringValue: String?, columnPos: Int): Any? {
        return deserialize(stringValue)
    }

    override fun resultToSqlArg(fieldType: FieldType?, results: DatabaseResults?, columnPos: Int): Any? {
        return results?.getString(columnPos)
    }

    fun serialize(contents: ItemStack): String {
        val baos = ByteArrayOutputStream()
        var boos: BukkitObjectOutputStream? = null

        try {
            boos = BukkitObjectOutputStream(baos)
            boos.writeObject(contents)
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            try {
                boos?.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }

        return Base64.getEncoder().encodeToString(baos.toByteArray())
    }

    fun deserialize(data: String?): ItemStack? {
        var contents: ItemStack? = null

        try {
            val bais = ByteArrayInputStream(Base64.getDecoder().decode(data))
            val bois = BukkitObjectInputStream(bais)
            contents = bois.readObject() as ItemStack

        } catch (e: Exception) {
            e.printStackTrace()
        }
        return contents
    }


    companion object {
        private val singleTon = ItemstackSerializers()

        @JvmStatic
        fun getSingleton(): ItemstackSerializers {
            return singleTon
        }
    }
}