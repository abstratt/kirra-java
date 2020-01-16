package com.abstratt.kirra.spring.api

import com.abstratt.kirra.KirraErrorCode
import com.abstratt.kirra.rest.common.ErrorDTO
import com.abstratt.kirra.rest.common.KirraContext
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.core.AuthenticationException
import org.springframework.security.web.AuthenticationEntryPoint
import org.springframework.stereotype.Component
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import javax.ws.rs.core.MediaType

@Component
class KirraAuthenticationEntryPoint : AuthenticationEntryPoint {
    @Autowired
    private lateinit var objectMapper : ObjectMapper

    override fun commence(request: HttpServletRequest, response: HttpServletResponse, authException: AuthenticationException) {
        if (!response.isCommitted) {
            response.status = HttpStatus.UNAUTHORIZED.value()
            response.contentType = MediaType.APPLICATION_JSON
            response.addHeader("WWW-Authenticate", "Basic realm=\"${KirraContext.getApplication().name}\"")
            response.writer.println(objectMapper.writeValue(response.writer, ErrorDTO(KirraErrorCode.AUTHENTICATION_REQUIRED)))
        }
    }

}