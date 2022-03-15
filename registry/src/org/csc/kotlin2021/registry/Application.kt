package org.csc.kotlin2021.registry

import com.fasterxml.jackson.core.util.DefaultIndenter
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter
import com.fasterxml.jackson.databind.SerializationFeature
import io.ktor.application.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.jackson.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.server.netty.*
import org.csc.kotlin2021.UserAddress
import org.csc.kotlin2021.UserInfo
import org.csc.kotlin2021.checkUserName
import org.slf4j.event.Level
import java.lang.Thread.sleep
import java.util.concurrent.ConcurrentHashMap
import kotlin.concurrent.thread

const val ATTEMPTS_TIME: Long = 120000

fun main(args: Array<String>) {
    thread {
        while (true) {
            sleep(ATTEMPTS_TIME)
            checkAttempts()
        }
    }
    EngineMain.main(args)
}

object Registry {
    val users = ConcurrentHashMap<String, UserAddress>()
    val attempts = ConcurrentHashMap<String, Int>()
    const val numberOfAllAttempts = 3
}

fun checkAttempts() {
    for ((name, amountOfAttempts) in Registry.attempts) {
        when (amountOfAttempts) {
            in 1 until Registry.numberOfAllAttempts -> Registry.attempts[name]?.plus(1)
            Registry.numberOfAllAttempts -> {
                Registry.attempts.remove(name)
                Registry.users.remove(name)
            }
        }
    }
}

@Suppress("UNUSED_PARAMETER")
@JvmOverloads
fun Application.module(testing: Boolean = false) {
    install(CallLogging) {
        level = Level.INFO
        filter { call -> call.request.path().startsWith("/") }
    }

    install(ContentNegotiation) {
        jackson {
            enable(SerializationFeature.INDENT_OUTPUT)
        }
    }
    install(StatusPages) {
        exception<IllegalArgumentException> { cause ->
            call.respond(HttpStatusCode.BadRequest, cause.message ?: "invalid argument")
        }
        exception<UserAlreadyRegisteredException> { cause ->
            call.respond(HttpStatusCode.Conflict, cause.message ?: "user already registered")
        }
        exception<IllegalUserNameException> { cause ->
            call.respond(HttpStatusCode.BadRequest, cause.message ?: "illegal user name")
        }
    }
    routing {
        get("/v1/health") {
            call.respondText("OK", contentType = ContentType.Text.Plain)
        }

        post("/v1/users") {
            val user = call.receive<UserInfo>()
            val name = user.name
            checkUserName(name) ?: throw IllegalUserNameException()
            if (Registry.users.containsKey(name)) {
                throw UserAlreadyRegisteredException()
            }
            Registry.users[name] = user.address
            Registry.attempts[name] = 0
            call.respond(mapOf("status" to "ok"))
        }

        get("/v1/users") {
            call.respond(HttpStatusCode.OK, Registry.users)
        }

        put("/v1/users/{name}") {
            val address = call.receive<UserAddress>()
            val name = call.parameters["name"].toString()
            checkUserName(name) ?: throw IllegalUserNameException()
            Registry.users.remove(name)
            Registry.attempts.remove(name)
            Registry.users[name] = address
            Registry.attempts[name] = 0
            call.respond(mapOf("status" to "ok"))
        }

        delete("/v1/users/{name}") {
            val name: String = call.parameters["name"].toString()
            if (name in Registry.users.keys) {
                Registry.users.remove(name)
                Registry.attempts.remove(name)
                call.respond(mapOf("status" to "ok"))
            } else {
                call.respondText("No users found", status = HttpStatusCode.NotFound)
            }
        }
    }
}

class UserAlreadyRegisteredException : RuntimeException("User already registered")
class IllegalUserNameException : RuntimeException("Illegal user name")
