import java.io.File

fun main(args: Array<String>) {
    val path =
        if (args.isNotEmpty()) args[0]
        else "/Volumes/MacWorkplace/Coursera/build-a-computer/nand2tetris/projects/08/ProgramFlow/BasicLoop/BasicLoop.vm"
    val hasComment =
        if (args.size > 1) args[1].toBoolean()
        else false

    val parser = Parser(hasComment)

    val file = File(path)
    val outputPath = (if (file.isFile) file.parent else file.path) + File.separator + file.nameWithoutExtension + ".asm"
    val outputFile = File(outputPath)
    outputFile.writeText("")

    val writer = CodeWriter(outputFile)

    if (file.isFile && file.extension == "vm") {

        parser.process(file, writer)
    } else if (file.isDirectory) {
        writer.writeInit()
        file.walk()
            .filter { it.isFile }
            .filter { it.extension == "vm" }
            .forEach {
                parser.process(it, writer)
            }
    }
}

