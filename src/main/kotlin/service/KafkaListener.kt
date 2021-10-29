package service

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import dto.IncomingMessage

class KafkaListener(
    private val incomingMessageService: IncomingMessageService
) {

    //Kafka listener
    fun receiveMessage(messageKey: String, messageBody: String) {
        runCatching {
            jacksonObjectMapper().readValue<IncomingMessage>(messageBody)
        }.onSuccess {
            incomingMessageService.processIncomingMessage(messageKey, it)
        }
    }
}