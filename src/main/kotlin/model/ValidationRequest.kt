package model

import java.time.LocalDateTime

class ValidationRequest(
    var id: Long,
    var status: ValidationStatus,
    var validOn: LocalDateTime? = null,
    var failReason: String? = null,
    var responseReceived: LocalDateTime? = null,
    var retries: Int = 0
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ValidationRequest

        if (id != other.id) return false
        if (status != other.status) return false
        if (validOn != other.validOn) return false
        if (failReason != other.failReason) return false
        if (responseReceived != other.responseReceived) return false
        if (retries != other.retries) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + status.hashCode()
        result = 31 * result + (validOn?.hashCode() ?: 0)
        result = 31 * result + (failReason?.hashCode() ?: 0)
        result = 31 * result + (responseReceived?.hashCode() ?: 0)
        result = 31 * result + retries
        return result
    }
}

enum class ValidationStatus {
    WAITING, VALID, INVALID, FAILED_TO_VALIDATE, FAILED
}