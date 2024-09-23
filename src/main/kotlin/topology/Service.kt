package topology

import topology.OperandOption.*
import topology.OperatorType.*

class Topology {
    private fun getContextKey(contextKey: String, contextNamespace: String): String {
        return contextKey.replace("$.", "$.${contextNamespace}.")
    }

    private fun getValue(event: Event, operand: Operand,context: Context, contextNamespace: String): Any? {
        return when (operand.source) {
            OperandSource.EVENT -> event.get(operand.value)
            OperandSource.CONSTANT -> operand.getConstantValue()
            OperandSource.CONTEXT -> {
                val namespace = operand.options?.get(CONTEXT_NAMESPACE) ?: contextNamespace
                context.get(operand.value.replace("$.", "$.$namespace."))
            }
        }
    }

    fun run(event: Event, operation: Operation, context: Context, contextNamespace: String) {
        when (operation.operator) {
            PROJECTION, MERGE -> {
                for (operand in operation.operands) {
                    val value = getValue(event, operand, context, contextNamespace)
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
                    val value = getValue(event, operand, context, contextNamespace)
                    val contextKey = getContextKey(operation.contextKey, contextNamespace)
                    val currentValue = context.get(contextKey)
                    val newValue = Operator.sum(operand.type, value, currentValue)
                    context.set(contextKey, newValue)
                }
            }

            NOT_EQUAL -> {/* not implemented */
            }

            EQUAL -> {
                val left = getValue(event, operation.operands[0], context, contextNamespace)
                val right = getValue(event, operation.operands[1], context, contextNamespace)
                val result = Operator.equal(operation.operands[0].type, left, operation.operands[1].type, right)
                val contextKey = getContextKey(operation.contextKey, contextNamespace)
                context.set(contextKey, result)
            }

            GREATER_THAN -> {/* not implemented */
            }

            GREATER_THAN_EQUAL -> {
                val left = getValue(event, operation.operands[0], context, contextNamespace)
                val right = getValue(event, operation.operands[1], context, contextNamespace)
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
                    getValue(event, operand, context, contextNamespace) != null
                }
                val contextKey = getContextKey(operation.contextKey, contextNamespace)
                context.set(contextKey, result)
            }

            HAS_ANY_KEY -> {
                val result = operation.operands.any { operand ->
                    getValue(event, operand, context, contextNamespace) != null
                }
                val contextKey = getContextKey(operation.contextKey, contextNamespace)
                context.set(contextKey, result)
            }

            SUBSTRING -> {
                val value = getValue(event, operation.operands[0], context, contextNamespace)
                val start = operation.operands[0].options?.get(START)?.toInt() ?: 0
                val end = operation.operands[0].options?.get(END)?.toInt() ?: value.toString().length
                val result = value.toString().substring(start, end)
                val contextKey = getContextKey(operation.contextKey, contextNamespace)
                context.set(contextKey, result)
            }
        }
    }
}