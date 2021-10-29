package dao

import model.ValidationRequest
import model.ValidationStatus


interface ValidationRequestRepository {
    fun saveValidationRequest(validationRequest: ValidationRequest) : ValidationRequest
    fun findById(keyLong: Long) : ValidationRequest?
}