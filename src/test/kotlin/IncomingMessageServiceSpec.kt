import dao.ValidationRequestRepository
import dto.ExternalServiceResponse
import dto.IncomingMessage
import dto.IncomingMessageStatus
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import model.ValidationRequest
import model.ValidationStatus
import service.IncomingMessageService
import java.time.LocalDateTime
import kotlin.random.Random

class IncomingMessageServiceSpec : FreeSpec() {
    val repository = mockk<ValidationRequestRepository>()
    val service = IncomingMessageService(repository)
    init {
        "verify message processed" {
            //arrange
            val id = Long.MAX_VALUE
            val validationRequest = ValidationRequest(id, ValidationStatus.WAITING)
            every { repository.findById(id) } returns validationRequest
            every { repository.saveValidationRequest(validationRequest) } returnsArgument 0
            val validityOn = LocalDateTime.now()

            //act
            service.processIncomingMessage(id.toString(), IncomingMessage(IncomingMessageStatus.OK, response = ExternalServiceResponse(true,
                validityOn
            ), error = null))

            //assert
            verify(exactly = 1) { repository.saveValidationRequest(withArg {
                it.status shouldBe ValidationStatus.VALID
            }) }

        }
    }
}