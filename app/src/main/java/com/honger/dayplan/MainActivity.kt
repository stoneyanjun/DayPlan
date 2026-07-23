package com.honger.dayplan

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.honger.dayplan.ui.theme.DayPlanTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        testValVar(increaseCount = 110)
        enableEdgeToEdge()
        setContent {
            DayPlanTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Greeting(
                        name = "Android",
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

fun testValVar(increaseCount: Int = 100) {
    var count = 0
    for (i in 0..10) {
        if (count > 100) {
            break
        } else {
            count += increaseCount
        }
    }
    val labelText = countLabel(count = 101) ?: "so far"
    println("count: $count $labelText")
}

fun countLabel(count: Int): String? {
    return when(count) {
        0 -> "zero"
        1 -> "one"
        10 -> "ten"
        else -> null
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    DayPlanTheme {
        Greeting("Android")
    }
}