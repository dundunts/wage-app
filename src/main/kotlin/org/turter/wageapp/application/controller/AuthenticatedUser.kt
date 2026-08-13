package org.turter.wageapp.application.controller

import kotlinx.coroutines.reactor.awaitSingle
import org.springframework.security.core.context.ReactiveSecurityContextHolder

internal suspend fun currentUserId(): String =
    ReactiveSecurityContextHolder.getContext().map { it.authentication.name }.awaitSingle()
