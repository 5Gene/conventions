package com.osp.app

import android.util.Log
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.PrintStream

object Hello {
    @JvmStatic
    fun println(string: Any) {
        Log.e("e", "println 被替换啦 $string")
    }

    @JvmStatic
    fun println(stream: PrintStream, string: Any) {
        Log.e("e", "println 被替换啦 $string")
    }

    @JvmStatic
    fun method2(owner: EmptyAllMethod?, string: String) {
        Log.w("w", owner?.toString() ?: "null")
        owner?.method2("被我改了")
        System.out.println("RemoveAllMethod $owner $string -> Hello")
        Log.e("e", "RemoveAllMethod $string -> Hello")
    }

    @JvmStatic
    fun method2(string: String) {
        Log.e("e", "RemoveAllMethod $string -> Hello")
        System.out.println("RemoveAllMethod $string -> Hello")
    }
}

@Entity
class OWEvent(
    @PrimaryKey val time: Int,
    @ColumnInfo(name = "type") val type: Int,
    @ColumnInfo(name = "feature") val feature: String,
    @ColumnInfo(name = "data") val byteArray: ByteArray
)
