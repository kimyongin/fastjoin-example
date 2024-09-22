
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

sealed class OperandType {
    object Number : OperandType()
    object String : OperandType()
    object Boolean : OperandType()
}

data class Operand(
    val source: String,
    val type: OperandType,
    val value: String,
    val options: Map<String, String>? = null
)

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

class Operation(val contextKey: String, val operator: String, val operands: List<Operand>) {

    private fun getContextKey(condition: String): String {
        return contextKey.replace("$.", "$.$condition.")
    }

    private fun getValue(condition: String, operand: Operand, event: Event, context: Context): Any? {
        return when (operand.source) {
            "event" -> event.get(operand.value)
            "constant" -> operand.value
            "context" -> context.get(operand.value.replace("$.", "$.$condition."))
            "context::run" -> context.get(operand.value.replace("$.", "$.run."))
            "context::succeed" -> context.get(operand.value.replace("$.", "$.succeed."))
            "context::fail" -> context.get(operand.value.replace("$.", "$.fail."))
            "context::pause" -> context.get(operand.value.replace("$.", "$.pause."))
            "context::reset" -> context.get(operand.value.replace("$.", "$.reset."))
            else -> throw UnsupportedOperationException("'${operand.source}' is not supported")
        }
    }

    fun run(condition: String, context: Context, event: Event) {
        when (operator) {
            "projection", "merge" -> {
                for (operand in operands) {
                    val value = getValue(condition, operand, event, context)
                    val contextKey = operand.options?.get("contextKeyPostfix")?.let {
                        val postfixValue = event.get(it) ?: ""
                        "${getContextKey(condition)}.$postfixValue"
                    } ?: getContextKey(condition)
                    context.set(contextKey, value)
                }
            }
            "count" -> {/* not implemented */}
            "sum" -> {
                operands[0].let { operand ->
                    val value = getValue(condition, operand, event, context)
                    val contextKey = getContextKey(condition)
                    val currentValue = context.get(contextKey)
                    val newValue = Operator.sum(operand.type, value, currentValue)
                    context.set(contextKey, newValue)
                }
            }
            "not_equal" -> {/* not implemented */}
            "equal" -> {
                val left = getValue(condition, operands[0], event, context)
                val right = getValue(condition, operands[1], event, context)
                val result = Operator.equal(operands[0].type, left, operands[1].type, right)
                val contextKey = getContextKey(condition)
                context.set(contextKey, result)
            }
            "greater_than" -> {/* not implemented */}
            "greater_than_equal" -> {
                val left = getValue(condition, operands[0], event, context)
                val right = getValue(condition, operands[1], event, context)
                val result = Operator.greaterThanEqual(operands[0].type, left, operands[1].type, right)
                val contextKey = getContextKey(condition)
                context.set(contextKey, result)
            }
            "less_than" -> {/* not implemented */}
            "less_than_equal" -> {/* not implemented */}
            "has_all_key" -> {
                val result = operands.all { operand ->
                    getValue(condition, operand, event, context) != null
                }
                val contextKey = getContextKey(condition)
                context.set(contextKey, result)
            }
            "has_any_key" -> {
                val result = operands.any { operand ->
                    getValue(condition, operand, event, context) != null
                }
                val contextKey = getContextKey(condition)
                context.set(contextKey, result)
            }
            "substring" -> {
                val value = getValue(condition, operands[0], event, context)
                val start = operands[0].options?.get("start")?.toInt() ?: 0
                val end = operands[0].options?.get("end")?.toInt() ?: value.toString().length
                val result = value.toString().substring(start, end)
                val contextKey = getContextKey(condition)
                context.set(contextKey, result)
            }
            else -> throw UnsupportedOperationException("Operator '$operator' is not supported")
        }
    }
}