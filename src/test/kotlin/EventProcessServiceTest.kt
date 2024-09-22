import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Test
import com.fasterxml.jackson.module.kotlin.readValue

fun getResourceAsText(path: String): String? =
    object {}.javaClass.getResource(path)?.readText()

fun getJob(jsonString: String): Job {
    val objectMapper = ObjectMapper()
    return objectMapper.readValue(jsonString)
}

class EventProcessServiceTest {
    @Test
    fun testContext() {
        val context = Context()

        context.set("$.projection1", "name1")
        context.set("$.projection2.meta.meta_name", "name1")
        context.set("$.projection2.meta.meta_level", 12)

        println(context.toJsonString())
    }

    @Test
    fun testProcess() {
        val resource: String = getResourceAsText("/job.json")!!
        val job = getJob(resource)
        val context = Context()
        val eventProcessUsecase = EventProcessService(job, context)

        val event1 = Event(
            mapOf(
                "info" to mapOf(
                    "name" to "name1",
                    "level" to 12
                ),
                "quantity" to 2,
                "type" to "head",
                "value" to "head",
                "datetime" to "2021-01-01T01:02:03"
            )
        )
        val event2 = Event(
            mapOf(
                "info" to mapOf(
                    "name" to "name2",
                    "level" to 13
                ),
                "quantity" to 3,
                "type" to "foot",
                "value" to "foot",
                "datetime" to "2021-01-02T02:03:04"
            )
        )
        eventProcessUsecase.process(event1)
        println(context.toJsonString(true))
        eventProcessUsecase.process(event2)
        println(context.toJsonString(true))
        if (context.isUpdated()) {
            println("updated")
            // jobSession 업데이트
        }
    }
}