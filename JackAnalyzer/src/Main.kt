import java.io.File

fun main(args: Array<String>) {
    val path =
        if (args.isNotEmpty()) args[0]
        else "/Volumes/MacWorkplace/Workplace/Kotlin/Nand2Tetris/JackAnalyzer/test.jack"
    val file = File(path)

    if(file.isDirectory){
        file.walk()
            .filter { it.isFile }
            .filter { it.extension == "jack" }
            .forEach {
                process(it)
            }
    }else if (file.extension == "jack"){
        process(file)
    }
}

private fun process(file:File){
    val jackTokenizer = JackTokenizer(file)
    val outputPath = file.parent + File.separator + file.nameWithoutExtension + ".xml"
    val outputFile = File(outputPath)
    outputFile.writeText("")
    val writer = XmlWriter(outputFile)
    val compilationEngine = CompilationEngine(jackTokenizer,writer)

    compilationEngine.start()
}
