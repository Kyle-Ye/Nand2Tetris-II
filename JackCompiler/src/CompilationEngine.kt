@file:Suppress("DuplicatedCode")

import kotlin.IllegalArgumentException

class CompilationEngine(
    private val jackTokenizer: JackTokenizer,
    private val xmlWriter: XmlWriter,
    private val vmWriter: VMWriter,
    private val table: SymbolTable
) {
    companion object {
        val classVarKeywords = setOf(Keyword.STATIC, Keyword.FIELD)
        val subroutineKeywords = setOf(Keyword.CONSTRUCTOR, Keyword.FUNCTION, Keyword.METHOD)
        val typeKeywords = setOf(Keyword.INT, Keyword.CHAR, Keyword.BOOLEAN)
        val returnTypeKeywords = typeKeywords + Keyword.VOID
        val statementKeywords = setOf(Keyword.LET, Keyword.IF, Keyword.WHILE, Keyword.DO, Keyword.RETURN)
        val constantKeywords = setOf(Keyword.TRUE, Keyword.FALSE, Keyword.NULL, Keyword.THIS)

        val operatorSymbols = setOf('+', '-', '*', '/', '&', '|', '<', '>', '=')
        val unaryOperatorSymbols = setOf('-', '~')


        private fun String.toEnumFormat(): String {
            // Since there has no 2-words keyword, we simply return its uppercase
            return toUpperCase()
        }
    }

    private fun VMWriter.writePushTable(name: String) {
        if (table.kindOf(name) == Kind.FIELD ) {
            if (subroutineType == Keyword.CONSTRUCTOR) writePush(Segment.LOCAL,0)
            else writePush(Segment.ARGUMENT, 0)
            writePop(Segment.POINTER, 0)
        }
        writePush(table.kindOf(name).toSegment(), table.indexOf(name))
    }
    private fun VMWriter.writePopTable(name: String) {
        if (table.kindOf(name) == Kind.FIELD ) {
            if (subroutineType == Keyword.CONSTRUCTOR) writePush(Segment.LOCAL,0)
            else writePush(Segment.ARGUMENT, 0)
            writePop(Segment.POINTER, 0)
        }
        writePop(table.kindOf(name).toSegment(), table.indexOf(name))
    }

    private lateinit var className: String
    private var filedNumber: Int = 0
    private lateinit var subroutineName: String
    private lateinit var subroutineType: Keyword
    private var ifIndex = 0
    private var whileIndex = 0
    /**
     * 1) (xx)*
     * ```
     * jackTokenizer.advance()
     * while(condition) {
     *      do xx
     *      jackTokenizer.advance()
     * }
     * jackTokenizer.pushBack()
     * ```
     *
     * 2) (xx)?
     * ```
     * jackTokenizer.advance()
     * if (condition) {
     * do xx
     * } else {
     * jackTokenizer.pushBack()
     * }
     * ```
     */


    fun start() {
        if (jackTokenizer.hasMoreTokens()) {
            jackTokenizer.advance()
            if (Keyword.valueOf(jackTokenizer.keyword().toEnumFormat()) == Keyword.CLASS) {
                compileClass()
            } else {
                throw IllegalCallerException()
            }
        }

    }


    private fun typeHelper(set: Set<Keyword>): String {
        return when (jackTokenizer.tokenType()) {
            TokenType.KEYWORD -> if (Keyword.valueOf(jackTokenizer.keyword().toEnumFormat()) in set) {
                xmlWriter.writeTag(jackTokenizer.tokenType().toString(), jackTokenizer.keyword())
                jackTokenizer.keyword()
            } else throw IllegalArgumentException()
            TokenType.IDENTIFIER -> {
                compileIdentifier(IdentifierType.CLASS)
                jackTokenizer.identifier()
            }
            else -> throw IllegalArgumentException()
        }
    }

    private fun compileClass() {
        val tag = Tag.CLASS
        xmlWriter.writeStartTag(tag.toString())
        xmlWriter.writeTag(jackTokenizer.tokenType().toString(), jackTokenizer.keyword())

        jackTokenizer.advance()
        className = jackTokenizer.identifier()
        compileIdentifier(IdentifierType.CLASS)

        jackTokenizer.advance()
        xmlWriter.writeTag(jackTokenizer.tokenType().toString(), jackTokenizer.symbol(), '{')

        jackTokenizer.advance()
        while (jackTokenizer.tokenType() == TokenType.KEYWORD && Keyword.valueOf(jackTokenizer.keyword().toEnumFormat()) in classVarKeywords) {
            compileClassVarDec()
            jackTokenizer.advance()
        }
        jackTokenizer.pushBack()

        jackTokenizer.advance()
        while (jackTokenizer.tokenType() == TokenType.KEYWORD && Keyword.valueOf(jackTokenizer.keyword().toEnumFormat()) in subroutineKeywords) {
            subroutineType = Keyword.valueOf(jackTokenizer.keyword().toEnumFormat())
            compileSubroutineDec()
            jackTokenizer.advance()
        }
        jackTokenizer.pushBack()

        jackTokenizer.advance()
        xmlWriter.writeTag(jackTokenizer.tokenType().toString(), jackTokenizer.symbol(), '}')
        xmlWriter.writeEndTag(tag.toString())
    }

    private fun compileClassVarDec() {
        val tag = Tag.CLASS_VAR_DEC
        xmlWriter.writeStartTag(tag.toString())
        val keyword = Keyword.valueOf(jackTokenizer.keyword().toEnumFormat())
        xmlWriter.writeTag(jackTokenizer.tokenType().toString(), jackTokenizer.keyword())
        val identifierType = IdentifierType.valueOf(jackTokenizer.keyword().toUpperCase())

        jackTokenizer.advance()
        val type = typeHelper(typeKeywords)

        jackTokenizer.advance()
        compileIdentifier(identifierType)
        table.define(jackTokenizer.identifier(), type, Kind.valueOf(identifierType.toString()))
        if (keyword == Keyword.FIELD) filedNumber++

        jackTokenizer.advance()
        while (jackTokenizer.tokenType() == TokenType.SYMBOL && jackTokenizer.symbol() == ',') {
            xmlWriter.writeTag(jackTokenizer.tokenType().toString(), jackTokenizer.symbol(), ',')
            jackTokenizer.advance()
            compileIdentifier(identifierType)
            table.define(jackTokenizer.identifier(), type, Kind.valueOf(identifierType.toString()))
            if (keyword == Keyword.FIELD) filedNumber++
            jackTokenizer.advance()
        }
        jackTokenizer.pushBack()

        jackTokenizer.advance()
        xmlWriter.writeTag(jackTokenizer.tokenType().toString(), jackTokenizer.symbol(), ';')
        xmlWriter.writeEndTag(tag.toString())
    }

    private fun compileSubroutineDec() {
        ifIndex = 0
        whileIndex = 0
        table.startSubroutine()

        val tag = Tag.SUBROUTINE_DEC
        xmlWriter.writeStartTag(tag.toString())
        xmlWriter.writeTag(jackTokenizer.tokenType().toString(), jackTokenizer.keyword())

        jackTokenizer.advance()
        typeHelper(returnTypeKeywords)

        jackTokenizer.advance()
        subroutineName = jackTokenizer.identifier()
        compileIdentifier(IdentifierType.SUBROUTINE)

        jackTokenizer.advance()
        xmlWriter.writeTag(jackTokenizer.tokenType().toString(), jackTokenizer.symbol(), '(')

        if (subroutineType == Keyword.METHOD) {
            table.define("this", className, Kind.ARGUMENT)
        }
        jackTokenizer.advance()
        compileParameterList()

        jackTokenizer.advance()
        xmlWriter.writeTag(jackTokenizer.tokenType().toString(), jackTokenizer.symbol(), ')')

        jackTokenizer.advance()
        compileSubroutineBody()

        xmlWriter.writeEndTag(tag.toString())
    }

    private fun compileParameterList() {
        val tag = Tag.PARAMETER_LIST
        xmlWriter.writeStartTag(tag.toString())

        if (jackTokenizer.tokenType() != TokenType.SYMBOL) {
            var type = typeHelper(typeKeywords)

            jackTokenizer.advance()
            compileIdentifier(IdentifierType.ARGUMENT)
            table.define(jackTokenizer.identifier(), type, Kind.ARGUMENT)

            jackTokenizer.advance()
            while (jackTokenizer.symbol() == ',') {
                xmlWriter.writeTag(jackTokenizer.tokenType().toString(), jackTokenizer.symbol(), ',')

                jackTokenizer.advance()
                type = typeHelper(typeKeywords)

                jackTokenizer.advance()
                compileIdentifier(IdentifierType.ARGUMENT)
                table.define(jackTokenizer.identifier(), type, Kind.ARGUMENT)

                jackTokenizer.advance()
            }
            jackTokenizer.pushBack()

        } else {
            jackTokenizer.pushBack()
        }
        xmlWriter.writeEndTag(tag.toString())
    }

    private fun compileSubroutineBody() {
        val tag = Tag.SUBROUTINE_BODY
        xmlWriter.writeStartTag(tag.toString())
        xmlWriter.writeTag(jackTokenizer.tokenType().toString(), jackTokenizer.symbol(), '{')

        var nLocals = 0

        if (subroutineType == Keyword.CONSTRUCTOR){
            table.define("this",className,Kind.VAR)
            nLocals ++
        }

        jackTokenizer.advance()
        while (jackTokenizer.tokenType() == TokenType.KEYWORD && Keyword.valueOf(jackTokenizer.keyword().toEnumFormat()) == Keyword.VAR) {
            nLocals += compileVarDec()
            jackTokenizer.advance()
        }
        jackTokenizer.pushBack()

        vmWriter.writeFunction("$className.$subroutineName", nLocals)

        if (subroutineType == Keyword.CONSTRUCTOR) {
            vmWriter.apply {
                writePush(Segment.CONSTANT, filedNumber)
                writeCall("Memory.alloc", 1)
                writePop(Segment.LOCAL, 0)
            }
        }

        jackTokenizer.advance()
        compileStatements()

        jackTokenizer.advance()
        xmlWriter.writeTag(jackTokenizer.tokenType().toString(), jackTokenizer.symbol(), '}')
        xmlWriter.writeEndTag(tag.toString())
    }

    private fun compileVarDec(): Int {
        var number = 0
        val tag = Tag.VAR_DEC
        xmlWriter.writeStartTag(tag.toString())
        xmlWriter.writeTag(jackTokenizer.tokenType().toString(), jackTokenizer.keyword())

        jackTokenizer.advance()
        val type = typeHelper(typeKeywords)

        jackTokenizer.advance()
        compileIdentifier(IdentifierType.VAR)
        table.define(jackTokenizer.identifier(), type, Kind.VAR)
        number++

        jackTokenizer.advance()
        while (jackTokenizer.symbol() == ',') {
            xmlWriter.writeTag(jackTokenizer.tokenType().toString(), jackTokenizer.symbol(), ',')
            jackTokenizer.advance()
            compileIdentifier(IdentifierType.VAR)
            table.define(jackTokenizer.identifier(), type, Kind.VAR)
            number++

            jackTokenizer.advance()
        }
        jackTokenizer.pushBack()

        jackTokenizer.advance()
        xmlWriter.writeTag(jackTokenizer.tokenType().toString(), jackTokenizer.symbol(), ';')
        xmlWriter.writeEndTag(tag.toString())
        return number
    }

    private fun compileStatements() {
        val tag = Tag.STATEMENTS
        xmlWriter.writeStartTag(tag.toString())

        while (jackTokenizer.tokenType() == TokenType.KEYWORD && Keyword.valueOf(jackTokenizer.keyword().toEnumFormat()) in statementKeywords) {
            when (Keyword.valueOf(jackTokenizer.keyword().toEnumFormat())) {
                Keyword.LET -> compileLet()
                Keyword.IF -> compileIf()
                Keyword.WHILE -> compileWhile()
                Keyword.DO -> compileDo()
                Keyword.RETURN -> compileReturn()
                else -> throw IllegalAccessException()
            }
            jackTokenizer.advance()
        }
        jackTokenizer.pushBack()
        xmlWriter.writeEndTag(tag.toString())
    }

    private fun compileLet() {
        var ifArray = false
        val tag = Tag.LET_STATEMENT
        xmlWriter.writeStartTag(tag.toString())
        xmlWriter.writeTag(jackTokenizer.tokenType().toString(), jackTokenizer.keyword())

        jackTokenizer.advance()
        val name = jackTokenizer.identifier()


        jackTokenizer.advance()
        if (jackTokenizer.tokenType() == TokenType.SYMBOL && jackTokenizer.symbol() == '[') {
            ifArray = true

            vmWriter.apply {
                writePushTable(name)
            }

            compileIdentifier(IdentifierType.valueOf(table.kindOf(name).toString()), name)
            xmlWriter.writeTag(jackTokenizer.tokenType().toString(), jackTokenizer.symbol(), '[')
            jackTokenizer.advance()
            compileExpression()
            jackTokenizer.advance()
            xmlWriter.writeTag(jackTokenizer.tokenType().toString(), jackTokenizer.symbol(), ']')

            vmWriter.apply {
                writeArithmetic(Command.ADD)
            }

        } else {
            compileIdentifier(IdentifierType.valueOf(table.kindOf(name).toString()), name)
            jackTokenizer.pushBack()
        }

        jackTokenizer.advance()
        xmlWriter.writeTag(jackTokenizer.tokenType().toString(), jackTokenizer.symbol(), '=')

        jackTokenizer.advance()
        compileExpression()
        vmWriter.apply {
            if (ifArray) {
                writePop(Segment.TEMP, 0)
                writePop(Segment.POINTER, 1)
                writePush(Segment.TEMP, 0)
                writePop(Segment.THAT, 0)
            } else {
                writePopTable(name)
            }
        }

        jackTokenizer.advance()
        xmlWriter.writeTag(jackTokenizer.tokenType().toString(), jackTokenizer.symbol(), ';')
        xmlWriter.writeEndTag(tag.toString())
    }

    private fun compileIf() {
        val tag = Tag.IF_STATEMENT
        xmlWriter.writeStartTag(tag.toString())
        xmlWriter.writeTag(jackTokenizer.tokenType().toString(), jackTokenizer.keyword())

        jackTokenizer.advance()
        xmlWriter.writeTag(jackTokenizer.tokenType().toString(), jackTokenizer.symbol(), '(')

        jackTokenizer.advance()
        compileExpression()

        jackTokenizer.advance()
        xmlWriter.writeTag(jackTokenizer.tokenType().toString(), jackTokenizer.symbol(), ')')

        val index = ifIndex++
        vmWriter.apply {
            writeArithmetic(Command.NOT)
            writeIf("$className.$subroutineName.IF_L1\$$index")
        }

        jackTokenizer.advance()
        xmlWriter.writeTag(jackTokenizer.tokenType().toString(), jackTokenizer.symbol(), '{')

        jackTokenizer.advance()
        compileStatements()

        vmWriter.apply {
            writeGoto("$className.$subroutineName.IF_L2\$$index")
            writeLabel("$className.$subroutineName.IF_L1\$$index")
        }

        jackTokenizer.advance()
        xmlWriter.writeTag(jackTokenizer.tokenType().toString(), jackTokenizer.symbol(), '}')

        jackTokenizer.advance()
        if (jackTokenizer.tokenType() == TokenType.KEYWORD && Keyword.valueOf(jackTokenizer.keyword().toEnumFormat()) == Keyword.ELSE) {
            xmlWriter.writeTag(jackTokenizer.tokenType().toString(), jackTokenizer.keyword())

            jackTokenizer.advance()
            xmlWriter.writeTag(jackTokenizer.tokenType().toString(), jackTokenizer.symbol(), '{')

            jackTokenizer.advance()
            compileStatements()

            jackTokenizer.advance()
            xmlWriter.writeTag(jackTokenizer.tokenType().toString(), jackTokenizer.symbol(), '}')
        } else {
            jackTokenizer.pushBack()
        }

        vmWriter.apply {
            writeLabel("$className.$subroutineName.IF_L2\$$index")
        }

        xmlWriter.writeEndTag(tag.toString())
    }

    private fun compileWhile() {
        val tag = Tag.WHILE_STATEMENT
        xmlWriter.writeStartTag(tag.toString())
        xmlWriter.writeTag(jackTokenizer.tokenType().toString(), jackTokenizer.keyword())

        val index = whileIndex++
        vmWriter.apply {
            writeLabel("$className.$subroutineName.WHILE_L1\$$index")
        }

        jackTokenizer.advance()
        xmlWriter.writeTag(jackTokenizer.tokenType().toString(), jackTokenizer.symbol(), '(')

        jackTokenizer.advance()
        compileExpression()

        jackTokenizer.advance()
        xmlWriter.writeTag(jackTokenizer.tokenType().toString(), jackTokenizer.symbol(), ')')

        vmWriter.apply {
            writeArithmetic(Command.NOT)
            writeIf("$className.$subroutineName.WHILE_L2\$$index")
        }

        jackTokenizer.advance()
        xmlWriter.writeTag(jackTokenizer.tokenType().toString(), jackTokenizer.symbol(), '{')

        jackTokenizer.advance()
        compileStatements()

        jackTokenizer.advance()
        xmlWriter.writeTag(jackTokenizer.tokenType().toString(), jackTokenizer.symbol(), '}')

        vmWriter.apply {
            writeGoto("$className.$subroutineName.WHILE_L1\$$index")
            writeLabel("$className.$subroutineName.WHILE_L2\$$index")
        }

        xmlWriter.writeEndTag(tag.toString())
    }

    private fun compileDo() {
        val tag = Tag.DO_STATEMENT
        xmlWriter.writeStartTag(tag.toString())
        xmlWriter.writeTag(jackTokenizer.tokenType().toString(), jackTokenizer.keyword())

        // compileSubroutineCall
        jackTokenizer.advance()
        val name = jackTokenizer.identifier()
        var nArgs = 0
        jackTokenizer.advance()
        val callee = if (jackTokenizer.tokenType() == TokenType.SYMBOL && jackTokenizer.symbol() == '.') {
            val calleeClassName = if (name in table) {
                vmWriter.apply {
                    writePushTable(name)
                }
                nArgs++
                compileIdentifier(IdentifierType.valueOf(table.kindOf(name).toString()), name)
                table.typeOf(name)
            } else {
                compileIdentifier(IdentifierType.CLASS, name)
                name
            }
            xmlWriter.writeTag(jackTokenizer.tokenType().toString(), jackTokenizer.symbol(), '.')
            jackTokenizer.advance()
            val subroutineName = jackTokenizer.identifier()
            compileIdentifier(IdentifierType.SUBROUTINE)
            "$calleeClassName.$subroutineName"
        } else {

            vmWriter.apply {
                if (subroutineType == Keyword.CONSTRUCTOR) writePush(Segment.LOCAL,0)
                else writePush(Segment.ARGUMENT, 0)
            }
            nArgs++
            compileIdentifier(IdentifierType.SUBROUTINE, name)
            jackTokenizer.pushBack()
            "$className.$name"
        }

        jackTokenizer.advance()
        xmlWriter.writeTag(jackTokenizer.tokenType().toString(), jackTokenizer.symbol(), '(')
        jackTokenizer.advance()
        nArgs += compileExpressionList()
        jackTokenizer.advance()
        xmlWriter.writeTag(jackTokenizer.tokenType().toString(), jackTokenizer.symbol(), ')')

        vmWriter.apply {
            writeCall(callee, nArgs)
            writePop(Segment.TEMP, 0)
        }

        jackTokenizer.advance()
        xmlWriter.writeTag(jackTokenizer.tokenType().toString(), jackTokenizer.symbol(), ';')
        xmlWriter.writeEndTag(tag.toString())
    }

    private fun compileReturn() {
        val tag = Tag.RETURN_STATEMENT
        xmlWriter.writeStartTag(tag.toString())
        xmlWriter.writeTag(jackTokenizer.tokenType().toString(), jackTokenizer.keyword())

        jackTokenizer.advance()
        if (!(jackTokenizer.tokenType() == TokenType.SYMBOL && jackTokenizer.symbol() == ';')) {
            compileExpression()
        } else {
            vmWriter.apply {
                writePush(Segment.CONSTANT, 0)
            }
            jackTokenizer.pushBack()
        }

        vmWriter.apply {
            writeReturn()
        }

        jackTokenizer.advance()
        xmlWriter.writeTag(jackTokenizer.tokenType().toString(), jackTokenizer.symbol(), ';')
        xmlWriter.writeEndTag(tag.toString())
    }

    private fun compileExpression() {
        val tag = Tag.EXPRESSION
        xmlWriter.writeStartTag(tag.toString())
        compileTerm()

        jackTokenizer.advance()
        while (jackTokenizer.tokenType() == TokenType.SYMBOL && jackTokenizer.symbol() in operatorSymbols) {
            val symbol = jackTokenizer.symbol()
            xmlWriter.writeTag(jackTokenizer.tokenType().toString(), jackTokenizer.symbol(), true)
            jackTokenizer.advance()
            compileTerm()

            vmWriter.apply {
                when (symbol) {
                    '+' -> writeArithmetic(Command.ADD)
                    '-' -> writeArithmetic(Command.SUB)
                    '*' -> writeCall("Math.multiply", 2)
                    '/' -> writeCall("Math.divide", 2)
                    '&' -> writeArithmetic(Command.AND)
                    '|' -> writeArithmetic(Command.OR)
                    '>' -> writeArithmetic(Command.GT)
                    '<' -> writeArithmetic(Command.LT)
                    '=' -> writeArithmetic(Command.EQ)
                }
            }

            jackTokenizer.advance()
        }
        jackTokenizer.pushBack()

        xmlWriter.writeEndTag(tag.toString())
    }

    private fun compileTerm() {
        val tag = Tag.TERM
        xmlWriter.writeStartTag(tag.toString())

        when (jackTokenizer.tokenType()) {
            TokenType.INTEGER_CONSTANT -> {
                vmWriter.apply {
                    writePush(Segment.CONSTANT, jackTokenizer.intVal())
                }
                xmlWriter.writeTag(jackTokenizer.tokenType().toString(), jackTokenizer.intVal().toString())
            }
            TokenType.STRING_CONSTANT -> {
                vmWriter.apply {
                    writePush(Segment.CONSTANT, jackTokenizer.stringVal().length)
                    writeCall("String.new", 1)
                    for (i in jackTokenizer.stringVal().chars()) {
                        writePush(Segment.CONSTANT, i)
                        writeCall("String.appendChar", 2)
                    }
                }
                xmlWriter.writeTag(jackTokenizer.tokenType().toString(), jackTokenizer.stringVal())
            }
            TokenType.KEYWORD -> {
                val keyword = Keyword.valueOf(jackTokenizer.keyword().toEnumFormat())
                if (keyword in constantKeywords) {
                    xmlWriter.writeTag(jackTokenizer.tokenType().toString(), jackTokenizer.keyword())
                    vmWriter.apply {
                        when (keyword) {
                            Keyword.TRUE -> {
                                writePush(Segment.CONSTANT, 1)
                                writeArithmetic(Command.NEG)
                            }
                            Keyword.FALSE -> writePush(Segment.CONSTANT, 0)
                            Keyword.NULL -> writePush(Segment.CONSTANT, 0)
                            // Only shown in constructor
                            Keyword.THIS -> writePush(Segment.LOCAL, 0)
                            else->throw Exception("Should never reach here")
                        }
                    }
                } else throw IllegalArgumentException()
            }
            TokenType.IDENTIFIER -> {
                val name = jackTokenizer.identifier()
                jackTokenizer.advance()
                if (jackTokenizer.tokenType() == TokenType.SYMBOL) {
                    when (jackTokenizer.symbol()) {
                        '[' -> {
                            vmWriter.apply {
                                writePushTable(name)
                            }
                            compileIdentifier(IdentifierType.valueOf(table.kindOf(name).toString()), name)
                            xmlWriter.writeTag(jackTokenizer.tokenType().toString(), jackTokenizer.symbol(), '[')
                            jackTokenizer.advance()
                            compileExpression()
                            jackTokenizer.advance()
                            xmlWriter.writeTag(jackTokenizer.tokenType().toString(), jackTokenizer.symbol(), ']')
                            vmWriter.apply {
                                writeArithmetic(Command.ADD)
                                writePop(Segment.POINTER, 1)
                                writePush(Segment.THAT, 0)
                            }
                        }
                        '(' -> {
                            compileIdentifier(IdentifierType.SUBROUTINE, name)
                            xmlWriter.writeTag(jackTokenizer.tokenType().toString(), jackTokenizer.symbol(), '(')
                            jackTokenizer.advance()
                            val nArgs = compileExpressionList()
                            jackTokenizer.advance()
                            xmlWriter.writeTag(jackTokenizer.tokenType().toString(), jackTokenizer.symbol(), ')')
                            vmWriter.apply {
                                writeCall("$className.$name", nArgs)
                            }
                        }
                        '.' -> {
                            var nArgs = 0
                            val calleeClassName = if (name in table) {
                                vmWriter.apply {
                                    writePushTable(name)
                                }
                                nArgs++
                                compileIdentifier(IdentifierType.valueOf(table.kindOf(name).toString()), name)
                                table.typeOf(name)
                            } else {
                                compileIdentifier(IdentifierType.CLASS, name)
                                name
                            }
                            xmlWriter.writeTag(jackTokenizer.tokenType().toString(), jackTokenizer.symbol(), '.')
                            jackTokenizer.advance()
                            val subroutineName = jackTokenizer.identifier()
                            compileIdentifier(IdentifierType.SUBROUTINE)
                            jackTokenizer.advance()
                            xmlWriter.writeTag(jackTokenizer.tokenType().toString(), jackTokenizer.symbol(), '(')
                            jackTokenizer.advance()
                            nArgs += compileExpressionList()
                            jackTokenizer.advance()
                            xmlWriter.writeTag(jackTokenizer.tokenType().toString(), jackTokenizer.symbol(), ')')
                            vmWriter.apply {
                                writeCall("$calleeClassName.$subroutineName", nArgs)
                            }
                        }
                        else -> {
                            compileIdentifier(IdentifierType.valueOf(table.kindOf(name).toString()), name)
                            vmWriter.apply {
                                writePushTable(name)
                            }
                            jackTokenizer.pushBack()
                        }
                    }
                }
            }
            TokenType.SYMBOL ->
                when (jackTokenizer.symbol()) {
                    '(' -> {
                        xmlWriter.writeTag(jackTokenizer.tokenType().toString(), jackTokenizer.symbol(), '(')
                        jackTokenizer.advance()
                        compileExpression()
                        jackTokenizer.advance()
                        xmlWriter.writeTag(jackTokenizer.tokenType().toString(), jackTokenizer.symbol(), ')')
                    }
                    in unaryOperatorSymbols -> {
                        val symbol = jackTokenizer.symbol()
                        xmlWriter.writeTag(jackTokenizer.tokenType().toString(), symbol, true)
                        jackTokenizer.advance()
                        compileTerm()
                        vmWriter.apply {
                            when (symbol) {
                                '-' -> writeArithmetic(Command.NEG)
                                '~' -> writeArithmetic(Command.NOT)
                            }
                        }
                    }
                    else -> throw IllegalArgumentException()
                }
        }

        xmlWriter.writeEndTag(tag.toString())
    }

    private fun compileExpressionList(): Int {
        var nArg = 0
        val tag = Tag.EXPRESSION_LIST
        xmlWriter.writeStartTag(tag.toString())
        if (!(jackTokenizer.tokenType() == TokenType.SYMBOL && jackTokenizer.symbol() == ')')) {
            compileExpression()
            nArg++
            jackTokenizer.advance()
            while (jackTokenizer.tokenType() == TokenType.SYMBOL && jackTokenizer.symbol() == ',') {
                xmlWriter.writeTag(jackTokenizer.tokenType().toString(), jackTokenizer.symbol(), ',')
                jackTokenizer.advance()
                compileExpression()
                nArg++
                jackTokenizer.advance()
            }
            jackTokenizer.pushBack()
        } else {
            jackTokenizer.pushBack()
        }
        xmlWriter.writeEndTag(tag.toString())
        return nArg
    }

    private fun compileIdentifier(identifierType: IdentifierType, name: String = jackTokenizer.identifier()) {
        val tag = Tag.IDENTIFIER
        xmlWriter.writeStartTag(tag.toString())
        xmlWriter.writeTag(identifierType.toString().toLowerCase(), name)
        xmlWriter.writeEndTag(tag.toString())
    }
}