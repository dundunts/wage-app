package org.turter.wageapp.application.service

import io.r2dbc.spi.R2dbcException
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.dao.DuplicateKeyException
import org.turter.wageapp.domain.shared.EntityNotFoundException
import java.util.UUID

suspend fun <T> mapDuplicateKey(
    exception: (DuplicateKeyException) -> RuntimeException,
    action: suspend () -> T
): T = try {
    action()
} catch (e: DuplicateKeyException) {
    throw exception(e)
}

suspend fun <T> mapForeignKeyViolation(
    referenceKind: ReferenceKind,
    referencedIds: Collection<UUID>,
    action: suspend () -> T
): T = try {
    action()
} catch (e: DataIntegrityViolationException) {
    if (!e.isForeignKeyViolation()) throw e

    throw EntityNotFoundException(
        concurrentReferenceFailureDetail(referenceKind, referencedIds),
        e
    )
}

private fun Throwable.isForeignKeyViolation(): Boolean =
    generateSequence(this) { it.cause }
        .filterIsInstance<R2dbcException>()
        .any { it.sqlState == FOREIGN_KEY_VIOLATION_SQL_STATE }

private const val FOREIGN_KEY_VIOLATION_SQL_STATE = "23503"
