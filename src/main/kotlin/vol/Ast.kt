package vol

data class SourceLocation(val line: Int, val column: Int) {
    override fun toString(): String = "$line:$column"
}

sealed interface Node {
    val location: SourceLocation
}

data class Program(val statements: List<Stmt>, override val location: SourceLocation) : Node

sealed interface Stmt : Node

data class Declaration(
    val name: String,
    val typeName: String,
    val initializer: Expr,
    override val location: SourceLocation,
) : Stmt

data class Assignment(val target: Expr, val value: Expr, override val location: SourceLocation) : Stmt
data class ExpressionStatement(val expression: Expr, override val location: SourceLocation) : Stmt
data class BreakStatement(override val location: SourceLocation) : Stmt

sealed interface Expr : Node

data class IntLiteral(val value: Long, override val location: SourceLocation) : Expr
data class StringLiteral(val value: String, override val location: SourceLocation) : Expr
data class BoolLiteral(val value: Boolean, override val location: SourceLocation) : Expr
data class Variable(val name: String, override val location: SourceLocation) : Expr
data class Unary(val operator: TokenType, val operand: Expr, override val location: SourceLocation) : Expr
data class Binary(val left: Expr, val operator: TokenType, val right: Expr, override val location: SourceLocation) : Expr
data class Call(val callee: Expr, val arguments: List<Expr>, override val location: SourceLocation) : Expr
data class FieldAccess(val receiver: Expr, val field: String, override val location: SourceLocation) : Expr

data class FieldInitializer(
    val name: String,
    val typeName: String,
    val value: Expr,
    override val location: SourceLocation,
) : Node

data class RecordLiteral(val fields: List<FieldInitializer>, override val location: SourceLocation) : Expr
data class Parameter(val name: String, val typeName: String, override val location: SourceLocation) : Node
data class FunctionLiteral(val parameters: List<Parameter>, val body: Program, override val location: SourceLocation) : Expr
data class IfLiteral(val condition: Expr, val body: Program, override val location: SourceLocation) : Expr
data class LoopLiteral(
    val start: Expr,
    val end: Expr,
    val step: Expr,
    val variable: String,
    val body: Program,
    override val location: SourceLocation,
) : Expr

open class VolException(message: String) : RuntimeException(message)
class LexException(message: String, location: SourceLocation) : VolException("${location}: $message")
class ParseException(message: String, location: SourceLocation) : VolException("${location}: $message")
class TypeException(message: String, location: SourceLocation) : VolException("${location}: $message")
class EvaluationException(message: String, location: SourceLocation) : VolException("${location}: $message")
