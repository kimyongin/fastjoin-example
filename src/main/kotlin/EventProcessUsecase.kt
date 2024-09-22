import OperandType.*
import OperatorType.*

data class Event(val data: Map<String, Any>) {
    fun get(jsonPath: String): Any? {
        return JsonHelper.get(data, jsonPath)
    }
}

data class Context(val data: MutableMap<String, Any?> = mutableMapOf()) {
    private var updated: Boolean = false

    fun get(jsonPath: String): Any? {
        return JsonHelper.get(data, jsonPath)
    }

    fun set(jsonPath: String, value: Any?) {
        val isUpdated = JsonHelper.set(data, jsonPath, value)
        if (!updated && isUpdated) {
            updated = true
        }
    }

    fun toJsonString(isPretty: Boolean = false): String {
        return JsonHelper.toJsonString(data, isPretty)
    }

    fun isUpdated(): Boolean {
        return updated
    }
}

enum class OperandSource {
    EVENT,
    CONSTANT,
    CONTEXT,
    CONTEXT_RUN,
    CONTEXT_SUCCEED,
    CONTEXT_FAIL,
    CONTEXT_PAUSE,
    CONTEXT_RESET
}

enum class OperandType {
    NUMBER,
    STRING,
    BOOLEAN
}

data class Operand(
    val source: OperandSource,
    val type: OperandType,
    val value: String,
    val options: Map<String, String>? = null
) {
    fun getConstantValue(): Any? {
        return when (type) {
            NUMBER -> value.toDoubleOrNull()
            STRING -> value
            BOOLEAN -> value.toBooleanStrictOrNull()
        }
    }
}

data class Condition(val operations: List<Operation>)

data class Job(
    val run: Condition? = null,
    val pause: Condition? = null,
    val succeed: Condition? = null,
    val fail: Condition? = null,
    val reset: Condition? = null
)

class EventProcessUsecase(val job: Job, val context: Context) {
    fun process(event: Event) {
        val conditions = listOf(
            "run" to job.run,
            "pause" to job.pause,
            "succeed" to job.succeed,
            "fail" to job.fail,
            "reset" to job.reset
        )
        conditions.forEach { (conditionName, condition) ->
            condition?.operations?.forEach { operation ->
                operation.run(conditionName, context, event)
            }
        }
    }
}

enum class OperatorType {
    PROJECTION,
    MERGE,
    COUNT,
    SUM,
    EQUAL,
    NOT_EQUAL,
    GREATER_THAN,
    GREATER_THAN_EQUAL,
    LESS_THAN,
    LESS_THAN_EQUAL,
    HAS_ALL_KEY,
    HAS_ANY_KEY,
    SUBSTRING
}

class Operation(val contextKey: String, val operator: OperatorType, val operands: List<Operand>) {

    private fun getContextKey(condition: String): String {
        return contextKey.replace("$.", "$.$condition.")
    }

    private fun getValue(condition: String, operand: Operand, event: Event, context: Context): Any? {
        return when (operand.source) {
            OperandSource.EVENT -> event.get(operand.value)
            OperandSource.CONSTANT -> operand.getConstantValue()
            OperandSource.CONTEXT -> context.get(operand.value.replace("$.", "$.$condition."))
            OperandSource.CONTEXT_RUN -> context.get(operand.value.replace("$.", "$.run."))
            OperandSource.CONTEXT_SUCCEED -> context.get(operand.value.replace("$.", "$.succeed."))
            OperandSource.CONTEXT_FAIL -> context.get(operand.value.replace("$.", "$.fail."))
            OperandSource.CONTEXT_PAUSE -> context.get(operand.value.replace("$.", "$.pause."))
            OperandSource.CONTEXT_RESET -> context.get(operand.value.replace("$.", "$.reset."))
        }
    }

    fun run(condition: String, context: Context, event: Event) {
        when (operator) {
            PROJECTION, MERGE -> {
                for (operand in operands) {
                    val value = getValue(condition, operand, event, context)
                    val contextKey = operand.options?.get("contextKeyPostfix")?.let {
                        val postfixValue = event.get(it) ?: ""
                        "${getContextKey(condition)}.$postfixValue"
                    } ?: getContextKey(condition)
                    context.set(contextKey, value)
                }
            }

            COUNT -> {/* not implemented */
            }

            SUM -> {
                operands[0].let { operand ->
                    val value = getValue(condition, operand, event, context)
                    val contextKey = getContextKey(condition)
                    val currentValue = context.get(contextKey)
                    val newValue = Operator.sum(operand.type, value, currentValue)
                    context.set(contextKey, newValue)
                }
            }

            NOT_EQUAL -> {/* not implemented */
            }

            EQUAL -> {
                val left = getValue(condition, operands[0], event, context)
                val right = getValue(condition, operands[1], event, context)
                val result = Operator.equal(operands[0].type, left, operands[1].type, right)
                val contextKey = getContextKey(condition)
                context.set(contextKey, result)
            }

            GREATER_THAN -> {/* not implemented */
            }

            GREATER_THAN_EQUAL -> {
                val left = getValue(condition, operands[0], event, context)
                val right = getValue(condition, operands[1], event, context)
                val result = Operator.greaterThanEqual(operands[0].type, left, operands[1].type, right)
                val contextKey = getContextKey(condition)
                context.set(contextKey, result)
            }

            LESS_THAN -> {/* not implemented */
            }

            LESS_THAN_EQUAL -> {/* not implemented */
            }

            HAS_ALL_KEY -> {
                val result = operands.all { operand ->
                    getValue(condition, operand, event, context) != null
                }
                val contextKey = getContextKey(condition)
                context.set(contextKey, result)
            }

            HAS_ANY_KEY -> {
                val result = operands.any { operand ->
                    getValue(condition, operand, event, context) != null
                }
                val contextKey = getContextKey(condition)
                context.set(contextKey, result)
            }

            SUBSTRING -> {
                val value = getValue(condition, operands[0], event, context)
                val start = operands[0].options?.get("start")?.toInt() ?: 0
                val end = operands[0].options?.get("end")?.toInt() ?: value.toString().length
                val result = value.toString().substring(start, end)
                val contextKey = getContextKey(condition)
                context.set(contextKey, result)
            }
        }
    }
}