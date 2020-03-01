import Type.*
import java.io.File

class CodeWriter(private val outputFile: File) {
    private var currentFunctionName: String? = null

    companion object {
        private var compareIndex = 0
        private var returnIndex = 0
        private fun valueIncrease(location: String = "SP") = """
            |@${location}
            |M=M+1
            |
        """.trimMargin()

        private fun valueDecrease(location: String = "SP") = """
            |@${location}
            |M=M-1
            |
        """.trimMargin()

        private fun String.toSegment(): String =
            when (this) {
                "local" -> "LCL"
                "argument" -> "ARG"
                "this" -> "THIS"
                "that" -> "THAT"
                else -> ""
            }

        private fun getPointer(location: String, value: String = "M") = """
            |@${location}
            |D=${value}
            |
        """.trimMargin()

        private fun setPointer(location: String, value: String = "D") = """
            |@${location}
            |M=${value}
            |
        """.trimMargin()

        private fun setPointer(location: String, value: Int) = """
            |@${value}
            |D=A
            |@${location}
            |M=D
            |
        """.trimMargin()

        private fun getPointerValue(location: String, value: String = "M") = """
            |@${location}
            |A=M
            |D=${value}
            |
        """.trimMargin()

        private fun setPointerValue(location: String, value: String = "D") = """
            |@${location}
            |A=M
            |M=${value}
            |
        """.trimMargin()

        private fun getPointerMinusConstant(location: String, constant: Int) = """
            |@${location}
            |D=M
            |@${constant}
            |D=D-A
            |
        """.trimMargin()

        // D = *(location - constant)
        private fun getPointerMinusConstantValue(location: String, constant: Int) = """
            |@${location}
            |D=M
            |@${constant}
            |A=D-A
            |D=M
            |
        """.trimMargin()

        private fun pushSegment(segment: String, index: Int) = """
            |@${segment.toSegment()}
            |D=M
            |@${index}
            |A=D+A
            |D=M
            |
        """.trimMargin() + setPointerValue("SP") + valueIncrease("SP")

        private fun pushConstant(index: Int): String = """
            |@${index}
            |D=A
            |
        """.trimMargin() + setPointerValue("SP") + valueIncrease("SP")

        private fun pushStatic(name: String, index: Int): String =
            getPointer("${name}.${index}") + setPointerValue("SP") + valueIncrease("SP")

        private fun pushTemp(index: Int): String = """
            |@5
            |D=A
            |@${index}
            |A=D+A
            |D=M
            |
        """.trimMargin() + setPointerValue("SP") + valueIncrease("SP")

        private fun pushPointer(index: Int): String =
            when (index) {
                0 -> pushThis()
                1 -> pushThat()
                else -> throw IllegalArgumentException()
            }

        private fun pushThis(): String = getPointer("THIS") + setPointerValue("SP") + valueIncrease("SP")

        private fun pushThat(): String = getPointer("THAT") + setPointerValue("SP") + valueIncrease("SP")

        private fun popSegment(segment: String, index: Int): String = valueDecrease("SP") + """
            |@${segment.toSegment()}
            |D=M
            |@${index}
            |D=D+A
            |@R13
            |M=D
            |
        """.trimMargin() + getPointerValue("SP") + setPointerValue("R13")

        private fun popStatic(name: String, index: Int): String = valueDecrease("SP") +
                getPointerValue("SP") + setPointer("${name}.${index}")

        private fun popTemp(index: Int): String = valueDecrease("SP") + """
            |@5
            |D=A
            |@${index}
            |D=D+A
            |@R13
            |M=D
            |
        """.trimMargin() + getPointerValue("SP") + setPointerValue("R13")

        private fun popPointer(index: Int): String =
            when (index) {
                0 -> popThis()
                1 -> popThat()
                else -> throw IllegalArgumentException()
            }

        private fun popThis(): String = valueDecrease("SP") + getPointerValue("SP") + setPointer("THIS")
        private fun popThat(): String = valueDecrease("SP") + getPointerValue("SP") + setPointer("THAT")

        private fun binaryArithmeticCommand(command: String) =
            valueDecrease("SP") + getPointerValue("SP") + valueDecrease("SP") + setPointerValue(
                "SP",
                command
            ) + valueIncrease("SP")

        private fun unaryArithmeticCommand(command: String) =
            valueDecrease("SP") + setPointerValue("SP", command) + valueIncrease("SP")

        private fun binaryLogicalCommand(command: String, index: Int) = valueDecrease("SP") + getPointerValue("SP") +
                valueDecrease("SP") + getPointerValue("SP", "M-D") + """
                    |@${command}.cmp.${index}
                    |D;J${command}
                    |
                """.trimMargin() + setPointerValue("SP", "0") + """
                    |@END.${index}
                    |0;JMP
                    |(${command}.cmp.${index})
                    |
                """.trimMargin() + setPointerValue("SP", "-1") + """
                    |(END.${index})
                    |
                """.trimMargin() + valueIncrease("SP")

        private fun labelCommand(label: String) = """
            |(${label})
            |
        """.trimMargin()

        private fun unconditionalJump(label: String) = """
            |@${label}
            |0;JMP
            |
        """.trimMargin()

        private fun unconditionalJumpPointer(position: String) = """
            |@${position}
            |A=M
            |0;JMP
            |
        """.trimMargin()

        private fun conditionalJump(label: String, value: String = "D", condition: String = "JNE") = """
            |@${label}
            |${value};${condition}
            |
        """.trimMargin()

        private fun gotoCommand(label: String) = unconditionalJump(label)
        private fun ifGotoCommand(label: String) =
            valueDecrease("SP") + getPointerValue("SP") + conditionalJump(label, "D")

        private fun functionCommand(functionName: String, numVars: Int) =
            labelCommand(functionName) + pushConstant(0).repeat(numVars)

        private fun callCommand(functionName: String, returnFunctionName: String?, numArgs: Int) =
            // push retAddrLabel
            getPointer("$returnFunctionName\$ret.$returnIndex", "A") + setPointerValue("SP") + valueIncrease("SP") +
                    // push LCL
                    getPointer("LCL") + setPointerValue("SP") + valueIncrease("SP") +
                    // push ARG
                    getPointer("ARG") + setPointerValue("SP") + valueIncrease("SP") +
                    // push THIS
                    getPointer("THIS") + setPointerValue("SP") + valueIncrease("SP") +
                    // push THAT
                    getPointer("THAT") + setPointerValue("SP") + valueIncrease("SP") +
                    // ARG = SP - 5 -numArgs
                    getPointerMinusConstant("SP", 5 + numArgs) + setPointer("ARG") +
                    // LCL = SP
                    getPointer("SP") + setPointer("LCL") +
                    // goto functionName
                    unconditionalJump(functionName) +
                    // (retAddrLabel)
                    labelCommand("$returnFunctionName\$ret.${returnIndex++}")

        // use R13 to store temp value in popSegment()
        private fun returnCommand() =
            // R5: endFrame = LCL
            getPointer("LCL") + setPointer("R14") +
                    // R6: retAddr = *(endFrame - 5)
                    getPointerMinusConstantValue("R14", 5) + setPointer("R15") +
                    // *ARG = pop()
                    popSegment("argument", 0) +
                    // SP = ARG + 1
                    getPointer("ARG", "M+1") + setPointer("SP") +
                    // THAT = *(endFrame - 1)
                    getPointerMinusConstantValue("R14", 1) + setPointer("THAT") +
                    // THIS = *(endFrame - 2)
                    getPointerMinusConstantValue("R14", 2) + setPointer("THIS") +
                    // ARG = *(endFrame - 3)
                    getPointerMinusConstantValue("R14", 3) + setPointer("ARG") +
                    // LCL = *(endFrame - 4)
                    getPointerMinusConstantValue("R14", 4) + setPointer("LCL") +
                    // goto retAddr
                    unconditionalJumpPointer("R15")

        private fun bootstrapCommand() = setPointer("SP", 256) + callCommand("Sys.init", null, 0)
    }

