package com.abstratt.kirra.spring

import org.springframework.core.NestedRuntimeException

interface ErrorCodeException {
    val errorCode: ErrorCode
}

class BusinessException : NestedRuntimeException, ErrorCodeException {
    override var errorCode: ErrorCode

    constructor(message: String?, e: Exception) : super(message, e) {
        this.errorCode = ErrorCode.UNEXPECTED
    }

    constructor(errorCode: ErrorCode, message: String?) : super(message ?: errorCode.defaultMessage) {
        this.errorCode = errorCode
    }

    companion object {
        private val serialVersionUID = 1L

        fun ensure(condition: Boolean, errorCode: ErrorCode, messageSupplier: () -> String) {
            if (!condition) {
                throw BusinessException(errorCode, messageSupplier())
            }
        }

        fun ensure(condition: Boolean, errorCode: ErrorCode, message: String? = null) {
            if (!condition) {
                throw BusinessException(errorCode, message)
            }
        }

        fun wrapException(e: Exception): BusinessException {
            return if (e is BusinessException) {
                e
            } else BusinessException("Unexpected error", e)
        }
    }
}

enum class ErrorCode private constructor(val errorCode: Int?, val errorKind: ErrorKind, val defaultMessage: String) {
    UNEXPECTED(-1, ErrorKind.INTERNAL, "Unexpected error"),
    UNSPECIFIED(null, "Unspecified error"),
    UNKNOWN_OBJECT(1001, ErrorKind.UNKNOWN_OBJECT, "Object not found"),
    TOO_MANY_ITEMS(1002, "Too may items"),
    INVALID_OR_MISSING_DATA(1003, "Invalid or missing data");

    enum class ErrorKind {
        CLIENT,
        UNKNOWN_OBJECT,
        INTERNAL
    }

    private constructor(errorCode: Int?, defaultMessage: String) : this(errorCode, ErrorKind.CLIENT, defaultMessage) {}
}
