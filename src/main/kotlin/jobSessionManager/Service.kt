package jobSessionManager

import jobSessionManager.ConditionType.*
import topology.Context
import topology.Event
import topology.Topology

class ConditionProcessor(private val job: Job, private val context: Context) {
    private val topology = Topology()

    fun run(event: Event) {
        job.conditions[PAUSE]?.operations?.forEach { operation ->
            topology.run(event, operation, context, PAUSE.value)
        }
        job.conditions[RUN]?.operations?.forEach { operation ->
            topology.run(event, operation, context, RUN.value)
        }
        job.conditions[SUCCEED]?.operations?.forEach { operation ->
            topology.run(event, operation, context, SUCCEED.value)
        }
        job.conditions[FAIL]?.operations?.forEach { operation ->
            topology.run(event, operation, context, FAIL.value)
        }
        job.conditions[RESET]?.operations?.forEach { operation ->
            topology.run(event, operation, context, RESET.value)
        }
    }
}