    fun write(text: String) {
        outputFile.appendText(text)
    }

    fun writeArithmetic(command: String) {
        write(
            when (command) {
                "add" -> binaryArithmeticCommand("D+M")
                "sub" -> binaryArithmeticCommand("M-D")
                "and" -> binaryArithmeticCommand("D&M")
                "or" -> binaryArithmeticCommand("D|M")
                "neg" -> unaryArithmeticCommand("-M")
                "not" -> unaryArithmeticCommand("!M")
                "eq" -> binaryLogicalCommand(command.toUpperCase(), compareIndex++)
                "gt" -> binaryLogicalCommand(command.toUpperCase(), compareIndex++)
                "lt" -> binaryLogicalCommand(command.toUpperCase(), compareIndex++)
                else -> throw IllegalArgumentException()
            }
        )
    }

    fun writePushPop(type: Type, segment: String, index: Int, fileName: String) {
        when (type) {
            C_PUSH -> writePush(segment, index, fileName)
            C_POP -> writePop(segment, index, fileName)
            else -> throw IllegalArgumentException()
        }
    }

    private fun writePush(segment: String, index: Int, fileName: String) {
        write(
            when (segment) {
                "local", "argument", "this", "that" -> pushSegment(segment, index)
                "constant" -> pushConstant(index)
                "static" -> pushStatic(fileName, index)
                "temp" -> pushTemp(index)
                "pointer" -> pushPointer(index)
                else -> throw IllegalArgumentException()
            }
        )
    }

    private fun writePop(segment: String, index: Int, fileName: String) {
        write(
            when (segment) {
                "local", "argument", "this", "that" -> popSegment(segment, index)
                "static" -> popStatic(fileName, index)
                "temp" -> popTemp(index)
                "pointer" -> popPointer(index)
                else -> throw IllegalArgumentException()
            }
        )
    }

    fun writeInit() {
        write(bootstrapCommand())
    }

    fun writeLabel(label: String) {
        write(labelCommand("$currentFunctionName\$$label"))
    }

    fun writeGoto(label: String) {
        write(gotoCommand("$currentFunctionName\$$label"))
    }

    fun writeIf(label: String) {
        write(ifGotoCommand("$currentFunctionName\$$label"))
    }

    fun writeFunction(functionName: String, numVars: Int) {
        currentFunctionName = functionName
        write(functionCommand(functionName, numVars))
    }

    fun writeCall(functionName: String, numArgs: Int) {
        write(callCommand(functionName, currentFunctionName, numArgs))
    }

    fun writeReturn() {
        write(returnCommand())
    }
}