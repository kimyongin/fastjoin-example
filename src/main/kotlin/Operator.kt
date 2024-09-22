object Operator {

    fun equal(type1: OperandType, value1: Any?, type2: OperandType, value2: Any?): Boolean {
        if (value1 == null || value2 == null) return false
        return when {
            type1 == OperandType.Number && type2 == OperandType.Number -> {
                val num1 = value1.toString().toDoubleOrNull()
                val num2 = value2.toString().toDoubleOrNull()
                num1 == num2
            }
            type1 == OperandType.String && type2 == OperandType.String -> value1 == value2
            type1 == OperandType.Boolean && type2 == OperandType.Boolean -> value1 == value2
            else -> throw UnsupportedOperationException("타입 '$type1'과 '$type2'의 비교는 지원하지 않습니다.")
        }
    }

    fun greaterThan(type1: OperandType, value1: Any?, type2: OperandType, value2: Any?): Boolean {
        return when {
            type1 == OperandType.Number && type2 == OperandType.Number -> {
                val num1 = value1.toString().toDoubleOrNull()
                val num2 = value2.toString().toDoubleOrNull()
                if (num1 != null && num2 != null) num1 > num2 else false
            }
            type1 == OperandType.String && type2 == OperandType.String -> {
                val str1 = value1 as? String
                val str2 = value2 as? String
                if (str1 != null && str2 != null) str1 > str2 else false
            }
            else -> throw UnsupportedOperationException("타입 '$type1'과 '$type2'의 비교는 지원하지 않습니다.")
        }
    }

    fun greaterThanEqual(type1: OperandType, value1: Any?, type2: OperandType, value2: Any?): Boolean {
        return greaterThan(type1, value1, type2, value2) || equal(type1, value1, type2, value2)
    }

    fun sum(type: OperandType, value: Any?, currentValue: Any?): Any {
        return when (type) {
            OperandType.Number -> {
                val num1 = (value as? Number)?.toLong() ?: 0L
                val num2 = (currentValue as? Number)?.toLong() ?: 0L
                num1 + num2
            }

            OperandType.String -> {
                val str1 = value as? String ?: ""
                val str2 = currentValue as? String ?: ""
                str1 + str2
            }

            else -> throw UnsupportedOperationException("Type '$type' is not supported for sum operation")
        }
    }
}
