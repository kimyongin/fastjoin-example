package jobSessionManager

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonValue
import topology.Operation

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
