package org.turter.wageapp.tips

class TipsBotException(val timedOut: Boolean, cause: Throwable) : RuntimeException(
    if (timedOut) "Tips bot request timed out" else "Tips bot request failed",
    cause,
)
