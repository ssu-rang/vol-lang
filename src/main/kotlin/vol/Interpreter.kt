package vol

sealed interface Value
data class IntValue(val value: Long) : Value
data class StringValue(val value: String) : Value
data class BoolValue(val value: Boolean) : Value
data object UnitValue : Value
data object PrintValue : Value

data class RuntimeField(val typeName: String, val default: Value)
data class RuntimeSchema(val name: String, val fields: LinkedHashMap<String, RuntimeField>) : Value
data class FieldCell(val typeName: String, var value: Value)
data class RecordValue(
    val schema: RuntimeSchema,
    val fields: LinkedHashMap<String, FieldCell>,
) : Value {
    fun isNull(): Boolean = (fields["null"]?.value as? IntValue)?.value == 1L
}

data class FunctionValue(
    val parameters: List<Parameter>,
    val body: Program,
    val closure: Environment,
) : Value

data class IfValue(val condition: Expr, val body: Program, val closure: Environment) : Value
data class LoopValue(
    val start: Expr,
    val end: Expr,
    val step: Expr,
    val variable: String,
    val body: Program,
    val closure: Environment,
) : Value

internal data class Cell(val typeName: String, var value: Value, val mutable: Boolean = true)

class Environment(private val parent: Environment? = null) {
    private val bindings = mutableMapOf<String, Cell>()
    private val schemas = mutableMapOf<String, RuntimeSchema>()

    fun define(name: String, typeName: String, value: Value, mutable: Boolean = true) {
        if (bindings.containsKey(name)) throw IllegalStateException("duplicate runtime binding")
        bindings[name] = Cell(typeName, value, mutable)
    }

    fun get(name: String, location: SourceLocation): Value =
        bindings[name]?.value ?: parent?.get(name, location)
        ?: throw EvaluationException("unknown binding '$name'", location)

    internal fun cell(name: String, location: SourceLocation): Pair<Environment, Cell> {
        val local = bindings[name]
        if (local != null) return this to local
        return parent?.cell(name, location)
            ?: throw EvaluationException("unknown binding '$name'", location)
    }

    fun defineSchema(schema: RuntimeSchema) {
        schemas[schema.name] = schema
    }

    fun schema(name: String, location: SourceLocation): RuntimeSchema =
        schemas[name] ?: parent?.schema(name, location)
        ?: throw EvaluationException("unknown record type '$name'", location)
}

private object BreakSignal : RuntimeException(null, null, false, false)

class Interpreter(private val output: (String) -> Unit = ::println) {
    private val globals = Environment().also {
        it.define("print", "fn", PrintValue, mutable = false)
    }

    fun execute(program: Program): Value = executeProgram(program, globals)

    private fun executeProgram(program: Program, environment: Environment): Value {
        var result: Value = UnitValue
        for (statement in program.statements) result = executeStatement(statement, environment)
        return result
    }

    private fun executeStatement(statement: Stmt, environment: Environment): Value = when (statement) {
        is Declaration -> declare(statement, environment)
        is Assignment -> assign(statement, environment)
        is ExpressionStatement -> executeValue(evaluate(statement.expression, environment), statement.location)
        is BreakStatement -> throw BreakSignal
    }

    private fun declare(declaration: Declaration, environment: Environment): Value {
        if (declaration.typeName == "record") {
            return declareSchema(declaration, environment)
        }
        val value = if (declaration.initializer is RecordLiteral) {
            createRecord(declaration.typeName, declaration.initializer, environment)
        } else {
            evaluate(declaration.initializer, environment)
        }
        ensureType(declaration.typeName, value, environment, declaration.location)
        environment.define(declaration.name, declaration.typeName, value)
        return value
    }

    private fun declareSchema(declaration: Declaration, environment: Environment): Value {
        val literal = declaration.initializer as RecordLiteral
        val fields = linkedMapOf<String, RuntimeField>()
        for (field in literal.fields) {
            val default = evaluateInitializer(field.typeName, field.value, environment)
            ensureType(field.typeName, default, environment, field.location)
            validateNullField(field.name, default, field.location)
            fields[field.name] = RuntimeField(field.typeName, default)
        }
        val schema = RuntimeSchema(declaration.name, LinkedHashMap(fields))
        environment.defineSchema(schema)
        environment.define(declaration.name, "record", schema, mutable = false)
        return schema
    }

    private fun createRecord(typeName: String, literal: RecordLiteral, environment: Environment): RecordValue {
        val schema = environment.schema(typeName, literal.location)
        val fields = linkedMapOf<String, FieldCell>()
        for (field in literal.fields) {
            val value = evaluateInitializer(field.typeName, field.value, environment)
            ensureType(field.typeName, value, environment, field.location)
            validateNullField(field.name, value, field.location)
            fields[field.name] = FieldCell(field.typeName, value)
        }
        return RecordValue(schema, LinkedHashMap(fields))
    }

