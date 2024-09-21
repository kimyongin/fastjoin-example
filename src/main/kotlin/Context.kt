import com.fasterxml.jackson.databind.ObjectMapper
import com.jayway.jsonpath.InvalidPathException
import com.jayway.jsonpath.JsonPath

// 예외 클래스 정의
class NotValidJsonPathException(message: String) : Exception(message)
class NotSupportedArrayTypeException(message: String) : Exception(message)

class Context(val data: MutableMap<String, Any?> = mutableMapOf()) {
    private var updated: Boolean = false

    companion object {
        private val objectMapper = ObjectMapper()
    }

    /**
     * JSON 경로를 사용하여 데이터를 조회하는 함수
     *
     * @param jsonPath 조회할 JSON 경로 (예: "$.a.b.c")
     * @return 조회된 값, 없으면 null
     * @throws NotValidJsonPathException 유효하지 않은 jsonPath인 경우
     * @throws NotSupportedArrayTypeException 배열 표현이 포함된 경우
     */
    fun <T> get(jsonPath: String): T? {
        // jsonPath의 유효성 검사
        isValidJsonPath(jsonPath)

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
        @Suppress("UNCHECKED_CAST")
        return current as? T
    }

    /**
     * JSON 경로를 사용하여 데이터를 설정하는 함수
     *
     * @param jsonPath 설정할 JSON 경로 (예: "$.a.b.c")
     * @param value 설정할 값
     * @throws NotValidJsonPathException 유효하지 않은 jsonPath인 경우
     * @throws NotSupportedArrayTypeException 배열 표현이 포함된 경우
     */
    fun set(jsonPath: String, value: Any?) {
        // jsonPath의 유효성 검사
        isValidJsonPath(jsonPath)

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
        // 마지막 키에 값 설정
        val lastKey = pathParts.last()
        currentMap[lastKey] = value
        updated = true
    }

    /**
     * 주어진 jsonPath가 유효한지 확인하고, 배열 표현이 있는지 검사하는 함수
     *
     * @param jsonPath 검사할 JSON 경로
     * @throws NotValidJsonPathException 유효하지 않은 jsonPath인 경우
     * @throws NotSupportedArrayTypeException 배열 표현이 포함된 경우
     */
    private fun isValidJsonPath(jsonPath: String) {
        try {
            // jsonPath를 컴파일하여 유효성 검사
            val compiledPath = JsonPath.compile(jsonPath)
            // 배열 표현이 포함되어 있는지 검사
            val pathString = compiledPath.path
            val arrayPattern = Regex("\\[(\\d+|\\*)\\]")
            if (arrayPattern.containsMatchIn(pathString)) {
                throw NotSupportedArrayTypeException("배열 표현이 포함된 jsonPath는 지원하지 않습니다: $jsonPath")
            }
        } catch (e: InvalidPathException) {
            throw NotValidJsonPathException("유효하지 않은 jsonPath입니다: $jsonPath")
        }
    }

    /**
     * Context 객체의 깊은 복사본을 생성하는 함수
     *
     * @return 복사된 Context 객체
     */
    fun copy(): Context {
        val newData = deepCopy(data)
        return Context(newData)
    }

    /**
     * 데이터를 깊은 복사하는 재귀 함수
     *
     * @param original 원본 데이터
     * @return 복사된 데이터
     */
    private fun deepCopy(original: Map<String, Any?>): MutableMap<String, Any?> {
        val copy = mutableMapOf<String, Any?>()
        for ((key, value) in original) {
            copy[key] = if (value is Map<*, *>) {
                deepCopy(value as Map<String, Any?>)
            } else {
                value
            }
        }
        return copy
    }

    /**
     * 데이터 맵을 JSON 문자열로 변환하는 함수
     *
     * @return JSON 문자열
     */
    fun toJsonString(isPretty: Boolean = false): String {
        return if (isPretty) {
            objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(data)
        } else {
            objectMapper.writeValueAsString(data)
        }
    }

    /**
     * 데이터가 업데이트되었는지 여부를 반환하는 함수
     *
     * @return 업데이트 여부
     */
    fun isUpdated(): Boolean {
        return updated
    }
}
