import ConditionType.*
import OperandOption.*
import OperatorType.*

class EventProcessService(private val job: Job, private val context: Context) {
    private val operationService = OperationService()

    fun process(event: Event) {
        job.conditions[PAUSE]?.operations?.forEach { operation ->
            operationService.run(operation, PAUSE, context, event)
        }
        job.conditions[RUN]?.operations?.forEach { operation ->
            operationService.run(operation, RUN, context, event)
        }
        job.conditions[SUCCEED]?.operations?.forEach { operation ->
            operationService.run(operation, SUCCEED, context, event)
        }
        job.conditions[FAIL]?.operations?.forEach { operation ->
            operationService.run(operation, FAIL, context, event)
        }
        job.conditions[RESET]?.operations?.forEach { operation ->
            operationService.run(operation, RESET, context, event)
        }
    }
}

class OperationService {
    private fun getContextKey(contextKey: String, conditionType: ConditionType): String {
        return contextKey.replace("$.", "$.${conditionType.value}.")
    }

    private fun getValue(conditionType: ConditionType, operand: Operand, event: Event, context: Context): Any? {
        return when (operand.source) {
            OperandSource.EVENT -> event.get(operand.value)
            OperandSource.CONSTANT -> operand.getConstantValue()
            OperandSource.CONTEXT -> context.get(operand.value.replace("$.", "$.${conditionType.value}."))
            OperandSource.CONTEXT_RUN -> context.get(operand.value.replace("$.", "$.${RUN.value}."))
            OperandSource.CONTEXT_SUCCEED -> context.get(operand.value.replace("$.", "$.${SUCCEED.value}."))
            OperandSource.CONTEXT_FAIL -> context.get(operand.value.replace("$.", "$.${FAIL.value}."))
            OperandSource.CONTEXT_PAUSE -> context.get(operand.value.replace("$.", "$.${PAUSE.value}."))
            OperandSource.CONTEXT_RESET -> context.get(operand.value.replace("$.", "$.${RESET.value}."))
        }
    }

    fun run(operation: Operation, conditionType: ConditionType, context: Context, event: Event) {
        when (operation.operator) {
            PROJECTION, MERGE -> {
                for (operand in operation.operands) {
                    val value = getValue(conditionType, operand, event, context)
                    val contextKey = operand.options?.get(CONTEXT_KEY_POSTFIX)?.let {
                        val postfixValue = event.get(it) ?: ""
                        "${getContextKey(operation.contextKey, conditionType)}.$postfixValue"
                    } ?: getContextKey(operation.contextKey, conditionType)
                    context.set(contextKey, value)
                }
            }

            COUNT -> {/* not implemented */
            }

            SUM -> {
                operation.operands[0].let { operand ->
                    val value = getValue(conditionType, operand, event, context)
                    val contextKey = getContextKey(operation.contextKey, conditionType)
                    val currentValue = context.get(contextKey)
                    val newValue = Operator.sum(operand.type, value, currentValue)
                    context.set(contextKey, newValue)
                }
            }

            NOT_EQUAL -> {/* not implemented */
            }

            EQUAL -> {
                val left = getValue(conditionType, operation.operands[0], event, context)
                val right = getValue(conditionType, operation.operands[1], event, context)
                val result = Operator.equal(operation.operands[0].type, left, operation.operands[1].type, right)
                val contextKey = getContextKey(operation.contextKey, conditionType)
                context.set(contextKey, result)
            }

            GREATER_THAN -> {/* not implemented */
            }

            GREATER_THAN_EQUAL -> {
                val left = getValue(conditionType, operation.operands[0], event, context)
                val right = getValue(conditionType, operation.operands[1], event, context)
                val result =
                    Operator.greaterThanEqual(operation.operands[0].type, left, operation.operands[1].type, right)
                val contextKey = getContextKey(operation.contextKey, conditionType)
                context.set(contextKey, result)
            }

            LESS_THAN -> {/* not implemented */
            }

            LESS_THAN_EQUAL -> {/* not implemented */
            }

            HAS_ALL_KEY -> {
                val result = operation.operands.all { operand ->
                    getValue(conditionType, operand, event, context) != null
                }
                val contextKey = getContextKey(operation.contextKey, conditionType)
                context.set(contextKey, result)
            }

            HAS_ANY_KEY -> {
                val result = operation.operands.any { operand ->
                    getValue(conditionType, operand, event, context) != null
                }
                val contextKey = getContextKey(operation.contextKey, conditionType)
                context.set(contextKey, result)
            }

            SUBSTRING -> {
                val value = getValue(conditionType, operation.operands[0], event, context)
                val start = operation.operands[0].options?.get(START)?.toInt() ?: 0
                val end = operation.operands[0].options?.get(END)?.toInt() ?: value.toString().length
                val result = value.toString().substring(start, end)
                val contextKey = getContextKey(operation.contextKey, conditionType)
                context.set(contextKey, result)
            }
        }
    }
}