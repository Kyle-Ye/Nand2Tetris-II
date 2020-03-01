enum class TokenType {
    KEYWORD, SYMBOL, IDENTIFIER, INTEGER_CONSTANT, STRING_CONSTANT;

    override fun toString(): String {
        return super.toString().toLowerCase().replace(Regex("_.")) {
            it.value[1].toUpperCase().toString()
        }
    }
}

enum class Tag {
    CLASS, CLASS_VAR_DEC, SUBROUTINE_DEC, PARAMETER_LIST,
    SUBROUTINE_BODY, VAR_DEC, STATEMENTS, LET_STATEMENT,
    IF_STATEMENT, WHILE_STATEMENT, DO_STATEMENT, RETURN_STATEMENT,
    EXPRESSION, TERM, EXPRESSION_LIST,IDENTIFIER;

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

enum class Command{
    ADD,SUB,NEG,EQ,GT,LT,AND,OR,NOT
}

enum class Kind(){
    STATIC,FIELD,ARGUMENT,VAR;
    fun toSegment():Segment{
        return when(this){
            STATIC->Segment.STATIC
            FIELD-> Segment.THIS
            ARGUMENT->Segment.ARGUMENT
            VAR->Segment.LOCAL
        }
    }
}
enum class IdentifierType(){
    VAR,ARGUMENT,STATIC,FIELD,CLASS,SUBROUTINE;
}

enum class Segment(){
    CONSTANT,ARGUMENT,LOCAL,STATIC,THIS,THAT,POINTER,TEMP;
    override fun toString(): String {
        return super.toString().toLowerCase()
    }
}