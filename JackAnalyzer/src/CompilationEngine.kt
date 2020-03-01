@file:Suppress("DuplicatedCode")

import kotlin.IllegalArgumentException

enum class Tag {
    CLASS, CLASS_VAR_DEC, SUBROUTINE_DEC, PARAMETER_LIST,
    SUBROUTINE_BODY, VAR_DEC, STATEMENTS, LET_STATEMENT,
    IF_STATEMENT, WHILE_STATEMENT, DO_STATEMENT, RETURN_STATEMENT,
    EXPRESSION, TERM, EXPRESSION_LIST;

    override fun toString(): String {
        return super.toString().toLowerCase().replace(Regex("_.")) {
            it.value[1].toUpperCase().toString()
        }
    }
}

enum class Keyword {
    CLASS, CONSTRUCTOR, FUNCTION, METHOD,
    FIELD, STATIC, VAR, INT,
    CHAR, BOOLEAN, VOID, TRUE,
    FALSE, NULL, THIS, LET,
    DO, IF, ELSE, WHILE,
    RETURN;

    override fun toString(): String {
        return super.toString().toLowerCase()
    }
}

class CompilationEngine(private val jackTokenizer: JackTokenizer, private val writer: XmlWriter) {
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

    private fun hasTypeToken(): Boolean {
        return jackTokenizer.tokenType() == TokenType.KEYWORD && Keyword.valueOf(jackTokenizer.keyword().toEnumFormat()) in typeKeywords
    }

    private fun typeHelper(set: Set<Keyword>) {
        writer.writeTag(
            jackTokenizer.tokenType().toString(), when (jackTokenizer.tokenType()) {
                TokenType.KEYWORD -> if (Keyword.valueOf(jackTokenizer.keyword().toEnumFormat()) in set) {
                    jackTokenizer.keyword()
                } else throw IllegalArgumentException()
                TokenType.IDENTIFIER -> jackTokenizer.identifier()
                else -> throw IllegalArgumentException()
            }
        )
    }

    private fun compileClass() {
        val tag = Tag.CLASS
        writer.writeStartTag(tag.toString())
        writer.writeTag(jackTokenizer.tokenType().toString(), jackTokenizer.keyword())

        jackTokenizer.advance()
        writer.writeTag(jackTokenizer.tokenType().toString(), jackTokenizer.identifier())

        jackTokenizer.advance()
        writer.writeTag(jackTokenizer.tokenType().toString(), jackTokenizer.symbol(), '{')

        jackTokenizer.advance()
        while (jackTokenizer.tokenType() == TokenType.KEYWORD && Keyword.valueOf(jackTokenizer.keyword().toEnumFormat()) in classVarKeywords) {
            compileClassVarDec()
            jackTokenizer.advance()
        }
        jackTokenizer.pushBack()

        jackTokenizer.advance()
        while (jackTokenizer.tokenType() == TokenType.KEYWORD && Keyword.valueOf(jackTokenizer.keyword().toEnumFormat()) in subroutineKeywords) {
            compileSubroutineDec()
            jackTokenizer.advance()
        }
        jackTokenizer.pushBack()

        jackTokenizer.advance()
        writer.writeTag(jackTokenizer.tokenType().toString(), jackTokenizer.symbol(), '}')
        writer.writeEndTag(tag.toString())
    }

    private fun compileClassVarDec() {
        val tag = Tag.CLASS_VAR_DEC
        writer.writeStartTag(tag.toString())
        writer.writeTag(jackTokenizer.tokenType().toString(), jackTokenizer.keyword())

        jackTokenizer.advance()
        typeHelper(typeKeywords)

        jackTokenizer.advance()
        writer.writeTag(jackTokenizer.tokenType().toString(), jackTokenizer.identifier())

        jackTokenizer.advance()
        while (jackTokenizer.tokenType() == TokenType.SYMBOL && jackTokenizer.symbol() == ',') {
            writer.writeTag(jackTokenizer.tokenType().toString(), jackTokenizer.symbol(), ',')
            jackTokenizer.advance()
            writer.writeTag(jackTokenizer.tokenType().toString(), jackTokenizer.identifier())
            jackTokenizer.advance()
        }
        jackTokenizer.pushBack()

        jackTokenizer.advance()
        writer.writeTag(jackTokenizer.tokenType().toString(), jackTokenizer.symbol(), ';')
        writer.writeEndTag(tag.toString())
    }

