import com.fasterxml.jackson.databind.introspect.TypeResolutionContext.Empty
import org.junit.jupiter.api.Test


private fun getJob(): Job {
    return Job(
        run = getRunCondition(),
        succeed = getSucceedCondition(),
        paused = getPausedCondition(),
        fail = Condition(emptyList()),
        reset = Condition(emptyList())
    )
}

private fun getRunCondition(): Condition {
    val operations = listOf(
        Operation(
            contextKey = "$.projection",
            operator = "projection",
            operands = listOf(
                Operand(source = "event", type = "string", value = "$.info.name")
            )
        ),
        Operation(
            contextKey = "$.quantity_sum",
            operator = "sum",
            operands = listOf(
                Operand(source = "event", type = "number", value = "$.quantity")
            )
        ),
        Operation(
            contextKey = "$.quantity_sum_equal",
            operator = "equal",
            operands = listOf(
                Operand(source = "context", type = "number", value = "$.quantity_sum"),
                Operand(source = "constant", type = "number", value = "5")
            )
        )
    )
    return Condition(operations)
}

private fun getPausedCondition(): Condition {
    val operations = listOf(
        Operation(
            contextKey = "$.inner.projection",
            operator = "projection",
            operands = listOf(
                Operand(source = "event", type = "string", value = "$.info.name"),
            )
        ),
        Operation(
            contextKey = "$.inner.quantity_sum",
            operator = "sum",
            operands = listOf(
                Operand(source = "event", type = "number", value = "$.quantity")
            )
        ),
        Operation(
            contextKey = "$.inner.quantity_sum_equal",
            operator = "equal",
            operands = listOf(
                Operand(source = "context", type = "number", value = "$.inner.quantity_sum"),
                Operand(source = "constant", type = "number", value = "5")
            )
        )
    )
    return Condition(operations)
}

private fun getSucceedCondition(): Condition {
    val operations = listOf(
        Operation(
            contextKey = "$.human",
            operator = "merge",
            operands = listOf(
                Operand(source = "event", type = "string", value = "$.value", contextKeyPostfix = "$.type")
            )
        )
    )
    return Condition(operations)
}

class EventProcessUsecaseTest {
    @Test
    fun testContext() {
        val context = Context()

        context.set("$.projection1", "name1")
        context.set("$.projection2.meta.meta_name", "name1")
        context.set("$.projection2.meta.meta_level", 12)

        println(context.toJsonString())
    }

    @Test
    fun testProcess() {
        val job = getJob()
        val context = Context()
        val eventProcessUsecase = EventProcessUsecase(job, context)

        val event1 = Event(
            mapOf(
                "info" to mapOf(
                    "name" to "name1",
                    "level" to 12
                ),
                "quantity" to 2,
                "type" to "head",
                "value" to "head"
            )
        )
        val event2 = Event(
            mapOf(
                "info" to mapOf(
                    "name" to "name2",
                    "level" to 13
                ),
                "quantity" to 3,
                "type" to "foot",
                "value" to "foot"
            )
        )
        eventProcessUsecase.process(event1)
        println(context.toJsonString(true))
        eventProcessUsecase.process(event2)
        println(context.toJsonString(true))
    }
}