package org.turter.wageapp.application.advice.controller

import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.support.WebExchangeBindException
import org.springframework.web.server.ServerWebInputException
import org.turter.wageapp.domain.shared.ConflictDataException
import org.turter.wageapp.domain.shared.EntityNotFoundException
import org.turter.wageapp.domain.shared.NotConsistenceDataException
import org.turter.wageapp.domain.shared.NotUniqueValueException

@ControllerAdvice
class WageAppExceptionHandler {

    @ExceptionHandler(ServerWebInputException::class)
    suspend fun handleServerWebInputException(e: ServerWebInputException): ResponseEntity<ProblemDetail> {
        val detail = when (e) {
            is WebExchangeBindException -> e.fieldErrors.joinToString("; ") { error ->
                "${error.field}: ${error.defaultMessage ?: "invalid value"}"
            }
            else -> e.reason ?: "Invalid request"
        }
        val pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail)
        return ResponseEntity.status(pd.status).body(pd)
    }

    @ExceptionHandler(IllegalArgumentException::class)
    suspend fun handleIllegalArgumentException(e: IllegalArgumentException): ResponseEntity<ProblemDetail> {
        val pd = ProblemDetail.forStatus(400)
        pd.detail = e.message
        return ResponseEntity.status(pd.status).body(pd)
    }

    @ExceptionHandler(EntityNotFoundException::class)
    suspend fun handleEntityNotFoundException(e: EntityNotFoundException): ResponseEntity<ProblemDetail> {
        val pd = ProblemDetail.forStatus(404)
        pd.detail = e.message
        return ResponseEntity.status(pd.status).body(pd)
    }

    @ExceptionHandler(NotUniqueValueException::class)
    suspend fun handleNotUniqueValueException(e: NotUniqueValueException): ResponseEntity<ProblemDetail> {
        val pd = ProblemDetail.forStatus(409)
        pd.detail = e.message
        return ResponseEntity.status(pd.status).body(pd)
    }

    @ExceptionHandler(ConflictDataException::class)
    suspend fun handleConflictDataException(e: ConflictDataException): ResponseEntity<ProblemDetail> {
        val pd = ProblemDetail.forStatus(409)
        pd.detail = e.message
        return ResponseEntity.status(pd.status).body(pd)
    }

    @ExceptionHandler(NotConsistenceDataException::class)
    suspend fun handleNotConsistenceDataException(e: NotConsistenceDataException): ResponseEntity<ProblemDetail> {
        val pd = ProblemDetail.forStatus(409)
        pd.detail = e.message
        return ResponseEntity.status(pd.status).body(pd)
    }

}