    private fun compileSubroutineDec() {
        val tag = Tag.SUBROUTINE_DEC
        writer.writeStartTag(tag.toString())
        writer.writeTag(jackTokenizer.tokenType().toString(), jackTokenizer.keyword())

        jackTokenizer.advance()
        typeHelper(returnTypeKeywords)

        jackTokenizer.advance()
        writer.writeTag(jackTokenizer.tokenType().toString(), jackTokenizer.identifier())

        jackTokenizer.advance()
        writer.writeTag(jackTokenizer.tokenType().toString(), jackTokenizer.symbol(), '(')

        jackTokenizer.advance()
        compileParameterList()

        jackTokenizer.advance()
        writer.writeTag(jackTokenizer.tokenType().toString(), jackTokenizer.symbol(), ')')

        jackTokenizer.advance()
        compileSubroutineBody()

        writer.writeEndTag(tag.toString())
    }

    private fun compileParameterList() {
        val tag = Tag.PARAMETER_LIST
        writer.writeStartTag(tag.toString())

        if (hasTypeToken()) {
            typeHelper(typeKeywords)

            jackTokenizer.advance()
            writer.writeTag(jackTokenizer.tokenType().toString(), jackTokenizer.identifier())

            jackTokenizer.advance()
            while (jackTokenizer.symbol() == ',') {
                writer.writeTag(jackTokenizer.tokenType().toString(), jackTokenizer.symbol(), ',')

                jackTokenizer.advance()
                typeHelper(typeKeywords)

                jackTokenizer.advance()
                writer.writeTag(jackTokenizer.tokenType().toString(), jackTokenizer.identifier())

                jackTokenizer.advance()
            }
            jackTokenizer.pushBack()

        } else {
            jackTokenizer.pushBack()
        }
        writer.writeEndTag(tag.toString())
    }

    private fun compileSubroutineBody() {
        val tag = Tag.SUBROUTINE_BODY
        writer.writeStartTag(tag.toString())
        writer.writeTag(jackTokenizer.tokenType().toString(), jackTokenizer.symbol(), '{')

        jackTokenizer.advance()
        while (jackTokenizer.tokenType() == TokenType.KEYWORD && Keyword.valueOf(jackTokenizer.keyword().toEnumFormat()) == Keyword.VAR) {
            compileVarDec()
            jackTokenizer.advance()
        }
        jackTokenizer.pushBack()

        jackTokenizer.advance()
        compileStatements()

        jackTokenizer.advance()
        writer.writeTag(jackTokenizer.tokenType().toString(), jackTokenizer.symbol(), '}')
        writer.writeEndTag(tag.toString())
    }

    private fun compileVarDec() {
        val tag = Tag.VAR_DEC
        writer.writeStartTag(tag.toString())
        writer.writeTag(jackTokenizer.tokenType().toString(), jackTokenizer.keyword())

        jackTokenizer.advance()
        typeHelper(typeKeywords)

        jackTokenizer.advance()
        writer.writeTag(jackTokenizer.tokenType().toString(), jackTokenizer.identifier())

        jackTokenizer.advance()
        while (jackTokenizer.symbol() == ',') {
            writer.writeTag(jackTokenizer.tokenType().toString(), jackTokenizer.symbol(), ',')
            jackTokenizer.advance()
            writer.writeTag(jackTokenizer.tokenType().toString(), jackTokenizer.identifier())

            jackTokenizer.advance()
        }
        jackTokenizer.pushBack()

        jackTokenizer.advance()
        writer.writeTag(jackTokenizer.tokenType().toString(), jackTokenizer.symbol(), ';')
        writer.writeEndTag(tag.toString())
    }

    private fun compileStatements() {
        val tag = Tag.STATEMENTS
        writer.writeStartTag(tag.toString())

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
        writer.writeEndTag(tag.toString())
    }

    private fun compileLet() {
        val tag = Tag.LET_STATEMENT
        writer.writeStartTag(tag.toString())
        writer.writeTag(jackTokenizer.tokenType().toString(), jackTokenizer.keyword())

        jackTokenizer.advance()
        writer.writeTag(jackTokenizer.tokenType().toString(), jackTokenizer.identifier())

        jackTokenizer.advance()
        if (jackTokenizer.tokenType() == TokenType.SYMBOL && jackTokenizer.symbol() == '[') {
            writer.writeTag(jackTokenizer.tokenType().toString(), jackTokenizer.symbol(), '[')
            jackTokenizer.advance()
            compileExpression()
            jackTokenizer.advance()
            writer.writeTag(jackTokenizer.tokenType().toString(), jackTokenizer.symbol(), ']')
        } else {
            jackTokenizer.pushBack()
        }

        jackTokenizer.advance()
        writer.writeTag(jackTokenizer.tokenType().toString(), jackTokenizer.symbol(), '=')

        jackTokenizer.advance()
        compileExpression()

        jackTokenizer.advance()
        writer.writeTag(jackTokenizer.tokenType().toString(), jackTokenizer.symbol(), ';')
        writer.writeEndTag(tag.toString())
    }

