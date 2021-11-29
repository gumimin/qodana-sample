import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
//import kotlinx.serialization.json.Json // Unused import directive

fun main() = runBlocking {
    launch {
//        Thread.sleep(10000) // Inappropriate blocking method call
        println("second")
    }

    delay(5000)
    println("first")
}
