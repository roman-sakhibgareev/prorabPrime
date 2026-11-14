package ru.prorabprime.server.error

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond
import io.ktor.util.AttributeKey
import java.io.EOFException
import java.sql.SQLException
import ru.prorabprime.contract.ErrorCode
import ru.prorabprime.contract.ErrorDto

/** The one place failures become HTTP responses (ARCHITECTURE.md §11). */
fun Application.installErrorHandling() {
    install(StatusPages) {
        exception<ServiceException> { call, cause ->
            val (status, body) = cause.error.toResponse()
            call.respond(status, body)
        }
        exception<BadRequestException> { call, _ ->
            call.respond(HttpStatusCode.BadRequest, ErrorDto(ErrorCode.VALIDATION, "Malformed request"))
        }
        exception<Throwable> { call, cause ->
            if (cause is EOFException && call.attributes.contains(ReadingUploadBody)) {
                // The body ended early (a dropped mobile connection). Stays a 500: the app retries it a few
                // times and then gives up, while a 4xx would be dropped at once and a 502-504 retried forever.
                call.application.environment.log.warn("Request body cut off on ${call.request.local.uri}")
                val body = ErrorDto(ErrorCode.INTERNAL, "The request body was cut off")
                call.respond(HttpStatusCode.InternalServerError, body)
                return@exception
            }
            if (cause.isUniqueViolation()) {
                // A record made with an id somebody else already has (ADR-0021): the same answer as a retry that
                // finds another account's record, and it says nothing about whose it is.
                call.respond(HttpStatusCode.Conflict, ErrorDto(ErrorCode.CONFLICT, "This id is already used"))
                return@exception
            }
            call.application.environment.log.error("Unhandled error on ${call.request.local.uri}", cause)
            // The cause stays in the log: its message may describe internals.
            call.respond(HttpStatusCode.InternalServerError, ErrorDto(ErrorCode.INTERNAL, "Internal error"))
        }
        // The bearer provider's challenge has no body of its own.
        status(HttpStatusCode.Unauthorized) { call, status ->
            call.respond(status, ErrorDto(ErrorCode.UNAUTHORIZED, "Missing or invalid token"))
        }
        unhandled { call ->
            call.respond(HttpStatusCode.NotFound, ErrorDto(ErrorCode.NOT_FOUND, "No such route"))
        }
    }
}

/**
 * Set while a route reads an upload's body, so an `EOFException` then means the client hung up; the same
 * exception anywhere else is a server bug and stays a 500. The parser fails the call on its own, which is
 * why a try/catch in the route cannot translate it.
 */
internal val ReadingUploadBody = AttributeKey<Boolean>("ReadingUploadBody")

private const val UNIQUE_VIOLATION = "23505"

private fun Throwable.isUniqueViolation(): Boolean =
    generateSequence(this) { it.cause }.any { it is SQLException && it.sqlState == UNIQUE_VIOLATION }

internal fun ServiceError.toResponse(): Pair<HttpStatusCode, ErrorDto> = when (this) {
    is ServiceError.NotFound -> HttpStatusCode.NotFound to ErrorDto(ErrorCode.NOT_FOUND, message)

    is ServiceError.Validation -> HttpStatusCode.BadRequest to ErrorDto(ErrorCode.VALIDATION, message, fieldErrors)

    is ServiceError.UnsupportedMedia ->
        HttpStatusCode.UnsupportedMediaType to ErrorDto(ErrorCode.UNSUPPORTED_MEDIA, message)

    is ServiceError.Conflict -> HttpStatusCode.Conflict to ErrorDto(ErrorCode.CONFLICT, message)

    is ServiceError.TooLarge -> HttpStatusCode.PayloadTooLarge to ErrorDto(ErrorCode.PAYLOAD_TOO_LARGE, message)

    is ServiceError.QuotaExceeded ->
        HttpStatusCode.InsufficientStorage to ErrorDto(ErrorCode.QUOTA_EXCEEDED, message)
}
