package vol

sealed interface VolType { val displayName: String }

data object IntType : VolType { override val displayName = "Int" }
data object StringType : VolType { override val displayName = "String" }
data object BoolType : VolType { override val displayName = "Bool" }
data object AnyFunctionType : VolType { override val displayName = "fn" }
data class FunctionType(val parameters: List<VolType>, val result: VolType) : VolType { override val displayName = "fn" }
data object IfType : VolType { override val displayName = "if" }
data object LoopType : VolType { override val displayName = "loop" }
data object RecordMetaType : VolType { override val displayName = "record" }
data object UnitType : VolType { override val displayName = "Unit" }
data object PrintType : VolType { override val displayName = "fn" }

data class RecordSchema(val name: String, val fields: LinkedHashMap<String, VolType>)
data class RecordType(val schema: RecordSchema) : VolType { override val displayName = schema.name }

private class TypeScope(private val parent: TypeScope? = null) {
    private val bindings = mutableMapOf<String, VolType>()
    private val schemas = mutableMapOf<String, RecordSchema>()

    fun define(name: String, type: VolType, location: SourceLocation) {
        if (bindings.containsKey(name)) throw TypeException("binding '$name' is already declared", location)
        bindings[name] = type
    }

    fun find(name: String, location: SourceLocation): VolType =
        bindings[name] ?: parent?.find(name, location)
        ?: throw TypeException("unknown binding '$name'", location)

    fun defineSchema(schema: RecordSchema, location: SourceLocation) {
        if (schemas.containsKey(schema.name)) throw TypeException("type '${schema.name}' is already declared", location)
        schemas[schema.name] = schema
    }

    fun findSchema(name: String, location: SourceLocation): RecordSchema =
        schemas[name] ?: parent?.findSchema(name, location)
        ?: throw TypeException("unknown type '$name'", location)
}

class TypeChecker {
    private val globals = TypeScope().also { it.define("print", PrintType, SourceLocation(1, 1)) }
    private var loopDepth = 0

    fun check(program: Program) { checkProgram(program, globals) }

    private fun checkProgram(program: Program, scope: TypeScope): VolType {
        var last: VolType = UnitType
        for (statement in program.statements) last = checkStatement(statement, scope)
        return last
    }

    private fun checkStatement(statement: Stmt, scope: TypeScope): VolType = when (statement) {
        is Declaration -> checkDeclaration(statement, scope)
        is Assignment -> {
            val target = assignmentTargetType(statement.target, scope)
            val value = checkExpression(statement.value, scope, target)
            requireAssignable(target, value, statement.location)
            target
        }
        is ExpressionStatement -> {
            val type = checkExpression(statement.expression, scope)
            if (type == IfType || type == LoopType) UnitType else type
        }
        is BreakStatement -> {
            if (loopDepth == 0) throw TypeException("'break' is only valid inside a loop", statement.location)
            UnitType
        }
    }

    private fun checkDeclaration(declaration: Declaration, scope: TypeScope): VolType {
        if (declaration.typeName == "record") return checkSchemaDeclaration(declaration, scope)
        val expected = resolveType(declaration.typeName, scope, declaration.location)
        val actual = checkExpression(declaration.initializer, scope, expected)
        requireAssignable(expected, actual, declaration.location)
        val bindingType = if (expected == AnyFunctionType && actual is FunctionType) actual else expected
        scope.define(declaration.name, bindingType, declaration.location)
        return bindingType
    }

