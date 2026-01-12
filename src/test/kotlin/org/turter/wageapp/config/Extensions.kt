package org.turter.wageapp.config

import org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.mockJwt
import org.springframework.test.web.reactive.server.WebTestClient

fun WebTestClient.withUser(): WebTestClient = mutateWith(mockJwt().jwt { builder -> builder.subject(USER_ID) })