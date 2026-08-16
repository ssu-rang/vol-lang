package vol

enum class TokenType {
    IDENTIFIER, INTEGER, STRING, TRUE, FALSE, BREAK,
    COLON, EQUAL, LEFT_BRACE, RIGHT_BRACE, LEFT_PAREN, RIGHT_PAREN,
    COMMA, DOT, PLUS, MINUS, STAR, SLASH, PERCENT, BANG,
    EQUAL_EQUAL, BANG_EQUAL, LESS, LESS_EQUAL, GREATER, GREATER_EQUAL,
    AND_AND, OR_OR, EOF,
}

data class Token(
    val type: TokenType,
    val lexeme: String,
    val literal: Any?,
    val location: SourceLocation,
)

class Lexer(private val source: String) {
    private val tokens = mutableListOf<Token>()
    private var start = 0
    private var current = 0
    private var line = 1
    private var column = 1
    private var tokenLine = 1
    private var tokenColumn = 1

    fun scanTokens(): List<Token> {
        while (!isAtEnd()) {
            start = current
            tokenLine = line
            tokenColumn = column
            scanToken()
        }
        tokens += Token(TokenType.EOF, "", null, SourceLocation(line, column))
        return tokens
    }

    private fun scanToken() {
        when (val char = advance()) {
            ' ', '\r', '\t', '\n' -> Unit
            ':' -> add(TokenType.COLON)
            '=' -> add(if (match('=')) TokenType.EQUAL_EQUAL else TokenType.EQUAL)
            '{' -> add(TokenType.LEFT_BRACE)
            '}' -> add(TokenType.RIGHT_BRACE)
            '(' -> add(TokenType.LEFT_PAREN)
            ')' -> add(TokenType.RIGHT_PAREN)
            ',' -> add(TokenType.COMMA)
            '.' -> add(TokenType.DOT)
            '+' -> add(TokenType.PLUS)
            '-' -> add(TokenType.MINUS)
            '*' -> add(TokenType.STAR)
            '%' -> add(TokenType.PERCENT)
            '!' -> add(if (match('=')) TokenType.BANG_EQUAL else TokenType.BANG)
            '<' -> add(if (match('=')) TokenType.LESS_EQUAL else TokenType.LESS)
            '>' -> add(if (match('=')) TokenType.GREATER_EQUAL else TokenType.GREATER)
            '/' -> if (match('/')) skipLineComment() else add(TokenType.SLASH)
            '#' -> skipLineComment()
            '"' -> string()
            else -> when {
                char.code == 38 -> logicalOperator(38, TokenType.AND_AND)
                char.code == 124 -> logicalOperator(124, TokenType.OR_OR)
                char.isDigit() -> number()
                char == '_' -> identifier()
                char.isLetter() -> identifier()
                else -> fail("unexpected character '$char'")
            }
        }
    }

    private fun logicalOperator(code: Int, type: TokenType) {
        if (match(code.toChar())) add(type) else fail("incomplete logical operator")
    }

    private fun skipLineComment() {
        while (!isAtEnd() && peek() != '\n') advance()
    }

    private fun number() {
        while (peek().isDigit()) advance()
        val text = source.substring(start, current)
        val value = text.toLongOrNull() ?: fail("integer is outside the supported range")
        add(TokenType.INTEGER, value)
    }

    private fun identifier() {
        while (peek() == '_' || peek().isLetterOrDigit()) advance()
        val text = source.substring(start, current)
        val type = when (text) {
            "true" -> TokenType.TRUE
            "false" -> TokenType.FALSE
            "break" -> TokenType.BREAK
            else -> TokenType.IDENTIFIER
        }
        add(type)
    }

    private fun string() {
        val value = StringBuilder()
        while (!isAtEnd() && peek() != '"') {
            val char = advance()
            if (char != '\\') {
                value.append(char)
                continue
            }
            if (isAtEnd()) fail("unterminated escape sequence")
            value.append(
                when (val escaped = advance()) {
                    'n' -> '\n'
                    'r' -> '\r'
                    't' -> '\t'
                    '"' -> '"'
                    '\\' -> '\\'
                    else -> fail("unsupported escape sequence \\$escaped")
                },
            )
        }
        if (isAtEnd()) fail("unterminated string")
        advance()
        add(TokenType.STRING, value.toString())
    }

    private fun advance(): Char {
        val char = source[current++]
        if (char == '\n') {
            line++
            column = 1
        } else {
            column++
        }
        return char
    }

    private fun match(expected: Char): Boolean {
        if (isAtEnd() || source[current] != expected) return false
        advance()
        return true
    }

    private fun peek(): Char = if (isAtEnd()) '\u0000' else source[current]
    private fun isAtEnd(): Boolean = current >= source.length

    private fun add(type: TokenType, literal: Any? = null) {
        tokens += Token(type, source.substring(start, current), literal, SourceLocation(tokenLine, tokenColumn))
    }

    private fun fail(message: String): Nothing {
        throw LexException(message, SourceLocation(tokenLine, tokenColumn))
    }
}
