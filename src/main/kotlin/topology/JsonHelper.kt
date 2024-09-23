package topology

import com.fasterxml.jackson.databind.ObjectMapper

object JsonHelper {
    private val objectMapper = ObjectMapper()

    fun get(data: Map<String, Any?>, jsonPath: String): Any? {
        // 경로의 앞부분에 있는 "$"와 "."을 제거
        val cleanedPath = jsonPath.removePrefix("$").removePrefix(".")
        // 경로를 "."으로 분리하여 각 키의 리스트로 변환
        val pathParts = cleanedPath.split(".")
        var current: Any? = data
        for (part in pathParts) {
            if (current is Map<*, *>) {
                current = current[part]
            } else {
                return null
            }
        }
        return current
    }

    fun set(data: MutableMap<String, Any?>, jsonPath: String, value: Any?): Boolean {
        // 경로의 앞부분에 있는 "$"와 "."을 제거
        val cleanedPath = jsonPath.removePrefix("$").removePrefix(".")
        // 경로를 "."으로 분리하여 각 키의 리스트로 변환
        val pathParts = cleanedPath.split(".")

        var currentMap: MutableMap<String, Any?> = data
        for (i in 0 until pathParts.size - 1) {
            val key = pathParts[i]
            val next = currentMap[key]
            if (next == null || next !is MutableMap<*, *>) {
                // 다음 키에 해당하는 값이 없거나 맵이 아닌 경우 새로운 맵을 생성하여 설정
                val newMap = mutableMapOf<String, Any?>()
                currentMap[key] = newMap
                currentMap = newMap
            } else {
                // 다음 맵으로 이동
                currentMap = next as MutableMap<String, Any?>
            }
        }

        // 마지막 키에 대해 값 비교
        val lastKey = pathParts.last()
        val currentValue = currentMap[lastKey]

        // 값이 다를 때만 값을 설정하고 true를 반환
        return if (currentValue != value) {
            currentMap[lastKey] = value
            true
        } else {
            // 값이 같다면 false 반환
            false
        }
    }

    fun clone(data: Map<String, Any?>): MutableMap<String, Any?> {
        return data.mapValues { entry ->
            when (val value = entry.value) {
                is Map<*, *> -> (value as Map<String, Any?>).toMutableMap()
                is List<*> -> value.toMutableList()
                else -> value
            }
        }.toMutableMap()
    }

    fun toJsonString(data: Map<String, Any?>, isPretty: Boolean = false): String {
        return if (isPretty) {
            objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(data)
        } else {
            objectMapper.writeValueAsString(data)
        }
    }
}