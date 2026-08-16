package vol

class Parser(private val tokens: List<Token>) {
    private var current = 0

    fun parse(): Program {
        val location = peek().location
        val statements = mutableListOf<Stmt>()
        while (!isAtEnd()) statements += statement()
        return Program(statements, location)
    }

    private fun statement(): Stmt {
        if (match(TokenType.BREAK)) return BreakStatement(previous().location)
        if (check(TokenType.IDENTIFIER) && checkNext(TokenType.COLON)) return declaration()

        val expression = expression()
        if (match(TokenType.EQUAL)) {
            if (expression !is Variable && expression !is FieldAccess) {
                throw error(previous(), "assignment target must be a binding or field")
            }
            return Assignment(expression, expression(), expression.location)
        }
        return ExpressionStatement(expression, expression.location)
    }

    private fun declaration(): Declaration {
        val name = consume(TokenType.IDENTIFIER, "expected binding name")
        consume(TokenType.COLON, "expected ':' after binding name")
        val type = consume(TokenType.IDENTIFIER, "expected type name")
        consume(TokenType.EQUAL, "expected '=' before initial value")
        return Declaration(name.lexeme, type.lexeme, initializer(type.lexeme), name.location)
    }

    private fun initializer(typeName: String): Expr = when {
        typeName == "record" -> recordLiteral()
        typeName == "fn" && check(TokenType.LEFT_PAREN) -> functionLiteral()
        typeName == "if" && check(TokenType.LEFT_PAREN) -> ifLiteral()
        typeName == "loop" && check(TokenType.LEFT_PAREN) -> loopLiteral()
        check(TokenType.LEFT_BRACE) -> recordLiteral()
        else -> expression()
    }

    private fun recordLiteral(): RecordLiteral {
        val opening = consume(TokenType.LEFT_BRACE, "expected '{'")
        val fields = mutableListOf<FieldInitializer>()
        while (!check(TokenType.RIGHT_BRACE) && !isAtEnd()) {
            val name = consume(TokenType.IDENTIFIER, "expected field name")
            consume(TokenType.COLON, "expected ':' after field name")
            val type = consume(TokenType.IDENTIFIER, "expected field type")
            consume(TokenType.EQUAL, "expected '=' before field value")
            fields += FieldInitializer(name.lexeme, type.lexeme, initializer(type.lexeme), name.location)
        }
        consume(TokenType.RIGHT_BRACE, "expected '}' after record fields")
        return RecordLiteral(fields, opening.location)
    }

    private fun functionLiteral(): FunctionLiteral {
        val opening = consume(TokenType.LEFT_PAREN, "expected '('")
        val parameters = mutableListOf<Parameter>()
        if (!check(TokenType.RIGHT_PAREN)) {
            do {
                val name = consume(TokenType.IDENTIFIER, "expected parameter name")
                consume(TokenType.COLON, "expected ':' after parameter name")
                val type = consume(TokenType.IDENTIFIER, "expected parameter type")
                parameters += Parameter(name.lexeme, type.lexeme, name.location)
            } while (match(TokenType.COMMA))
        }
        consume(TokenType.RIGHT_PAREN, "expected ')' after parameters")
        return FunctionLiteral(parameters, block(), opening.location)
    }

    private fun ifLiteral(): IfLiteral {
        val opening = consume(TokenType.LEFT_PAREN, "expected '('")
        val condition = expression()
        consume(TokenType.RIGHT_PAREN, "expected ')' after condition")
        return IfLiteral(condition, block(), opening.location)
    }

    private fun loopLiteral(): LoopLiteral {
        val opening = consume(TokenType.LEFT_PAREN, "expected '('")
        val start = expression()
        consume(TokenType.COMMA, "expected ',' after loop start")
        val end = expression()
        consume(TokenType.COMMA, "expected ',' after loop end")
        val step = expression()
        consume(TokenType.RIGHT_PAREN, "expected ')' after loop range")
        consume(TokenType.LEFT_PAREN, "expected '(' before loop binding")
        val variable = consume(TokenType.IDENTIFIER, "expected loop binding")
        consume(TokenType.RIGHT_PAREN, "expected ')' after loop binding")
        return LoopLiteral(start, end, step, variable.lexeme, block(), opening.location)
    }

