package net.refractored.joblistings.serializers

import com.j256.ormlite.field.FieldType
import com.j256.ormlite.field.SqlType
import com.j256.ormlite.field.types.BaseDataType
import com.j256.ormlite.support.DatabaseResults
import org.bukkit.inventory.ItemStack

class ItemstackSerializers private constructor() : BaseDataType(SqlType.BYTE_ARRAY, arrayOf<Class<*>>(ItemStack::class.java)) {
    override fun parseDefaultString(
        fieldType: FieldType?,
        defaultStr: String?,
    ): Any? = null

    override fun javaToSqlArg(
        fieldType: FieldType?,
        javaObject: Any?,
    ): ByteArray? {
        if (javaObject == null) {
            return null
        }
        return (javaObject as ItemStack).serializeAsBytes()
    }

    override fun resultToSqlArg(
        fieldType: FieldType?,
        results: DatabaseResults?,
        columnPos: Int,
    ): Any = results!!.getBytes(columnPos)

    override fun sqlArgToJava(
        fieldType: FieldType?,
        sqlArg: Any?,
        columnPos: Int,
    ): Any? {
        if (sqlArg == null) {
            return null
        }
        return ItemStack.deserializeBytes(sqlArg as ByteArray)
    }

    companion object {
        private val singleTon = ItemstackSerializers()

        @JvmStatic
        fun getSingleton(): ItemstackSerializers = singleTon
    }
}
