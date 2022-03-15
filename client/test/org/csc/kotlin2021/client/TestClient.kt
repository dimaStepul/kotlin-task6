package org.csc.kotlin2021.client


import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.ktor.http.*
import io.ktor.server.testing.*
import org.csc.kotlin2021.Message
import org.csc.kotlin2021.server.HttpChatServer
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals


class TestClient {
    private val objectMapper = jacksonObjectMapper()
    private val testUserName = "pupkin"
    private val testMessageContext = "test messages"
    private val testMessage = Message(testUserName, testMessageContext)
    private val servak = HttpChatServer("0.0.0.0", 8080)


    private val testUserName_2 = "testUser1337"
    private val testUserText = """Section 1.10.32 of "de Finibus Bonorum et Malorum", written by Cicero in 45 BC
"Sed ut perspiciatis unde omnis iste natus error sit voluptatem accusantium doloremque laudantium, totam rem aperiam, eaque ipsa quae ab illo inventore veritatis et quasi architecto beatae vitae dicta sunt explicabo. Nemo enim ipsam voluptatem quia voluptas sit aspernatur aut odit aut fugit, sed quia consequuntur magni dolores eos qui ratione voluptatem sequi nesciunt. Neque porro quisquam est, qui dolorem ipsum quia dolor sit amet, consectetur, adipisci velit, sed quia non numquam eius modi tempora incidunt ut labore et dolore magnam aliquam quaerat voluptatem. Ut enim ad minima veniam, quis nostrum exercitationem ullam corporis suscipit laboriosam, nisi ut aliquid ex ea commodi consequatur? Quis autem vel eum iure reprehenderit qui in ea voluptate velit esse quam nihil molestiae consequatur, vel illum qui dolorem eum fugiat quo voluptas nulla pariatur?"

1914 translation by H. Rackham
"But I must explain to you how all this mistaken idea of denouncing pleasure and praising pain was born and I will give you a complete account of the system, and expound the actual teachings of the great explorer of the truth, the master-builder of human happiness. No one rejects, dislikes, or avoids pleasure itself, because it is pleasure, but because those who do not know how to pursue pleasure rationally encounter consequences that are extremely painful. Nor again is there anyone who loves or pursues or desires to obtain pain of itself, because it is pain, but because occasionally circumstances occur in which toil and pain can procure him some great pleasure. To take a trivial example, which of us ever undertakes laborious physical exercise, except to obtain some advantage from it? But who has any right to find fault with a man who chooses to enjoy a pleasure that has no annoying consequences, or one who avoids a pain that produces no resultant pleasure?"

"""
    private val testUserName_3 = "777Dmitry777"
    private val dmitryText = """
        В вашем городе есть некоторое множество остановок ОТ. У каждой остановки есть персональный номер и адрес, записываемый в довольно произвольном виде (например, <<перекрёсток улиц Ленина и Николая Второго>>) и количество платформ -- мест для размещения одного ТС. Платформы одной остановки пронумерованы начиная с 1.

        Вы определяете маршруты ТС. У маршрута есть уникальный номер, известный пассажирам, тип ТС, который его обслуживает, остановка, условно называемая начальной и условная конечная остановка. В реальности ТС ходят по маршруту туда-сюда и вполне могут двигаться в обратном направлении, от <<конечной>> остановки к <<начальной>>.

        Ваш транспорт ходит по расписанию, которое тоже хранится в базе и показывается пассажирам на вашем сайте. В расписании написано с точностью до минуты, в какой момент времени ТС какого маршрута должен прибыть на ту или иную остановку и к какой платформе должен подъехать. ТС стоит у платформы одну минуту, и разумеется никакое другое ТС в это время у этой платформы стоять не может.

        UPD 2021-10-18

        Для рабочих и выходных дней расписания одного маршрута могут быть разные, но больше дни никак не различаются.

        Выпуск ТС на маршруты
    """.trimIndent()
    private val testMessage_2 = Message(testUserName_2, testUserText)
    private val testMessage_3 = Message(testUserName_3, dmitryText)


    @Test
    fun `test client post pupkin message`() {
        withTestApplication(servak.configureModule()) {
            handleRequest(HttpMethod.Post, "/v1/message") {
                addHeader("Content-type", "application/json")
                setBody(objectMapper.writeValueAsString(testMessage))
            }.apply {
                assertEquals(HttpStatusCode.OK, response.status())
                assertEquals(mapOf("status" to "ok"), objectMapper.readValue(response.content ?: ""))
            }
        }
    }

    @Test
    fun `test client post testUser message`() {
        withTestApplication(servak.configureModule()) {
            handleRequest(HttpMethod.Post, "/v1/message") {
                addHeader("Content-type", "application/json")
                setBody(objectMapper.writeValueAsString(testMessage_2))
            }.apply {
                assertEquals(HttpStatusCode.OK, response.status())
                assertEquals(mapOf("status" to "ok"), objectMapper.readValue(response.content ?: ""))
            }
        }
    }

    @Test
    fun `test client post Dmitry message`() {
        withTestApplication(servak.configureModule()) {
            handleRequest(HttpMethod.Post, "/v1/message") {
                addHeader("Content-type", "application/json")
                setBody(objectMapper.writeValueAsString(testMessage_3))
            }.apply {
                assertEquals(HttpStatusCode.OK, response.status())
                assertEquals(mapOf("status" to "ok"), objectMapper.readValue(response.content ?: ""))
            }
        }
    }


    @Test
    fun `health endpoint`() {
        withTestApplication(servak.configureModule()) {
            handleRequest(HttpMethod.Get, "/v1/health").apply {
                assertEquals(HttpStatusCode.OK, response.status())
                assertEquals("OK", response.content)
            }
        }
    }
}
