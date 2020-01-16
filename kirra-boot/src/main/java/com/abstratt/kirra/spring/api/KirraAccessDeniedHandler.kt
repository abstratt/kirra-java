package com.abstratt.kirra.spring.api

import com.abstratt.kirra.KirraErrorCode
import com.abstratt.kirra.rest.common.ErrorDTO
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.web.access.AccessDeniedHandler
import org.springframework.stereotype.Component
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import javax.ws.rs.core.MediaType

@Component
class KirraAccessDeniedHandler : AccessDeniedHandler {
    @Autowired
    private lateinit var objectMapper : ObjectMapper
    override fun handle(request: HttpServletRequest, response: HttpServletResponse, accessDeniedException: AccessDeniedException) {
        if (!response.isCommitted) {
            response.contentType = MediaType.APPLICATION_JSON
            response.writer.println(objectMapper.writeValue(response.writer, ErrorDTO(KirraErrorCode.AUTHENTICATION_REQUIRED)))
        }
    }
}