import ConditionType.*
import OperandOption.*
import OperatorType.*

class EventProcessService(private val job: Job, private val context: Context) {
    private val operationService = OperationService()

    fun process(event: Event) {
        job.conditions[PAUSE]?.operations?.forEach { operation ->
            operationService.run(operation, PAUSE.value, context, event)
        }
        job.conditions[RUN]?.operations?.forEach { operation ->
            operationService.run(operation, RUN.value, context, event)
        }
        job.conditions[SUCCEED]?.operations?.forEach { operation ->
            operationService.run(operation, SUCCEED.value, context, event)
        }
        job.conditions[FAIL]?.operations?.forEach { operation ->
            operationService.run(operation, FAIL.value, context, event)
        }
        job.conditions[RESET]?.operations?.forEach { operation ->
            operationService.run(operation, RESET.value, context, event)
        }
    }
}

class OperationService {
    private fun getContextKey(contextKey: String, contextNamespace: String): String {
        return contextKey.replace("$.", "$.${contextNamespace}.")
    }

    private fun getValue(operand: Operand, event: Event, context: Context, contextNamespace: String): Any? {
        return when (operand.source) {
            OperandSource.EVENT -> event.get(operand.value)
            OperandSource.CONSTANT -> operand.getConstantValue()
            OperandSource.CONTEXT -> {
                val namespace = operand.options?.get(CONTEXT_NAMESPACE) ?: contextNamespace
                context.get(operand.value.replace("$.", "$.$namespace."))
            }
        }
    }

    fun run(operation: Operation, contextNamespace: String, context: Context, event: Event) {
        when (operation.operator) {
            PROJECTION, MERGE -> {
                for (operand in operation.operands) {
                    val value = getValue(operand, event, context, contextNamespace)
                    val contextKey = operand.options?.get(CONTEXT_KEY_POSTFIX)?.let {
                        val postfixValue = event.get(it) ?: ""
                        "${getContextKey(operation.contextKey, contextNamespace)}.$postfixValue"
                    } ?: getContextKey(operation.contextKey, contextNamespace)
                    context.set(contextKey, value)
                }
            }

            COUNT -> {/* not implemented */
            }

            SUM -> {
                operation.operands[0].let { operand ->
                    val value = getValue(operand, event, context, contextNamespace)
                    val contextKey = getContextKey(operation.contextKey, contextNamespace)
                    val currentValue = context.get(contextKey)
                    val newValue = Operator.sum(operand.type, value, currentValue)
                    context.set(contextKey, newValue)
                }
            }

            NOT_EQUAL -> {/* not implemented */
            }

            EQUAL -> {
                val left = getValue(operation.operands[0], event, context, contextNamespace)
                val right = getValue(operation.operands[1], event, context, contextNamespace)
                val result = Operator.equal(operation.operands[0].type, left, operation.operands[1].type, right)
                val contextKey = getContextKey(operation.contextKey, contextNamespace)
                context.set(contextKey, result)
            }

            GREATER_THAN -> {/* not implemented */
            }

            GREATER_THAN_EQUAL -> {
                val left = getValue(operation.operands[0], event, context, contextNamespace)
                val right = getValue(operation.operands[1], event, context, contextNamespace)
                val result =
                    Operator.greaterThanEqual(operation.operands[0].type, left, operation.operands[1].type, right)
                val contextKey = getContextKey(operation.contextKey, contextNamespace)
                context.set(contextKey, result)
            }

            LESS_THAN -> {/* not implemented */
            }

            LESS_THAN_EQUAL -> {/* not implemented */
            }

            HAS_ALL_KEY -> {
                val result = operation.operands.all { operand ->
                    getValue(operand, event, context, contextNamespace) != null
                }
                val contextKey = getContextKey(operation.contextKey, contextNamespace)
                context.set(contextKey, result)
            }

            HAS_ANY_KEY -> {
                val result = operation.operands.any { operand ->
                    getValue(operand, event, context, contextNamespace) != null
                }
                val contextKey = getContextKey(operation.contextKey, contextNamespace)
                context.set(contextKey, result)
            }

            SUBSTRING -> {
                val value = getValue(operation.operands[0], event, context, contextNamespace)
                val start = operation.operands[0].options?.get(START)?.toInt() ?: 0
                val end = operation.operands[0].options?.get(END)?.toInt() ?: value.toString().length
                val result = value.toString().substring(start, end)
                val contextKey = getContextKey(operation.contextKey, contextNamespace)
                context.set(contextKey, result)
            }
        }
    }
}