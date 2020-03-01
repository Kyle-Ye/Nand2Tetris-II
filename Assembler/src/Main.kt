import java.io.File


fun main(args: Array<String>) {
    val path = File(args[0])
    val fileTree = path.walk()
    fileTree
        .filter { it.isFile }
        .filter { it.extension == "asm" }
        .forEach {
            assembly(it)
        }
}

fun assembly(inputFile: File) {
    val outputPath = inputFile.parent + '/' + inputFile.nameWithoutExtension + ".hack"
    val outputFile = File(outputPath)

    outputFile.writeText("")

    val symbolTable = SymbolTable()
    val parser = Parser(symbolTable)
    parser.preProcess(inputFile)
    parser.process(inputFile, outputFile)
}