    private fun checkSchemaDeclaration(declaration: Declaration, scope: TypeScope): VolType {
        val literal = declaration.initializer as? RecordLiteral
            ?: throw TypeException("record declaration requires a record body", declaration.location)
        val fields = linkedMapOf<String, VolType>()
        for (field in literal.fields) {
            if (fields.containsKey(field.name)) {
                throw TypeException("field '${field.name}' is declared more than once", field.location)
            }
            val type = resolveType(field.typeName, scope, field.location)
            val valueType = checkExpression(field.value, scope, type)
            requireAssignable(type, valueType, field.location)
            fields[field.name] = type
        }
        if (fields["null"] != IntType) {
            throw TypeException("every record must declare 'null: Int'", declaration.location)
        }
        val schema = RecordSchema(declaration.name, LinkedHashMap(fields))
        scope.defineSchema(schema, declaration.location)
        scope.define(declaration.name, RecordMetaType, declaration.location)
        return RecordMetaType
    }

    private fun resolveType(name: String, scope: TypeScope, location: SourceLocation): VolType = when (name) {
        "Int" -> IntType
        "String" -> StringType
        "Bool" -> BoolType
        "fn" -> AnyFunctionType
        "if" -> IfType
        "loop" -> LoopType
        "record" -> RecordMetaType
        else -> RecordType(scope.findSchema(name, location))
    }

    private fun checkExpression(expression: Expr, scope: TypeScope, expected: VolType? = null): VolType =
        when (expression) {
            is IntLiteral -> IntType
            is StringLiteral -> StringType
            is BoolLiteral -> BoolType
            is Variable -> scope.find(expression.name, expression.location)
            is Unary -> checkUnary(expression, scope)
            is Binary -> checkBinary(expression, scope)
            is Call -> checkCall(expression, scope)
            is FieldAccess -> fieldType(expression, scope)
            is FunctionLiteral -> checkFunction(expression, scope)
            is IfLiteral -> checkIf(expression, scope)
            is LoopLiteral -> checkLoop(expression, scope)
            is RecordLiteral -> {
                val record = expected as? RecordType
                    ?: throw TypeException("record value requires a named record type", expression.location)
                checkRecordValue(expression, record, scope)
                record
            }
        }

    private fun checkFunction(expression: FunctionLiteral, scope: TypeScope): VolType {
        val functionScope = TypeScope(scope)
        val parameters = mutableListOf<VolType>()
        for (parameter in expression.parameters) {
            val type = resolveType(parameter.typeName, scope, parameter.location)
            functionScope.define(parameter.name, type, parameter.location)
            parameters += type
        }
        val result = checkProgram(expression.body, functionScope)
        return FunctionType(parameters, result)
    }

    private fun checkIf(expression: IfLiteral, scope: TypeScope): VolType {
        requireType(BoolType, checkExpression(expression.condition, scope), expression.condition.location)
        checkProgram(expression.body, TypeScope(scope))
        return IfType
    }

    private fun checkLoop(expression: LoopLiteral, scope: TypeScope): VolType {
        requireType(IntType, checkExpression(expression.start, scope), expression.start.location)
        requireType(IntType, checkExpression(expression.end, scope), expression.end.location)
        requireType(IntType, checkExpression(expression.step, scope), expression.step.location)
        val loopScope = TypeScope(scope)
        loopScope.define(expression.variable, IntType, expression.location)
        loopDepth++
        try {
            checkProgram(expression.body, loopScope)
        } finally {
            loopDepth--
        }
        return LoopType
    }

    private fun checkRecordValue(expression: RecordLiteral, type: RecordType, scope: TypeScope) {
        val seen = mutableSetOf<String>()
        for (field in expression.fields) {
            if (!seen.add(field.name)) {
                throw TypeException("field '${field.name}' is assigned more than once", field.location)
            }
            val expected = type.schema.fields[field.name]
                ?: throw TypeException("record '${type.displayName}' has no field '${field.name}'", field.location)
            val annotated = resolveType(field.typeName, scope, field.location)
            requireType(expected, annotated, field.location)
            val actual = checkExpression(field.value, scope, expected)
            requireAssignable(expected, actual, field.location)
        }
        val missing = type.schema.fields.keys - seen
        if (missing.isNotEmpty()) {
            throw TypeException("record '${type.displayName}' is missing field '${missing.first()}'", expression.location)
        }
    }

