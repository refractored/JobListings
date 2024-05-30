package net.refractored.joblistings.serializers

import org.bukkit.inventory.ItemStack
import org.bukkit.util.io.BukkitObjectInputStream
import org.bukkit.util.io.BukkitObjectOutputStream
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.*


object ItemstackSerializers {

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
                boos!!.close()
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
}