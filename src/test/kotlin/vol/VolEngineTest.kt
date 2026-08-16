package vol

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class VolEngineTest {
    private fun execute(source: String): List<String> {
        val lines = mutableListOf<String>()
        VolEngine(lines::add).run(source.trimIndent())
        return lines
    }

    @Test
    fun runsTheDocumentedProgram() {
        val output = execute(
            """
            User: record = {
                name: String = ""
                age: Int = 0
                null: Int = 0
            }
            user: User = {
                name: String = "Rang"
                age: Int = 20
                null: Int = 0
            }
            greet: fn = (user: User) {
                print("Hello, " + user.name)
            }
            adult: if = (user.age >= 20) {
                print("adult")
            }
            counter: loop = (0, 3, 1) (i) {
                print(i)
            }
            greet(user)
            adult
            counter
            """,
        )
        assertEquals(listOf("Hello, Rang", "adult", "0", "1", "2"), output)
    }

    @Test
    fun executableValuesAreLazyAndCanBeAliased() {
        val output = execute(
            """
            age: Int = 19
            adult: if = (age >= 20) { print("adult") }
            check: if = adult
            age = 20
            check
            """,
        )
        assertEquals(listOf("adult"), output)
    }

    @Test
    fun functionsReturnTheirLastValue() {
        val output = execute(
            """
            add: fn = (a: Int, b: Int) { a + b }
            result: Int = add(10, 20)
            print(result)
            """,
        )
        assertEquals(listOf("30"), output)
    }

    @Test
    fun breakStopsOnlyTheCurrentLoop() {
        val output = execute(
            """
            counter: loop = (0, 10, 1) (i) {
                stop: if = (i == 3) { break }
                stop
                print(i)
            }
            counter
            print("done")
            """,
        )
        assertEquals(listOf("0", "1", "2", "done"), output)
    }

    @Test
    fun supportsDescendingLoops() {
        val output = execute(
            """
            countdown: loop = (3, 0, -1) (i) { print(i) }
            countdown
            """,
        )
        assertEquals(listOf("3", "2", "1"), output)
    }

    @Test
    fun nullStateTakesPriorityWhenARecordIsPrinted() {
        val output = execute(
            """
            User: record = {
                name: String = ""
                null: Int = 0
            }
            user: User = {
                name: String = "Kim"
                null: Int = 0
            }
            user.null = 1
            user.name = "Changed"
            print(user)
            """,
        )
        assertEquals(listOf("User(null)"), output)
    }

    @Test
    fun typeErrorsHappenBeforeExecution() {
        val output = mutableListOf<String>()
        val error = assertFailsWith<TypeException> {
            VolEngine(output::add).run(
                """
                print("must not run")
                age: Int = "wrong"
                """.trimIndent(),
            )
        }
        assertTrue(error.message!!.contains("expected 'Int'"))
        assertTrue(output.isEmpty())
    }

    @Test
    fun nullStateRejectsValuesOtherThanZeroAndOne() {
        assertFailsWith<EvaluationException> {
            execute(
                """
                User: record = { null: Int = 0 }
                user: User = { null: Int = 0 }
                user.null = 2
                """,
            )
        }
    }
}
