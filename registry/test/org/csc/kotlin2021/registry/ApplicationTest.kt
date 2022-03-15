package org.csc.kotlin2021.registry

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.ktor.application.*
import io.ktor.config.*
import io.ktor.http.*
import io.ktor.server.testing.*
import org.csc.kotlin2021.UserAddress
import org.csc.kotlin2021.UserInfo
import org.csc.kotlin2021.checkUserName
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.Ignore
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.fail

fun Application.testModule() {
    (environment.config as MapApplicationConfig).apply {
        // define test environment here
    }
    module(testing = true)
}

class ApplicationTest {
    private val objectMapper = jacksonObjectMapper()
    private val testUserName = "pupkin"
    private val testHttpAddress = UserAddress("127.0.0.1", 9999)
    private val userData = UserInfo(testUserName, testHttpAddress)

    @BeforeEach
    fun clearRegistry() {
        Registry.users.clear()
    }


    @Test
    fun `health endpoint`() {
        withTestApplication({ testModule() }) {
            handleRequest(HttpMethod.Get, "/v1/health").apply {
                assertEquals(HttpStatusCode.OK, response.status())
                assertEquals("OK", response.content)
            }
        }
    }


    @Test
    fun `register user`() {

        registerUser(userData, HttpStatusCode.OK)

        val userTest = "Dmitry"
        val userHttpAddress = UserAddress("127.0.0.1", 8089)
        val newUserInfo = UserInfo(userTest, userHttpAddress)
        registerUser(newUserInfo, HttpStatusCode.OK)
        registerUser(newUserInfo, HttpStatusCode.Conflict)

        val userTest_new = "ДМИТРИй выфа ываzzz"
        val userHttpAddress_new = UserAddress("127.0.0.1", 3333)
        val newUserInfo_new = UserInfo(userTest_new, userHttpAddress_new)
        registerUser(newUserInfo_new, HttpStatusCode.BadRequest)

        val userTest_new2 = "ДМИТРИй"
        val userHttpAddress_new2 = UserAddress("127.0.0.1", 3333)
        val newUserInfo_new2 = UserInfo(userTest_new2, userHttpAddress_new2)
        registerUser(newUserInfo_new2, HttpStatusCode.BadRequest)

    }


    @Test
    fun `list users`() = withRegisteredTestUser {
        handleRequest(HttpMethod.Get, "/v1/users").apply {
            assertEquals(HttpStatusCode.OK, response.status())
        }
    }


    @Test
    fun `update user`() = withRegisteredTestUser {
        fun update(user: String, address: UserAddress) {
            withTestApplication({ testModule() }) {
                handleRequest(HttpMethod.Put, "/v1/users/$user") {
                    addHeader("Content-Type", "application/json")
                    setBody(objectMapper.writeValueAsString(address))
                }.apply {
                    assertEquals(HttpStatusCode.OK, response.status())
                }
            }
        }
        update(testUserName, testHttpAddress)
        update(testUserName, UserAddress("127.0.0.0", 9333))
        update("elprimo", UserAddress("127.0.0.0", 9333))
        update("shelly", testHttpAddress)
    }


    @Test
    fun `delete user`() = withRegisteredTestUser {
        val userTest = "Dmitry"
        val userHttpAddress = UserAddress("127.0.0.1", 8089)
        val newUserInfo = UserInfo(userTest, userHttpAddress)
        registerUser(newUserInfo, HttpStatusCode.OK)
        deleteTestUser(testUserName)
        deleteTestUser("Dmitry")
    }


    fun deleteTestUser(userName: String) {
        withTestApplication({ testModule() }) {
            handleRequest(HttpMethod.Delete, "/v1/users/$userName").apply {
                assertEquals(HttpStatusCode.OK, response.status())
            }
        }
    }


    fun registerUser(user: UserInfo, httpStatus: HttpStatusCode) {
        withTestApplication({ testModule() }) {
            handleRequest(HttpMethod.Post, "/v1/users") {
                addHeader("Content-Type", "application/json")
                setBody(objectMapper.writeValueAsString(user))
            }.apply {
                assertEquals(httpStatus, response.status())
            }
        }
    }


    private fun withRegisteredTestUser(block: TestApplicationEngine.() -> Unit) {
        withTestApplication({ testModule() }) {
            handleRequest {
                method = HttpMethod.Post
                uri = "/v1/users"
                addHeader("Content-type", "application/json")
                setBody(objectMapper.writeValueAsString(userData))
            }.apply {
                assertEquals(HttpStatusCode.OK, response.status())
                val content = response.content ?: fail("No response content")
                val info = objectMapper.readValue<HashMap<String, String>>(content)

                assertNotNull(info["status"])
                assertEquals("ok", info["status"])

                this@withTestApplication.block()
            }
        }
    }
}
