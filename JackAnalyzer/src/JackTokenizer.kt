import TokenType.*
import java.io.File
import java.io.StreamTokenizer
import java.io.StreamTokenizer.*

enum class TokenType {
    KEYWORD, SYMBOL, IDENTIFIER, INTEGER_CONSTANT, STRING_CONSTANT;

    override fun toString(): String {
        return super.toString().toLowerCase().replace(Regex("_.")) {
            it.value[1].toUpperCase().toString()
        }
    }
}

class JackTokenizer(input: File) {
    companion object {
        private val keywordSet = setOf(
            "class", "constructor", "function", "method",
            "field", "static", "var", "int",
            "char", "boolean", "void", "true",
            "false", "null", "this", "let",
            "do", "if", "else", "while",
            "return"
        )
    }

    private val streamTokenizer = StreamTokenizer(input.reader())
    private lateinit var token: String
    private lateinit var tokenType: TokenType

    init {
        streamTokenizer.wordChars('A'.toInt(), 'Z'.toInt())
        streamTokenizer.wordChars('a'.toInt(), 'z'.toInt())
        streamTokenizer.wordChars('_'.toInt(), '_'.toInt())

        streamTokenizer.ordinaryChar('{'.toInt())
        streamTokenizer.ordinaryChar('}'.toInt())
        streamTokenizer.ordinaryChar('('.toInt())
        streamTokenizer.ordinaryChar(')'.toInt())
        streamTokenizer.ordinaryChar('['.toInt())
        streamTokenizer.ordinaryChar(']'.toInt())
        streamTokenizer.ordinaryChar('.'.toInt())
        streamTokenizer.ordinaryChar(','.toInt())
        streamTokenizer.ordinaryChar(';'.toInt())
        streamTokenizer.ordinaryChar('+'.toInt())
        streamTokenizer.ordinaryChar('-'.toInt())
        streamTokenizer.ordinaryChar('*'.toInt())
        streamTokenizer.ordinaryChar('/'.toInt())
        streamTokenizer.ordinaryChar('&'.toInt())
        streamTokenizer.ordinaryChar('|'.toInt())
        streamTokenizer.ordinaryChar('<'.toInt())
        streamTokenizer.ordinaryChar('>'.toInt())
        streamTokenizer.ordinaryChar('='.toInt())
        streamTokenizer.ordinaryChar('~'.toInt())

        streamTokenizer.eolIsSignificant(false)
        streamTokenizer.slashStarComments(true)
        streamTokenizer.slashSlashComments(true)
        streamTokenizer.lowerCaseMode(false)
    }

    fun hasMoreTokens(): Boolean {
        return if (streamTokenizer.nextToken() != TT_EOF) {
            streamTokenizer.pushBack()
            true
        } else {
            false
        }
    }

    fun advance() {
        when (streamTokenizer.nextToken()) {
            TT_NUMBER -> {
                token = streamTokenizer.nval.toInt().toString()
                tokenType = INTEGER_CONSTANT
            }
            TT_WORD -> {
                token = streamTokenizer.sval
                tokenType = if (token in keywordSet) {
                    KEYWORD
                } else {
                    IDENTIFIER
                }
            }
            '"'.toInt() -> {
                token = streamTokenizer.sval
                tokenType = STRING_CONSTANT
            }
            else -> {
                token = streamTokenizer.ttype.toChar().toString()
                tokenType = SYMBOL
            }
        }
    }

    fun pushBack() {
        streamTokenizer.pushBack()
    }

    fun tokenType(): TokenType {
        return tokenType
    }

    fun keyword(): String {
        return if (tokenType == KEYWORD) token else throw IllegalCallerException()
    }

    fun symbol(): Char {
        return if (tokenType == SYMBOL) token[0] else throw IllegalCallerException()
    }

    fun identifier(): String {
        return if (tokenType == IDENTIFIER) token else throw IllegalCallerException()
    }

    fun intVal(): Int {
        return if (tokenType == INTEGER_CONSTANT) token.toInt() else throw IllegalCallerException()
    }

    fun stringVal(): String {
        return if (tokenType == STRING_CONSTANT) token else throw IllegalCallerException()
    }
}

fun main() {
    val input = File("test.jack")
    val jackTokenizer = JackTokenizer(input)
    val output = File("test.xml")
    output.writeText("")
    val writer = XmlWriter(output)

    writer.writeStartTag()
    while (jackTokenizer.hasMoreTokens()) {
        jackTokenizer.advance()
        when (val tokenType = jackTokenizer.tokenType()) {
            KEYWORD -> writer.writeTag(tokenType.toString(), jackTokenizer.keyword())
            SYMBOL -> writer.writeTag(tokenType.toString(), jackTokenizer.symbol(), true)
            IDENTIFIER -> writer.writeTag(tokenType.toString(), jackTokenizer.identifier())
            INTEGER_CONSTANT -> writer.writeTag(tokenType.toString(), jackTokenizer.intVal().toString())
            STRING_CONSTANT -> writer.writeTag(tokenType.toString(), jackTokenizer.stringVal())
        }
    }
    writer.writeEndTag()
}