    private fun compileIf() {
        val tag = Tag.IF_STATEMENT
        writer.writeStartTag(tag.toString())
        writer.writeTag(jackTokenizer.tokenType().toString(), jackTokenizer.keyword())

        jackTokenizer.advance()
        writer.writeTag(jackTokenizer.tokenType().toString(), jackTokenizer.symbol(), '(')

        jackTokenizer.advance()
        compileExpression()

        jackTokenizer.advance()
        writer.writeTag(jackTokenizer.tokenType().toString(), jackTokenizer.symbol(), ')')

        jackTokenizer.advance()
        writer.writeTag(jackTokenizer.tokenType().toString(), jackTokenizer.symbol(), '{')

        jackTokenizer.advance()
        compileStatements()

        jackTokenizer.advance()
        writer.writeTag(jackTokenizer.tokenType().toString(), jackTokenizer.symbol(), '}')

        jackTokenizer.advance()
        if (jackTokenizer.tokenType() == TokenType.KEYWORD && Keyword.valueOf(jackTokenizer.keyword().toEnumFormat()) == Keyword.ELSE) {
            writer.writeTag(jackTokenizer.tokenType().toString(), jackTokenizer.keyword())

            jackTokenizer.advance()
            writer.writeTag(jackTokenizer.tokenType().toString(), jackTokenizer.symbol(), '{')

            jackTokenizer.advance()
            compileStatements()

            jackTokenizer.advance()
            writer.writeTag(jackTokenizer.tokenType().toString(), jackTokenizer.symbol(), '}')
        } else {
            jackTokenizer.pushBack()
        }

        writer.writeEndTag(tag.toString())
    }

    private fun compileWhile() {
        val tag = Tag.WHILE_STATEMENT
        writer.writeStartTag(tag.toString())
        writer.writeTag(jackTokenizer.tokenType().toString(), jackTokenizer.keyword())

        jackTokenizer.advance()
        writer.writeTag(jackTokenizer.tokenType().toString(), jackTokenizer.symbol(), '(')

        jackTokenizer.advance()
        compileExpression()

        jackTokenizer.advance()
        writer.writeTag(jackTokenizer.tokenType().toString(), jackTokenizer.symbol(), ')')

        jackTokenizer.advance()
        writer.writeTag(jackTokenizer.tokenType().toString(), jackTokenizer.symbol(), '{')

        jackTokenizer.advance()
        compileStatements()

        jackTokenizer.advance()
        writer.writeTag(jackTokenizer.tokenType().toString(), jackTokenizer.symbol(), '}')

        writer.writeEndTag(tag.toString())
    }

    private fun compileDo() {
        val tag = Tag.DO_STATEMENT
        writer.writeStartTag(tag.toString())
        writer.writeTag(jackTokenizer.tokenType().toString(), jackTokenizer.keyword())

//        compileSubroutineCall()
        jackTokenizer.advance()
        writer.writeTag(jackTokenizer.tokenType().toString(), jackTokenizer.identifier())
        jackTokenizer.advance()
        if (jackTokenizer.tokenType() == TokenType.SYMBOL && jackTokenizer.symbol() == '.') {
            writer.writeTag(jackTokenizer.tokenType().toString(), jackTokenizer.symbol(), '.')
            jackTokenizer.advance()
            writer.writeTag(jackTokenizer.tokenType().toString(), jackTokenizer.identifier())
        } else {
            jackTokenizer.pushBack()
        }
        jackTokenizer.advance()
        writer.writeTag(jackTokenizer.tokenType().toString(), jackTokenizer.symbol(), '(')
        jackTokenizer.advance()
        compileExpressionList()
        jackTokenizer.advance()
        writer.writeTag(jackTokenizer.tokenType().toString(), jackTokenizer.symbol(), ')')

        jackTokenizer.advance()
        writer.writeTag(jackTokenizer.tokenType().toString(), jackTokenizer.symbol(), ';')
        writer.writeEndTag(tag.toString())
    }

    private fun compileReturn() {
        val tag = Tag.RETURN_STATEMENT
        writer.writeStartTag(tag.toString())
        writer.writeTag(jackTokenizer.tokenType().toString(), jackTokenizer.keyword())

        jackTokenizer.advance()
        if (!(jackTokenizer.tokenType() == TokenType.SYMBOL && jackTokenizer.symbol() == ';')) {
            compileExpression()
        } else {
            jackTokenizer.pushBack()
        }

        jackTokenizer.advance()
        writer.writeTag(jackTokenizer.tokenType().toString(), jackTokenizer.symbol(), ';')
        writer.writeEndTag(tag.toString())
    }

