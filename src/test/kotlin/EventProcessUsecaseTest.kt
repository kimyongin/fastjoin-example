import ConditionType.*
import OperandSource.*
import OperatorType.*
import org.junit.jupiter.api.Test


private fun getJob(): Job {
    return Job(
        mapOf(
            RUN to getRunCondition(),
            PAUSE to getPauseCondition(),
            SUCCEED to getSucceedCondition(),
            FAIL to getFailCondition(),
            RESET to getResetCondition()
        )
    )
}

private fun getRunCondition(): Condition {
    val operations = listOf(
        Operation(
            contextKey = "$.projection",
            operator = PROJECTION,
            operands = listOf(
                Operand(source = EVENT, type = OperandType.STRING, value = "$.info.name")
            )
        ),
        Operation(
            contextKey = "$.quantity_sum",
            operator = SUM,
            operands = listOf(
                Operand(source = EVENT, type = OperandType.NUMBER, value = "$.quantity")
            )
        ),
        Operation(
            contextKey = "$.quantity_sum_equal",
            operator = EQUAL,
            operands = listOf(
                Operand(source = CONTEXT, type = OperandType.NUMBER, value = "$.quantity_sum"),
                Operand(source = CONSTANT, type = OperandType.NUMBER, value = "5")
            )
        )
    )
    return Condition(operations)
}

private fun getPauseCondition(): Condition {
    val operations = listOf(
        Operation(
            contextKey = "$.inner.projection",
            operator = PROJECTION,
            operands = listOf(
                Operand(source = EVENT, type = OperandType.STRING, value = "$.info.name"),
            )
        ),
        Operation(
            contextKey = "$.inner.quantity_sum",
            operator = SUM,
            operands = listOf(
                Operand(source = EVENT, type = OperandType.NUMBER, value = "$.quantity")
            )
        ),
        Operation(
            contextKey = "$.inner.quantity_sum_equal",
            operator = GREATER_THAN_EQUAL,
            operands = listOf(
                Operand(source = CONTEXT, type = OperandType.NUMBER, value = "$.inner.quantity_sum"),
                Operand(source = CONSTANT, type = OperandType.NUMBER, value = "5")
            )
        )
    )
    return Condition(operations)
}

private fun getSucceedCondition(): Condition {
    val operations = listOf(
        Operation(
            contextKey = "$.human",
            operator = MERGE,
            operands = listOf(
                Operand(source = EVENT, type = OperandType.STRING, value = "$.value", options = mapOf("contextKeyPostfix" to "$.type"))
            )
        ),
        Operation(
            contextKey = "$.has_all",
            operator = HAS_ALL_KEY,
            operands = listOf(
                Operand(source = CONTEXT, type = OperandType.STRING, value = "$.human.head"),
                Operand(source = CONTEXT, type = OperandType.STRING, value = "$.human.foot")
            )
        ),
        Operation(
            contextKey = "$.has_any",
            operator = HAS_ANY_KEY,
            operands = listOf(
                Operand(source = CONTEXT, type = OperandType.STRING, value = "$.human.head"),
                Operand(source = CONTEXT, type = OperandType.STRING, value = "$.human.foot")
            )
        )
    )
    return Condition(operations)
}


fun getFailCondition(): Condition {
    val operations = listOf(
        Operation(
            contextKey = "$.date",
            operator = SUBSTRING,
            operands = listOf(
                Operand(source = EVENT, type = OperandType.STRING, value = "$.datetime", options = mapOf("start" to "0", "end" to "10")),
            )
        )
    )
    return Condition(operations)
}

private fun getResetCondition(): Condition {
    val operations = listOf(
        Operation(
            contextKey = "$.copy_from_succeed.has_all",
            operator = PROJECTION,
            operands = listOf(
                Operand(source = CONTEXT_SUCCEED, type = OperandType.STRING, value = "$.has_all"),
            )
        ),
        Operation(
            contextKey = "$.copy_from_succeed.has_any",
            operator = PROJECTION,
            operands = listOf(
                Operand(source = CONTEXT_SUCCEED, type = OperandType.STRING, value = "$.has_any") // reset 에서 succeed 컨텍스트에 접근
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
                "value" to "head",
                "datetime" to "2021-01-01T01:02:03"
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
                "value" to "foot",
                "datetime" to "2021-01-02T02:03:04"
            )
        )
        eventProcessUsecase.process(event1)
        println(context.toJsonString(true))
        eventProcessUsecase.process(event2)
        println(context.toJsonString(true))
        if (context.isUpdated()) {
            println("updated")
            // jobSession 업데이트
        }
    }
}