import java.io.File

fun main(args: Array<String>) {
    val path =
        if (args.isNotEmpty()) args[0]
//        else "/Volumes/MacWorkplace/Coursera/build-a-computer/nand2tetris/projects/11/ComplexArrays"
        else "/Volumes/MacWorkplace/Workplace/Kotlin/Nand2Tetris/JackCompiler/test.jack"
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
    val outputPath = file.parent + File.separator + file.nameWithoutExtension
    val xmlFile = File("$outputPath.xml")
    val vmFile = File("$outputPath.vm")
    xmlFile.writeText("")
    vmFile.writeText("")
    val xmlWriter = XmlWriter(xmlFile)
    val vmWriter = VMWriter(vmFile)
    val table = SymbolTable()
    val compilationEngine = CompilationEngine(jackTokenizer,xmlWriter,vmWriter,table)

    compilationEngine.start()
}
