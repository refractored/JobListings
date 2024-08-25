package net.refractored.joblistings.serializers

import com.j256.ormlite.field.FieldType
import com.j256.ormlite.field.SqlType
import com.j256.ormlite.field.types.BaseDataType
import com.j256.ormlite.support.DatabaseResults
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer

class ComponentSerializers private constructor() : BaseDataType(SqlType.BYTE_ARRAY, arrayOf<Class<*>>(Component::class.java)) {
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
        return GsonComponentSerializer.gson().serialize(javaObject as Component).toByteArray(Charsets.UTF_8)
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

        (sqlArg as ByteArray).decodeToString().let {
            return GsonComponentSerializer.gson().deserialize(it)
        }
    }

    companion object {
        private val singleTon = ComponentSerializers()

        @JvmStatic
        fun getSingleton(): ComponentSerializers = singleTon
    }
}