    private fun evaluateInitializer(typeName: String, expression: Expr, environment: Environment): Value =
        if (expression is RecordLiteral) createRecord(typeName, expression, environment)
        else evaluate(expression, environment)

    private fun evaluate(expression: Expr, environment: Environment): Value = when (expression) {
        is IntLiteral -> IntValue(expression.value)
        is StringLiteral -> StringValue(expression.value)
        is BoolLiteral -> BoolValue(expression.value)
        is Variable -> environment.get(expression.name, expression.location)
        is Unary -> unary(expression, environment)
        is Binary -> binary(expression, environment)
        is Call -> call(expression, environment)
        is FieldAccess -> {
            val record = evaluate(expression.receiver, environment) as? RecordValue
                ?: throw EvaluationException("field access requires a record", expression.location)
            record.fields[expression.field]?.value
                ?: throw EvaluationException("record has no field '${expression.field}'", expression.location)
        }
        is FunctionLiteral -> FunctionValue(expression.parameters, expression.body, environment)
        is IfLiteral -> IfValue(expression.condition, expression.body, environment)
        is LoopLiteral -> LoopValue(
            expression.start, expression.end, expression.step, expression.variable, expression.body, environment,
        )
        is RecordLiteral -> throw EvaluationException("record value has no declared type", expression.location)
    }

    private fun assign(assignment: Assignment, environment: Environment): Value {
        val value = evaluate(assignment.value, environment)
        when (val target = assignment.target) {
            is Variable -> {
                val (_, cell) = environment.cell(target.name, target.location)
                if (!cell.mutable) throw EvaluationException("binding '${target.name}' cannot be assigned", target.location)
                ensureType(cell.typeName, value, environment, assignment.location)
                cell.value = value
            }
            is FieldAccess -> {
                val record = evaluate(target.receiver, environment) as? RecordValue
                    ?: throw EvaluationException("field assignment requires a record", target.location)
                val field = record.fields[target.field]
                    ?: throw EvaluationException("record has no field '${target.field}'", target.location)
                ensureType(field.typeName, value, environment, assignment.location)
                validateNullField(target.field, value, target.location)
                field.value = value
            }
            else -> throw EvaluationException("invalid assignment target", assignment.location)
        }
        return value
    }

    private fun call(expression: Call, environment: Environment): Value {
        val callee = evaluate(expression.callee, environment)
        val arguments = expression.arguments.map { evaluate(it, environment) }
        return when (callee) {
            PrintValue -> {
                output(format(arguments.single()))
                UnitValue
            }
            is FunctionValue -> {
                if (arguments.size != callee.parameters.size) {
                    throw EvaluationException("wrong number of arguments", expression.location)
                }
                val callEnvironment = Environment(callee.closure)
                for ((parameter, value) in callee.parameters.zip(arguments)) {
                    ensureType(parameter.typeName, value, callee.closure, parameter.location)
                    callEnvironment.define(parameter.name, parameter.typeName, value)
                }
                executeProgram(callee.body, callEnvironment)
            }
            else -> throw EvaluationException("value is not callable", expression.location)
        }
    }

    private fun executeValue(value: Value, location: SourceLocation): Value = when (value) {
        is IfValue -> executeIf(value)
        is LoopValue -> executeLoop(value, location)
        else -> value
    }

    private fun executeIf(value: IfValue): Value {
        val condition = evaluate(value.condition, value.closure) as BoolValue
        if (!condition.value) return UnitValue
        executeProgram(value.body, Environment(value.closure))
        return UnitValue
    }

    private fun executeLoop(value: LoopValue, location: SourceLocation): Value {
        val start = (evaluate(value.start, value.closure) as IntValue).value
        val end = (evaluate(value.end, value.closure) as IntValue).value
        val step = (evaluate(value.step, value.closure) as IntValue).value
        if (step == 0L) throw EvaluationException("loop step cannot be zero", location)
        var current = start
        fun inRange(): Boolean = if (step > 0) current < end else current > end
        while (inRange()) {
            val iteration = Environment(value.closure)
            iteration.define(value.variable, "Int", IntValue(current))
            try {
                executeProgram(value.body, iteration)
            } catch (_: BreakSignal) {
                break
            }
            current = try {
                Math.addExact(current, step)
            } catch (_: ArithmeticException) {
                throw EvaluationException("loop counter overflow", location)
            }
        }
        return UnitValue
    }

    private fun unary(expression: Unary, environment: Environment): Value {
        val value = evaluate(expression.operand, environment)
        return when (expression.operator) {
            TokenType.MINUS -> {
                val number = (value as IntValue).value
                try {
                    IntValue(Math.negateExact(number))
                } catch (_: ArithmeticException) {
                    throw EvaluationException("integer overflow", expression.location)
                }
            }
            TokenType.BANG -> BoolValue(!(value as BoolValue).value)
            else -> throw EvaluationException("unsupported unary operator", expression.location)
        }
    }

