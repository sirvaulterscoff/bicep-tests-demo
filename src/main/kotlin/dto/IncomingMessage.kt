package dto

import java.time.LocalDateTime

data class IncomingMessage(
    val status: IncomingMessageStatus,
    val response: ExternalServiceResponse?,
    val error: ExternalServiceError?
)

data class ExternalServiceResponse(
    val validity: Boolean?,
    val validityOn: LocalDateTime
)
data class ExternalServiceError(
    val errorCode: String,
    val errorMsg: String?,
    val rejectReason: String?,
    val externalResponseBody: ByteArray
)
enum class IncomingMessageStatus {
    OK, ERROR, FAILED
}