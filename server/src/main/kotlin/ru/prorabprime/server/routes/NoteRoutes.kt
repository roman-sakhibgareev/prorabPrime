package ru.prorabprime.server.routes

import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import org.koin.ktor.ext.inject
import ru.prorabprime.contract.ApiParams
import ru.prorabprime.contract.ApiPaths
import ru.prorabprime.contract.IdDto
import ru.prorabprime.contract.NoteDto
import ru.prorabprime.contract.NoteRequestDto
import ru.prorabprime.server.auth.owner
import ru.prorabprime.server.model.NoteRecord
import ru.prorabprime.server.service.NoteService

fun Route.noteRoutes() {
    val service by inject<NoteService>()

    get(ApiPaths.OBJECT_NOTES) {
        call.respond(service.list(call.owner, call.uuidParam(ApiParams.ID)).getOrThrow().map { it.toDto() })
    }
    post(ApiPaths.OBJECT_NOTES) {
        val created = service.create(
            call.owner,
            call.uuidParam(ApiParams.ID),
            call.receive<NoteRequestDto>(),
        ).getOrThrow()
        call.respond(HttpStatusCode.Created, IdDto(created.id.toString()))
    }
    put(ApiPaths.NOTE) {
        service.update(call.owner, call.uuidParam(ApiParams.ID), call.receive<NoteRequestDto>()).getOrThrow()
        call.respond(HttpStatusCode.NoContent)
    }
    delete(ApiPaths.NOTE) {
        service.delete(call.owner, call.uuidParam(ApiParams.ID)).getOrThrow()
        call.respond(HttpStatusCode.NoContent)
    }
}

fun NoteRecord.toDto() = NoteDto(id.toString(), objectId.toString(), text, createdAt.toString())
