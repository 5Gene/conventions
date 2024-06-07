package com.osp.app

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.material.Surface
import androidx.compose.material3.Text
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Surface {
                Column {
                    Text(text = "你")
                    Text(text = "好")
                    Text(text = "世")
                    Text(text = "界")

                }
            }
        }
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        testRemove(1)
        testEmpty(10)
        testChange(9)

        val removeAllMethod = RemoveAllMethod
        removeAllMethod.method2("90")

        println(testEmptyList(10))
    }

    fun testRemove(num: Int) {
        println("xxxxxxxxxxxxxx")
        println("xxxxxxxxxxxxxx ${num * 2}")
    }

    fun testChange(num: Int): String {
        println("xxxxxxxxxxxxxx")
        println("xxxxxxxxxxxxxx ${num * 2}")
        testRemove(99)
        return "123"
    }

    fun testEmpty(num: Int): Float {
        println("xxxxxxxxxxxxxx")
        println("xxxxxxxxxxxxxx ${num * 2}")
        return num * 2.0F
    }

    fun testEmptyList(num: Int): List<String>? {
        println("xxxxxxxxxxxxxx")
        println("xxxxxxxxxxxxxx ${num * 2}")
        return null
    }
}