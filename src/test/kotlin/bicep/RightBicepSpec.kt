package bicep

import dao.ValidationRequestRepository
import dto.ExternalServiceError
import dto.ExternalServiceResponse
import dto.IncomingMessage
import dto.IncomingMessageStatus
import exceptions.RecordNotFoundException
import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.ints.shouldBeLessThan
import io.kotest.matchers.longs.shouldBeGreaterThan
import io.kotest.matchers.longs.shouldNotBeGreaterThan
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import model.ValidationRequest
import model.ValidationStatus
import org.junit.jupiter.api.assertThrows
import service.IncomingMessageService
import service.KafkaListener
import java.time.LocalDateTime
import kotlin.system.measureTimeMillis

class RightBicepSpec : FreeSpec() {
    val repository = mockk<ValidationRequestRepository>()
    val service = IncomingMessageService(repository)
    val kafkaListener = KafkaListener(service)

    init {
        isolationMode = IsolationMode.InstancePerLeaf
        "[B]oundary conditions" - {
            "(C)onformance - соответствие формату" - {
                "does not process invalid json" {
                    assertThrows<Exception> {
                        kafkaListener.receiveMessage("null", "{")
                    }
                }
                "does not process  json with invalid status" {
                    assertThrows<Exception> {
                        kafkaListener.receiveMessage("11", "{status: \"FATAL\"}")
                    }
                }
            }
            "(O)rdering - правильный ли порядок" - {
                "receiving response before sending leads to error" {
                    //arrange
                    every { repository.findById(404L) } returns null

                    //act
                    val executable = {
                        service.processIncomingMessage(
                            404.toString(), IncomingMessage(
                                IncomingMessageStatus.OK, response = ExternalServiceResponse(
                                    true,
                                    LocalDateTime.now()
                                ), error = null
                            )
                        )
                    }

                    //assert
                    assertThrows<RecordNotFoundException>(executable)
                }
                // если ваши методы работаю с коллекциями, либо возвращают их, то тут
                // логично проверить передачу/возврат значений в разном порядке
            }
            "(R)ange - Все ли диапазоны значений проходят" - {
                for (id in listOf(-1L, Int.MAX_VALUE.toLong())) {
                    "verify $id" {
                        positiveCase(id)
                    }
                }
                //Эта проверка также касается возвращаемых величин - следует проверить, что они находятся в каких-то адекватных пределах

            }
            "(R)eference - какие зависимости есть у кода и что если они не работают?" - {
                //arrange
                val expectedId = 505L
                every { repository.findById(expectedId) } throws RuntimeException("База данных недоступна")

                //act
                val executable = {
                    service.processIncomingMessage(
                        expectedId.toString(), IncomingMessage(
                            IncomingMessageStatus.OK, response = ExternalServiceResponse(
                                true,
                                LocalDateTime.now()
                            ), error = null
                        )
                    )
                }

                //assert
                assertThrows<RuntimeException>(executable).message shouldBe "База данных недоступна"
                verify {
                    repository.findById(withArg { it shouldBe expectedId })
                }
            }
            "(E)xistence - проверка несуществующих данных" - {
                val positiveWithErrorFilled = normalResponse {
                    ExternalServiceError("111", "Error", "Rejected just because", ByteArray(0))
                }
                val negativeWithStatusFilled = errorResponse(buildResponse = {
                    ExternalServiceResponse(true, LocalDateTime.now())
                })

                "positive response with error filled" {
                    testCase(101, positiveWithErrorFilled, ValidationStatus.VALID)
                }
                "negative response with normal reponse filled" {
                    testCase(102, negativeWithStatusFilled, ValidationStatus.FAILED)
                }
            }
            "(С)ardinality - проверка порядка величин" {
                val negativeWithVeryLongErrorCode = errorResponse(buildError = {
                    ExternalServiceError("1".repeat(4096), "Error".repeat(1000), "2".repeat(2000), ByteArray(0))
                })
                testCase(103, negativeWithVeryLongErrorCode, ValidationStatus.FAILED) {
                    it.failReason!!.length shouldBeLessThan 256
                }
                //Не забываем также, что нужно проверять и допустимые диапазоны
            }
            "(T)ime - привязан ли код ко времени" {
                val positiveWithValidityInThePast = normalResponse(buildResponse = {
                    ExternalServiceResponse(true, LocalDateTime.now().minusYears(200))
                })
                testCase(104, positiveWithValidityInThePast, ValidationStatus.INVALID)

            }
        }
        "[I]nverse" {


        }
        "[C]ross-checking using other means. Можем ли проверить результат, используя другие средства" {
            //тут мог бы быть интеграционный тест, который после
            // обработки заглядывает в БД с помощью SQL запроса, если бы я его написал
            testCase(105, normalResponse(), ValidationStatus.VALID)
            //select status from ValidationRequest where id=105
        }
        "[E]rror conditions - проверьте все unhappy-path" - {
            //arrange
            for (id in listOf("null", "", "123a", " 544", "678 ")) {
                "checking id $id" {
                    //act
                    val executable = { service.processIncomingMessage(id, normalResponse()) }

                    //assert
                    assertThrows<IllegalArgumentException>(executable).message shouldBe "Ключ $id не является числом"
                    verify(inverse = true) {
                        repository.findById(any())
                        repository.saveValidationRequest(any())
                    }
                }
            }
        }
        "[P]erformance - все ли норм с производительностью" {
            measureTimeMillis {
                testCase(106, normalResponse(), ValidationStatus.VALID)

            } shouldNotBeGreaterThan 3_000L
        }
    }

    private fun positiveCase(id: Long) {
        //arrange
        val validationRequest = ValidationRequest(id, ValidationStatus.WAITING)
        every { repository.findById(id) } returns validationRequest
        every { repository.saveValidationRequest(validationRequest) } returnsArgument 0
        val validityOn = LocalDateTime.now()

        //act
        service.processIncomingMessage(
            id.toString(), IncomingMessage(
                IncomingMessageStatus.OK, response = ExternalServiceResponse(
                    true,
                    validityOn
                ), error = null
            )
        )

        //assert
        verify(exactly = 1) { repository.saveValidationRequest(validationRequest) }
    }

    private fun testCase(
        id: Long, message: IncomingMessage, targetStatus: ValidationStatus,
        validateRequest: (ValidationRequest) -> Unit = {}
    ) {
        //arrange
        val validationRequest = ValidationRequest(id, ValidationStatus.WAITING)
        every { repository.findById(id) } returns validationRequest
        every { repository.saveValidationRequest(validationRequest) } returnsArgument 0
        //act
        service.processIncomingMessage(
            id.toString(), message
        )

        //assert
        verify(exactly = 1) {
            repository.saveValidationRequest(withArg {
                it.status shouldBe targetStatus
                validateRequest(it)
            })
        }
    }
}

fun normalResponse(
    buildResponse: () -> ExternalServiceResponse? = { ExternalServiceResponse(true, LocalDateTime.now()) },
    buildError: () -> ExternalServiceError? = { null }
): IncomingMessage = IncomingMessage(IncomingMessageStatus.OK, response = buildResponse(), error = buildError())

fun errorResponse(
    buildResponse: () -> ExternalServiceResponse? = { null },
    buildError: () -> ExternalServiceError? = { ExternalServiceError("111", null, null, ByteArray(0)) }
): IncomingMessage = IncomingMessage(
    IncomingMessageStatus.FAILED,
    response = buildResponse(),
    error = buildError()
)