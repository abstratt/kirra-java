package com.abstratt.kirra.spring.api

import org.apache.commons.lang3.StringUtils
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.util.*
import javax.servlet.FilterChain
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

@Component
class CustomCorsFilter : OncePerRequestFilter() {
    override fun doFilterInternal(request: HttpServletRequest, response: HttpServletResponse, filterChain: FilterChain) {
        try {
            filterChain.doFilter(request, response)
        } finally {
            response.setHeader("Access-Control-Allow-Origin", getAllowOrigin(request))
            response.setHeader("Access-Control-Allow-Credentials", getAllowCredentials().toString())
            response.setHeader("Access-Control-Allow-Headers", StringUtils.join(getAllowHeaders(), ","))
            response.setHeader("Access-Control-Allow-Methods", StringUtils.join(getAllowMethods(), ","))
        }
    }

    protected fun getAllowMethods(): List<String> {
        return Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "HEAD")
    }

    protected fun getAllowHeaders(): List<String> {
        return Arrays.asList("origin", "content-type", "accept", "authorization", "x-requested-with", "cache-control")
    }

    protected fun getAllowCredentials(): Boolean {
        return true
    }

    protected fun getAllowOrigin(requestContext: HttpServletRequest): String {
        return requestContext.getHeader("Origin") ?: ""
    }

}