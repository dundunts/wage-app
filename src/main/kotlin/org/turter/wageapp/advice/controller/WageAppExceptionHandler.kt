package org.turter.wageapp.advice.controller

import org.springframework.http.ProblemDetail
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.turter.wageapp.domain.shared.EntityNotFoundException
import org.turter.wageapp.domain.shared.NotUniqueValueException

@ControllerAdvice
class WageAppExceptionHandler {

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

}