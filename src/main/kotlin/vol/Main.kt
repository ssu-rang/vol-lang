package vol

import java.nio.file.Files
import java.nio.file.Path
import kotlin.system.exitProcess

class VolEngine(private val output: (String) -> Unit = ::println) {
    fun run(source: String): Value {
        val tokens = Lexer(source).scanTokens()
        val program = Parser(tokens).parse()
        TypeChecker().check(program)
        return Interpreter(output).execute(program)
    }
}

fun main(arguments: Array<String>) {
    if (arguments.contentEquals(arrayOf("--help")) || arguments.contentEquals(arrayOf("-h"))) {
        println("Usage: vol [file] | vol -e <source>")
        return
    }
    try {
        val source = when {
            arguments.isEmpty() -> System.`in`.bufferedReader().readText()
            arguments.size == 2 && arguments[0] == "-e" -> arguments[1]
            arguments.size == 1 -> Files.readString(Path.of(arguments[0]))
            else -> {
                System.err.println("Usage: vol [file] | vol -e <source>")
                exitProcess(2)
            }
        }
        VolEngine().run(source)
    } catch (error: VolException) {
        System.err.println("VOL error: ${error.message}")
        exitProcess(1)
    } catch (error: Exception) {
        System.err.println("VOL error: ${error.message ?: error.javaClass.simpleName}")
        exitProcess(1)
    }
}
