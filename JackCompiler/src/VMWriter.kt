import java.io.File

class VMWriter(private val output: File) {

    fun writePush(segment: Segment, index: Int) {
        writeln("push $segment $index")
    }

    fun writePop(segment: Segment, index: Int) {
        if (segment == Segment.CONSTANT)
            throw IllegalArgumentException()
        writeln("pop $segment $index")
    }

    fun writeArithmetic(command: Command) {
        writeln(command.toString().toLowerCase())
    }

    fun writeLabel(label: String) {
        writeln("label $label")
    }

    fun writeGoto(label: String) {
        writeln("goto $label")
    }

    fun writeIf(label: String) {
        writeln("if-goto $label")
    }

    fun writeCall(name: String, nArgs: Int) {
        writeln("call $name $nArgs")
    }

    fun writeFunction(name: String, nLocals: Int) {
        writeln("function $name $nLocals")
    }

    fun writeReturn() {
        writeln("return")
    }

    private fun writeln(text: String) {
        output.appendText("$text\n")
    }
}