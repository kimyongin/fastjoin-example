object Operator {

    fun equal(value1: Any?, value2: Any?): Boolean {
        return value1?.toString() == value2?.toString()
    }

    fun greaterThan(type1: String, value1: Any?, type2: String, value2: Any?): Boolean {
        return when {
            type1 == "number" && type2 == "number" -> {
                val num1 = value1.toString().toLongOrNull()
                val num2 = value2.toString().toLongOrNull()
                if (num1 != null && num2 != null) num1 > num2 else false
            }

            else -> {
                val str1 = value1?.toString()
                val str2 = value2?.toString()
                if (str1 != null && str2 != null) str1 == str2 else false
            }
        }
    }

    fun greaterThanEqual(type1: String, value1: Any?, type2: String, value2: Any?): Boolean {
        return greaterThan(type1, value1, type2, value2) || equal(value1, value2)
    }

    fun sum(type: String, value: Any?, currentValue: Any?): Any {
        return when (type) {
            "number" -> {
                val num1 = (value as? Number)?.toLong() ?: 0L
                val num2 = (currentValue as? Number)?.toLong() ?: 0L
                num1 + num2
            }

            "string" -> {
                val str1 = value as? String ?: ""
                val str2 = currentValue as? String ?: ""
                str1 + str2
            }

            else -> throw UnsupportedOperationException("Type '$type' is not supported for sum operation")
        }
    }
}
