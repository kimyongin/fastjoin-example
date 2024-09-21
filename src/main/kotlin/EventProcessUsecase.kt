import com.jayway.jsonpath.JsonPath
import com.jayway.jsonpath.DocumentContext
import com.jayway.jsonpath.Configuration
import com.jayway.jsonpath.Option

data class Event(val data: Map<String, Any>) {
    private val jsonConfig: Configuration = Configuration.builder().options(Option.DEFAULT_PATH_LEAF_TO_NULL).build()
    private var document: DocumentContext = JsonPath.using(jsonConfig).parse(data)

    fun get(jsonPath: String): Any? {
        val path = JsonPath.compile(jsonPath)
        return document.read(path)
    }
}

class Operation(val contextKey: String, val operator: String, val operands: List<Operand>) {

    private fun getContextKey(condition: String, operand: Operand, event: Event): String {
        val baseKey = contextKey.replace("$.", "$.$condition.")
        return operand.contextKeyPostfix?.let {
            val postfixValue = event.get(it) ?: ""
            "$baseKey.$postfixValue"
        } ?: baseKey
    }

    private fun getValue(condition: String, operand: Operand, event: Event, context: Context): Any? {
        return when (operand.source) {
            "event" -> event.get(operand.value)
            "constant" -> operand.value
            "context" -> context.get(operand.value.replace("$.", "$.$condition."))
            else -> throw UnsupportedOperationException("'${operand.source}' is not supported")
        }
    }

    fun getClassFromType(type: String): Class<*> {
        return when (type) {
            "Int" -> Int::class.java
            "Long" -> Long::class.java
            "String" -> String::class.java
            else -> throw UnsupportedOperationException("'${type}' is not supported")
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
                    val value = getValue(condition, operand, event, context) as? Int ?: 0
                    val contextKey = getContextKey(condition, operand, event)
                    val currentValue = context.get<Int>(contextKey) ?: 0
                    val newValue = currentValue + value
                    context.set(contextKey, newValue)
                }
            }

            "equal" -> {
                val left = getValue(condition, operands[0], event, context)
                val right = getValue(condition, operands[1], event, context)
                val leftValue = (left as? String)?.toIntOrNull() ?: left
                val rightValue = (right as? String)?.toIntOrNull() ?: right
                val result = leftValue == rightValue
                val contextKey = getContextKey(condition, operands[0], event)
                context.set(contextKey, result)
            }
        }
    }
}

data class Operand(val source: String, val type: String, val value: String, val contextKeyPostfix: String? = null)

class Condition(val operations: List<Operation>)

data class Job(
    val run: Condition? = null,
    val paused: Condition? = null,
    val succeed: Condition? = null,
    val fail: Condition? = null,
    val reset: Condition? = null
)

class EventProcessUsecase(val job: Job, val context: Context) {
    fun process(event: Event) {
        job.run?.operations?.forEach { condition ->
            condition.run("run", context, event)
        }
        job.paused?.operations?.forEach { condition ->
            condition.run("paused", context, event)
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