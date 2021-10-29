package service

import dao.ValidationRequestRepository
import dto.IncomingMessage
import dto.IncomingMessageStatus
import exceptions.RecordNotFoundException
import model.ValidationRequest
import model.ValidationStatus
import java.time.LocalDateTime

class IncomingMessageService(
    private val validationRequestRepository: ValidationRequestRepository
) {


    @Suppress("unused")
    fun processIncomingMessage(key: String, message: IncomingMessage) {
        message.takeIf(::isSuccess)?.apply {
            processNewMessage(key, this)
        } ?: handleFailureMessage(key, message)
    }

    private fun processNewMessage(key: String, message: IncomingMessage) {
        val keyLong = key.toLongOrNull() ?: throw IllegalArgumentException("Ключ $key не является числом")
        validationRequestRepository
            .findById(keyLong)
            ?.let { existing ->
                val newState = when(existing.status) {
                    ValidationStatus.WAITING -> computeValidity(message)
                    ValidationStatus.FAILED ->  computeValidity(message)
                    else -> handleOutOfOrderMessage(message, existing)
                }
                existing.status = newState
                existing.responseReceived = LocalDateTime.now()
                existing.validOn = message.response?.validityOn
                validationRequestRepository.saveValidationRequest(existing)
            } ?: throw RecordNotFoundException(key)
    }

    private fun handleOutOfOrderMessage(message: IncomingMessage, request: ValidationRequest): ValidationStatus {
        println("Received an out-of-order message for request ${request.id}. Request is already in state ${request.status}. Ignoring")
        return request.status
    }

    private fun computeValidity(message: IncomingMessage): ValidationStatus {
        return when (message.response?.validity) {
            true -> ValidationStatus.VALID
            false -> ValidationStatus.INVALID
            null -> ValidationStatus.FAILED_TO_VALIDATE
        }
    }

    private fun handleFailureMessage(key: String, message: IncomingMessage) {
        val keyLong = key.toLongOrNull() ?: throw IllegalArgumentException("Ключ $key не является числом")
        validationRequestRepository
            .findById(keyLong)
            ?.let { existing ->
                val newState = when(existing.status) {
                    ValidationStatus.WAITING -> ValidationStatus.FAILED
                    else -> handleOutOfOrderMessage(message, existing)
                }
                existing.status = newState
                existing.responseReceived = LocalDateTime.now()
                existing.retries += 1
                existing.failReason = message.error?.errorCode + message.error?.errorMsg
                validationRequestRepository.saveValidationRequest(existing)
            }
    }

    private fun isSuccess(incomingMessage: IncomingMessage): Boolean {
        return incomingMessage.status == IncomingMessageStatus.OK
    }
}