    private fun compileExpression() {
        val tag = Tag.EXPRESSION
        writer.writeStartTag(tag.toString())
        compileTerm()

        jackTokenizer.advance()
        while (jackTokenizer.tokenType() == TokenType.SYMBOL && jackTokenizer.symbol() in operatorSymbols) {
            writer.writeTag(jackTokenizer.tokenType().toString(), jackTokenizer.symbol(), true)
            jackTokenizer.advance()
            compileTerm()
            jackTokenizer.advance()
        }
        jackTokenizer.pushBack()

        writer.writeEndTag(tag.toString())
    }

    private fun compileTerm() {
        val tag = Tag.TERM
        writer.writeStartTag(tag.toString())

        when (jackTokenizer.tokenType()) {
            TokenType.INTEGER_CONSTANT ->
                writer.writeTag(jackTokenizer.tokenType().toString(), jackTokenizer.intVal().toString())
            TokenType.STRING_CONSTANT ->
                writer.writeTag(jackTokenizer.tokenType().toString(), jackTokenizer.stringVal())
            TokenType.KEYWORD ->
                if (Keyword.valueOf(jackTokenizer.keyword().toEnumFormat()) in constantKeywords) {
                    writer.writeTag(jackTokenizer.tokenType().toString(), jackTokenizer.keyword())
                } else throw IllegalArgumentException()
            TokenType.IDENTIFIER -> {
                // TODO
                writer.writeTag(jackTokenizer.tokenType().toString(), jackTokenizer.identifier())
                jackTokenizer.advance()
                if (jackTokenizer.tokenType() == TokenType.SYMBOL) {
                    when (jackTokenizer.symbol()) {
                        '[' -> {
                            writer.writeTag(jackTokenizer.tokenType().toString(), jackTokenizer.symbol(), '[')
                            jackTokenizer.advance()
                            compileExpression()
                            jackTokenizer.advance()
                            writer.writeTag(jackTokenizer.tokenType().toString(), jackTokenizer.symbol(), ']')
                        }
                        '(' -> {
                            writer.writeTag(jackTokenizer.tokenType().toString(), jackTokenizer.symbol(), '(')
                            jackTokenizer.advance()
                            compileExpressionList()
                            jackTokenizer.advance()
                            writer.writeTag(jackTokenizer.tokenType().toString(), jackTokenizer.symbol(), ')')
                        }
                        '.' -> {
                            writer.writeTag(jackTokenizer.tokenType().toString(), jackTokenizer.symbol(), '.')
                            jackTokenizer.advance()
                            writer.writeTag(jackTokenizer.tokenType().toString(), jackTokenizer.identifier())
                            jackTokenizer.advance()
                            writer.writeTag(jackTokenizer.tokenType().toString(), jackTokenizer.symbol(), '(')
                            jackTokenizer.advance()
                            compileExpressionList()
                            jackTokenizer.advance()
                            writer.writeTag(jackTokenizer.tokenType().toString(), jackTokenizer.symbol(), ')')
                        }
                        else -> jackTokenizer.pushBack()
                    }
                } else {
                    jackTokenizer.pushBack()
                }
            }
            TokenType.SYMBOL ->
                when (jackTokenizer.symbol()) {
                    '(' -> {
                        writer.writeTag(jackTokenizer.tokenType().toString(), jackTokenizer.symbol(), '(')
                        jackTokenizer.advance()
                        compileExpression()
                        jackTokenizer.advance()
                        writer.writeTag(jackTokenizer.tokenType().toString(), jackTokenizer.symbol(), ')')
                    }
                    in unaryOperatorSymbols -> {
                        writer.writeTag(jackTokenizer.tokenType().toString(), jackTokenizer.symbol(), true)
                        jackTokenizer.advance()
                        compileTerm()
                    }
                    else -> throw IllegalArgumentException()
                }
        }

        writer.writeEndTag(tag.toString())
    }

    private fun compileExpressionList() {
        val tag = Tag.EXPRESSION_LIST
        writer.writeStartTag(tag.toString())
        if (!(jackTokenizer.tokenType() == TokenType.SYMBOL && jackTokenizer.symbol() == ')')) {
            compileExpression()

            jackTokenizer.advance()
            while (jackTokenizer.tokenType() == TokenType.SYMBOL && jackTokenizer.symbol() == ',') {
                writer.writeTag(jackTokenizer.tokenType().toString(), jackTokenizer.symbol(), ',')
                jackTokenizer.advance()
                compileExpression()
                jackTokenizer.advance()
            }
            jackTokenizer.pushBack()
        } else {
            jackTokenizer.pushBack()
        }
        writer.writeEndTag(tag.toString())
    }
}