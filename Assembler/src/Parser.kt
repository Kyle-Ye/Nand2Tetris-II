import Type.*
import java.io.*

enum class Type {
    A_COMMAND, C_COMMAND, L_COMMAND
}

class Parser(private val table: SymbolTable) {
    private var command: String = ""
    private var variableAddress = 16
    fun preProcess(input: File) {
        var linePosition = 0
        input.forEachLine { line ->
            command = line.substringBefore("//").trim()
            if (command.isNotEmpty()) {
                if (commandType == L_COMMAND) {
                    table.addEntry(symbol, linePosition)
                } else {
                    linePosition++
                }
            }
        }
    }

    fun process(input: File, output: File) {
        input.forEachLine { line ->
            command = line.substringBefore("//").trim()
            if (command.isNotEmpty()) {
                val outputLine = when (commandType) {
                    A_COMMAND -> {
                        var number = try {
                            Integer.parseInt(symbol)
                        } catch (e: NumberFormatException) {

                        }
                        if (number !is Int) {
                            number = if (symbol in table) {
                                table.getAddress(symbol)
                            } else {
                                table.addEntry(symbol, variableAddress++)
                                table.getAddress(symbol)
                            }
                        }
                        val binary = Integer.toBinaryString(number)
                        "0".repeat(16 - binary.length) + binary
                    }
                    L_COMMAND -> return@forEachLine
                    C_COMMAND -> {
                        "111" + comp(comp) + dest(dest) + jump(jump)
                    }
                }
                output.appendText(outputLine + "\n")
            }

        }
    }

    private val commandType: Type
        get() {
            return when (command[0]) {
                '@' -> A_COMMAND
                '(' -> L_COMMAND
                else -> C_COMMAND
            }
        }

    private val symbol: String
        get() {
            return when (commandType) {
                A_COMMAND -> command.substring(1)
                L_COMMAND -> command.substring(1, command.length - 1)
                else -> ""
            }
        }

    private val dest: String
        get() {
            return command.substringBefore('=', "")
        }


    private val comp: String
        get() {
            return when {
                '=' in command && ';' in command -> command.substringAfter('=').substringBefore(';')
                '=' in command -> command.substringAfter('=')
                ';' in command -> command.substringBefore(';')
                else -> command
            }
        }

    private val jump: String
        get() {
            return command.substringAfter(';', "")
        }

}