    private fun binary(expression: Binary, environment: Environment): Value {
        val left = evaluate(expression.left, environment)
        if (expression.operator == TokenType.AND_AND && !(left as BoolValue).value) return BoolValue(false)
        if (expression.operator == TokenType.OR_OR && (left as BoolValue).value) return BoolValue(true)
        val right = evaluate(expression.right, environment)
        return when (expression.operator) {
            TokenType.PLUS -> when {
                left is IntValue && right is IntValue ->
                    checkedInteger(expression.location) { Math.addExact(left.value, right.value) }
                left is StringValue && right is StringValue -> StringValue(left.value + right.value)
                else -> throw EvaluationException("invalid '+' operands", expression.location)
            }
            TokenType.MINUS -> integerPair(left, right, expression.location) { a, b -> Math.subtractExact(a, b) }
            TokenType.STAR -> integerPair(left, right, expression.location) { a, b -> Math.multiplyExact(a, b) }
            TokenType.SLASH -> integerPair(left, right, expression.location) { a, b ->
                if (b == 0L) throw ArithmeticException()
                if (a == Long.MIN_VALUE && b == -1L) throw ArithmeticException()
                a / b
            }
            TokenType.PERCENT -> integerPair(left, right, expression.location) { a, b ->
                if (b == 0L) throw ArithmeticException()
                a % b
            }
            TokenType.LESS -> compare(left, right) { a, b -> a < b }
            TokenType.LESS_EQUAL -> compare(left, right) { a, b -> a <= b }
            TokenType.GREATER -> compare(left, right) { a, b -> a > b }
            TokenType.GREATER_EQUAL -> compare(left, right) { a, b -> a >= b }
            TokenType.EQUAL_EQUAL -> BoolValue(valuesEqual(left, right))
            TokenType.BANG_EQUAL -> BoolValue(!valuesEqual(left, right))
            TokenType.AND_AND -> BoolValue((right as BoolValue).value)
            TokenType.OR_OR -> BoolValue((right as BoolValue).value)
            else -> throw EvaluationException("unsupported binary operator", expression.location)
        }
    }

    private fun checkedInteger(location: SourceLocation, operation: () -> Long): IntValue =
        try {
            IntValue(operation())
        } catch (_: ArithmeticException) {
            throw EvaluationException("invalid integer arithmetic", location)
        }

    private fun integerPair(
        left: Value,
        right: Value,
        location: SourceLocation,
        operation: (Long, Long) -> Long,
    ): IntValue {
        val a = (left as IntValue).value
        val b = (right as IntValue).value
        return checkedInteger(location) { operation(a, b) }
    }

    private fun compare(left: Value, right: Value, operation: (Long, Long) -> Boolean): BoolValue =
        BoolValue(operation((left as IntValue).value, (right as IntValue).value))

    private fun valuesEqual(left: Value, right: Value): Boolean = when {
        left is IntValue && right is IntValue -> left.value == right.value
        left is StringValue && right is StringValue -> left.value == right.value
        left is BoolValue && right is BoolValue -> left.value == right.value
        left is RecordValue && right is RecordValue -> {
            if (left.schema.name != right.schema.name) false
            else if (left.isNull() || right.isNull()) left.isNull() && right.isNull()
            else left.fields.keys.all { name -> valuesEqual(left.fields.getValue(name).value, right.fields.getValue(name).value) }
        }
        left === UnitValue && right === UnitValue -> true
        else -> left === right
    }

    private fun ensureType(
        typeName: String,
        value: Value,
        environment: Environment,
        location: SourceLocation,
    ) {
        val matches = when (typeName) {
            "Int" -> value is IntValue
            "String" -> value is StringValue
            "Bool" -> value is BoolValue
            "fn" -> value is FunctionValue || value === PrintValue
            "if" -> value is IfValue
            "loop" -> value is LoopValue
            "record" -> value is RuntimeSchema
            else -> value is RecordValue && value.schema === environment.schema(typeName, location)
        }
        if (!matches) {
            throw EvaluationException("value does not match declared type '$typeName'", location)
        }
    }

    private fun validateNullField(name: String, value: Value, location: SourceLocation) {
        if (name != "null") return
        val number = (value as IntValue).value
        if (number != 0L && number != 1L) {
            throw EvaluationException("record null state must be 0 or 1", location)
        }
    }

    private fun format(value: Value): String = when (value) {
        is IntValue -> value.value.toString()
        is StringValue -> value.value
        is BoolValue -> value.value.toString()
        UnitValue -> ""
        PrintValue -> "<fn print>"
        is FunctionValue -> "<fn>"
        is IfValue -> "<if>"
        is LoopValue -> "<loop>"
        is RuntimeSchema -> "<record ${value.name}>"
        is RecordValue -> {
            if (value.isNull()) "${value.schema.name}(null)"
            else value.fields.entries.joinToString(
                prefix = "${value.schema.name}{",
                postfix = "}",
            ) { (name, cell) -> "$name: ${format(cell.value)}" }
        }
    }
}
