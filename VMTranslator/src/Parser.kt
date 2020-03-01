import Type.*
import java.io.*

enum class Type {
    C_ARITHMETIC, C_PUSH, C_POP,
    C_LABEL, C_GOTO, C_IF,
    C_FUNCTION, C_RETURN, C_CALL,
    ERROR
}

class Parser(private val hasComment: Boolean = false) {
    private var command: String = ""
    private lateinit var commandList: List<String>

    fun process(input: File, writer: CodeWriter) {
        input.forEachLine { line ->
            command = line.substringBefore("//").trim()
            commandList = command.split(" ")
            if (command.isNotEmpty()) {
                if (hasComment) {
                    writer.write("//${command}\n")
                }
                when (commandType) {
                    C_ARITHMETIC ->
                        writer.writeArithmetic(commandList[0])
                    C_PUSH ->
                        writer.writePushPop(C_PUSH, commandList[1], commandList[2].toInt(),input.nameWithoutExtension)
                    C_POP ->
                        writer.writePushPop(C_POP, commandList[1], commandList[2].toInt(),input.nameWithoutExtension)
                    C_LABEL ->
                        writer.writeLabel(commandList[1])
                    C_GOTO ->
                        writer.writeGoto(commandList[1])
                    C_IF ->
                        writer.writeIf(commandList[1])
                    C_FUNCTION ->
                        writer.writeFunction(commandList[1], commandList[2].toInt())
                    C_CALL ->
                        writer.writeCall(commandList[1], commandList[2].toInt())
                    C_RETURN ->
                        writer.writeReturn()
                    ERROR ->
                        writer.write("//ERROR\n")
                }
            }
        }
    }

    private val commandType: Type
        get() {
            return when (commandList[0]) {
                "pop" -> C_POP
                "push" -> C_PUSH
                "add", "sub", "neg", "eq", "gt", "lt", "and", "or", "not" -> C_ARITHMETIC
                "label" -> C_LABEL
                "goto" -> C_GOTO
                "if-goto" -> C_IF
                "function" -> C_FUNCTION
                "call" -> C_CALL
                "return" -> C_RETURN
                else -> ERROR
            }
        }
}