    private fun block(): Program {
        val opening = consume(TokenType.LEFT_BRACE, "expected '{' before body")
        val statements = mutableListOf<Stmt>()
        while (!check(TokenType.RIGHT_BRACE) && !isAtEnd()) statements += statement()
        consume(TokenType.RIGHT_BRACE, "expected '}' after body")
        return Program(statements, opening.location)
    }

    private fun expression(): Expr = or()

    private fun or(): Expr {
        var expression = and()
        while (match(TokenType.OR_OR)) {
            val operator = previous()
            expression = Binary(expression, operator.type, and(), operator.location)
        }
        return expression
    }

    private fun and(): Expr {
        var expression = equality()
        while (match(TokenType.AND_AND)) {
            val operator = previous()
            expression = Binary(expression, operator.type, equality(), operator.location)
        }
        return expression
    }

    private fun equality(): Expr {
        var expression = comparison()
        while (match(TokenType.EQUAL_EQUAL, TokenType.BANG_EQUAL)) {
            val operator = previous()
            expression = Binary(expression, operator.type, comparison(), operator.location)
        }
        return expression
    }

    private fun comparison(): Expr {
        var expression = term()
        while (
            match(
                TokenType.LESS,
                TokenType.LESS_EQUAL,
                TokenType.GREATER,
                TokenType.GREATER_EQUAL,
            )
        ) {
            val operator = previous()
            expression = Binary(expression, operator.type, term(), operator.location)
        }
        return expression
    }

    private fun term(): Expr {
        var expression = factor()
        while (match(TokenType.PLUS, TokenType.MINUS)) {
            val operator = previous()
            expression = Binary(expression, operator.type, factor(), operator.location)
        }
        return expression
    }

    private fun factor(): Expr {
        var expression = unary()
        while (match(TokenType.STAR, TokenType.SLASH, TokenType.PERCENT)) {
            val operator = previous()
            expression = Binary(expression, operator.type, unary(), operator.location)
        }
        return expression
    }

    private fun unary(): Expr {
        if (match(TokenType.BANG, TokenType.MINUS)) {
            val operator = previous()
            return Unary(operator.type, unary(), operator.location)
        }
        return call()
    }

    private fun call(): Expr {
        var expression = primary()
        while (true) {
            expression = when {
                match(TokenType.LEFT_PAREN) -> finishCall(expression)
                match(TokenType.DOT) -> {
                    val field = consume(TokenType.IDENTIFIER, "expected field name after '.'")
                    FieldAccess(expression, field.lexeme, field.location)
                }
                else -> return expression
            }
        }
    }

    private fun finishCall(callee: Expr): Expr {
        val opening = previous()
        val arguments = mutableListOf<Expr>()
        if (!check(TokenType.RIGHT_PAREN)) {
            do {
                arguments += expression()
            } while (match(TokenType.COMMA))
        }
        consume(TokenType.RIGHT_PAREN, "expected ')' after arguments")
        return Call(callee, arguments, opening.location)
    }

    private fun primary(): Expr {
        if (match(TokenType.INTEGER)) return IntLiteral(previous().literal as Long, previous().location)
        if (match(TokenType.STRING)) return StringLiteral(previous().literal as String, previous().location)
        if (match(TokenType.TRUE)) return BoolLiteral(true, previous().location)
        if (match(TokenType.FALSE)) return BoolLiteral(false, previous().location)
        if (match(TokenType.IDENTIFIER)) return Variable(previous().lexeme, previous().location)
        if (match(TokenType.LEFT_PAREN)) {
            val expression = expression()
            consume(TokenType.RIGHT_PAREN, "expected ')' after expression")
            return expression
        }
        throw error(peek(), "expected expression")
    }

    private fun match(vararg types: TokenType): Boolean {
        for (type in types) {
            if (!check(type)) continue
            advance()
            return true
        }
        return false
    }

    private fun consume(type: TokenType, message: String): Token {
        if (check(type)) return advance()
        throw error(peek(), message)
    }

    private fun check(type: TokenType): Boolean = peek().type == type

    private fun checkNext(type: TokenType): Boolean =
        current + 1 < tokens.size && tokens[current + 1].type == type

    private fun advance(): Token {
        if (!isAtEnd()) current++
        return previous()
    }

    private fun isAtEnd(): Boolean = peek().type == TokenType.EOF
    private fun peek(): Token = tokens[current]
    private fun previous(): Token = tokens[current - 1]
    private fun error(token: Token, message: String): ParseException = ParseException(message, token.location)
}
