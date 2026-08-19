package org.turter.wageapp.application.service

import org.springframework.dao.DataIntegrityViolationException
import org.springframework.dao.DuplicateKeyException

suspend inline fun <T> mapDuplicateKey(
    exception: (DuplicateKeyException) -> RuntimeException,
    action: () -> T
): T = try {
    action()
} catch (e: DuplicateKeyException) {
    throw exception(e)
}

suspend inline fun <T> mapInvalidReference(
    exception: (DataIntegrityViolationException) -> RuntimeException,
    action: () -> T
): T = try {
    action()
} catch (e: DataIntegrityViolationException) {
    throw exception(e)
}
