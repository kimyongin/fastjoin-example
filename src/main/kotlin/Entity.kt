import OperandType.*
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonValue

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

enum class OperandSource(@JsonValue val value: String) {
    EVENT("event"),
    CONSTANT("constant"),
    CONTEXT("context"),
    CONTEXT_RUN("context::run"),
    CONTEXT_SUCCEED("context::succeed"),
    CONTEXT_FAIL("context::fail"),
    CONTEXT_PAUSE("context::pause"),
    CONTEXT_RESET("context::reset");

    companion object {
        @JsonCreator
        fun fromValue(value: String): OperandSource {
            return entries.find { it.value == value } ?: throw IllegalArgumentException("Invalid OperandSource: $value")
        }
    }
}

enum class OperandType(@JsonValue val value: String) {
    NUMBER("number"),
    STRING("string"),
    BOOLEAN("boolean");

    companion object {
        @JsonCreator
        fun fromValue(value: String): OperandType {
            return entries.find { it.value == value } ?: throw IllegalArgumentException("Invalid OperandType: $value")
        }
    }
}

data class Operand @JsonCreator constructor(
    @JsonProperty("source") val source: OperandSource,
    @JsonProperty("type") val type: OperandType,
    @JsonProperty("value") val value: String,
    @JsonProperty("options") val options: Map<String, String>? = null
) {
    fun getConstantValue(): Any? {
        return when (type) {
            NUMBER -> value.toDoubleOrNull()
            STRING -> value
            BOOLEAN -> value.toBooleanStrictOrNull()
        }
    }
}

enum class ConditionType(@JsonValue val value: String) {
    RUN("run"),
    PAUSE("pause"),
    SUCCEED("succeed"),
    FAIL("fail"),
    RESET("reset");

    companion object {
        @JsonCreator
        fun fromValue(value: String): ConditionType {
            return entries.find { it.value == value } ?: throw IllegalArgumentException("Invalid ConditionType: $value")
        }
    }
}

data class Condition @JsonCreator constructor(
    @JsonProperty("operations") val operations: List<Operation>
)

data class Job @JsonCreator constructor(
    @JsonProperty("conditions") val conditions: Map<ConditionType, Condition>
)


enum class OperatorType(@JsonValue val value: String) {
    PROJECTION("projection"),
    MERGE("merge"),
    COUNT("count"),
    SUM("sum"),
    EQUAL("equal"),
    NOT_EQUAL("not_equal"),
    GREATER_THAN("greater_than"),
    GREATER_THAN_EQUAL("greater_than_equal"),
    LESS_THAN("less_than"),
    LESS_THAN_EQUAL("less_than_equal"),
    HAS_ALL_KEY("has_all_key"),
    HAS_ANY_KEY("has_any_key"),
    SUBSTRING("substring");

    companion object {
        @JsonCreator
        fun fromValue(value: String): OperatorType {
            return entries.find { it.value == value } ?: throw IllegalArgumentException("Invalid OperatorType: $value")
        }
    }
}

data class Operation @JsonCreator constructor(
    @JsonProperty("context_key") val contextKey: String,
    @JsonProperty("operator") val operator: OperatorType,
    @JsonProperty("operands") val operands: List<Operand>
)