    private fun fieldType(expression: FieldAccess, scope: TypeScope): VolType {
        val receiver = checkExpression(expression.receiver, scope)
        val record = receiver as? RecordType
            ?: throw TypeException("field access requires a record", expression.location)
        return record.schema.fields[expression.field]
            ?: throw TypeException("record '${record.displayName}' has no field '${expression.field}'", expression.location)
    }

    private fun assignmentTargetType(expression: Expr, scope: TypeScope): VolType = when (expression) {
        is Variable -> {
            if (expression.name == "print") throw TypeException("built-in 'print' cannot be assigned", expression.location)
            scope.find(expression.name, expression.location)
        }
        is FieldAccess -> fieldType(expression, scope)
        else -> throw TypeException("invalid assignment target", expression.location)
    }

    private fun checkCall(expression: Call, scope: TypeScope): VolType =
        when (val callee = checkExpression(expression.callee, scope)) {
            PrintType -> {
                if (expression.arguments.size != 1) {
                    throw TypeException("print expects exactly one argument", expression.location)
                }
                checkExpression(expression.arguments.single(), scope)
                UnitType
            }
            is FunctionType -> {
                if (callee.parameters.size != expression.arguments.size) {
                    throw TypeException("function expects ${callee.parameters.size} arguments", expression.location)
                }
                for ((argument, expected) in expression.arguments.zip(callee.parameters)) {
                    val actual = checkExpression(argument, scope, expected)
                    requireAssignable(expected, actual, argument.location)
                }
                callee.result
            }
            AnyFunctionType -> {
                expression.arguments.forEach { checkExpression(it, scope) }
                UnitType
            }
            else -> throw TypeException("value of type '${callee.displayName}' is not callable", expression.location)
        }

    private fun checkUnary(expression: Unary, scope: TypeScope): VolType {
        val operand = checkExpression(expression.operand, scope)
        return when (expression.operator) {
            TokenType.MINUS -> {
                requireType(IntType, operand, expression.location)
                IntType
            }
            TokenType.BANG -> {
                requireType(BoolType, operand, expression.location)
                BoolType
            }
            else -> throw TypeException("unsupported unary operator", expression.location)
        }
    }

    private fun checkBinary(expression: Binary, scope: TypeScope): VolType {
        val left = checkExpression(expression.left, scope)
        val right = checkExpression(expression.right, scope)
        return when (expression.operator) {
            TokenType.PLUS -> {
                if (left == IntType && right == IntType) IntType
                else if (left == StringType && right == StringType) StringType
                else throw TypeException("'+' requires two Int or two String values", expression.location)
            }
            TokenType.MINUS, TokenType.STAR, TokenType.SLASH, TokenType.PERCENT -> {
                requireType(IntType, left, expression.left.location)
                requireType(IntType, right, expression.right.location)
                IntType
            }
            TokenType.LESS, TokenType.LESS_EQUAL, TokenType.GREATER, TokenType.GREATER_EQUAL -> {
                requireType(IntType, left, expression.left.location)
                requireType(IntType, right, expression.right.location)
                BoolType
            }
            TokenType.EQUAL_EQUAL, TokenType.BANG_EQUAL -> {
                requireType(left, right, expression.location)
                BoolType
            }
            TokenType.AND_AND, TokenType.OR_OR -> {
                requireType(BoolType, left, expression.left.location)
                requireType(BoolType, right, expression.right.location)
                BoolType
            }
            else -> throw TypeException("unsupported binary operator", expression.location)
        }
    }

    private fun requireAssignable(expected: VolType, actual: VolType, location: SourceLocation) {
        if (expected == AnyFunctionType && (actual == AnyFunctionType || actual is FunctionType)) return
        requireType(expected, actual, location)
    }

    private fun requireType(expected: VolType, actual: VolType, location: SourceLocation) {
        if (expected != actual) {
            throw TypeException("expected '${expected.displayName}' but got '${actual.displayName}'", location)
        }
    }
}
