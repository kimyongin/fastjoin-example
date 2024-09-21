
data class Event(val data: Map<String, Any>) {
    fun get(jsonPath: String): Any? {
        return JsonHelper.get(data, jsonPath)
    }
}

class Context(val data: MutableMap<String, Any?> = mutableMapOf()) {
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

data class Operand(val source: String, val type: String, val value: String, val contextKeyPostfix: String? = null)

class Condition(val operations: List<Operation>)

data class Job(
    val run: Condition? = null,
    val pause: Condition? = null,
    val succeed: Condition? = null,
    val fail: Condition? = null,
    val reset: Condition? = null
)

class EventProcessUsecase(val job: Job, val context: Context) {
    fun process(event: Event) {
        job.run?.operations?.forEach { condition ->
            condition.run("run", context, event)
        }
        job.pause?.operations?.forEach { condition ->
            condition.run("pause", context, event)
        }
        job.succeed?.operations?.forEach { condition ->
            condition.run("succeed", context, event)
        }
        job.fail?.operations?.forEach { condition ->
            condition.run("fail", context, event)
        }
        job.reset?.operations?.forEach { condition ->
            condition.run("reset", context, event)
        }
    }
}

class Operation(val contextKey: String, val operator: String, val operands: List<Operand>) {

    private fun getContextKey(condition: String, operand: Operand?, event: Event): String {
        val baseKey = contextKey.replace("$.", "$.$condition.")
        return operand?.contextKeyPostfix?.let {
            val postfixValue = event.get(it) ?: ""
            "$baseKey.$postfixValue"
        } ?: baseKey
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
                    val contextKey = getContextKey(condition, operand, event)
                    context.set(contextKey, value)
                }
            }
            "sum" -> {
                operands[0].let { operand ->
                    val value = getValue(condition, operand, event, context)
                    val contextKey = getContextKey(condition, operand, event)
                    val currentValue = context.get(contextKey)
                    val newValue = Operator.sum(operand.type, value, currentValue)
                    context.set(contextKey, newValue)
                }
            }
            "equal" -> {
                val left = getValue(condition, operands[0], event, context)
                val right = getValue(condition, operands[1], event, context)
                val result = Operator.equal(left, right)
                val contextKey = getContextKey(condition, operands[0], event)
                context.set(contextKey, result)
            }
            "greater_than_equal" -> {
                val left = getValue(condition, operands[0], event, context)
                val right = getValue(condition, operands[1], event, context)
                val result = Operator.greaterThanEqual(operands[0].type, left, operands[1].type, right)
                val contextKey = getContextKey(condition, operands[0], event)
                context.set(contextKey, result)
            }
            "has_all_key" -> {
                val result = operands.all { operand ->
                    getValue(condition, operand, event, context) != null
                }
                val contextKey = getContextKey(condition, null, event)
                context.set(contextKey, result)
            }
            "has_any_key" -> {
                val result = operands.any { operand ->
                    getValue(condition, operand, event, context) != null
                }
                val contextKey = getContextKey(condition, null, event)
                context.set(contextKey, result)
            }
        }
    }
}