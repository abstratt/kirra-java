package com.abstratt.kirra.spring.api

import com.abstratt.kirra.InstanceManagement
import com.abstratt.kirra.SchemaManagement
import com.abstratt.kirra.rest.common.KirraContext
import com.abstratt.kirra.spring.KirraSpringApplication
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.net.URI
import javax.servlet.FilterChain
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse


@Component
class KirraContextFilter : OncePerRequestFilter() {
    @Autowired
    private lateinit var kirraSpringApplication : KirraSpringApplication
    @Autowired
    private lateinit var schemaManagement: SchemaManagement
    @Autowired
    private lateinit var instanceManagement: InstanceManagement

    override fun doFilterInternal(request: HttpServletRequest, response: HttpServletResponse, filterChain: FilterChain) {
        val baseUri = URI.create(request.requestURL.toString())
        KirraContext.setBaseURI(baseUri.resolve("/kirra/" ))
        KirraContext.setSchemaManagement(schemaManagement)
        KirraContext.setInstanceManagement(instanceManagement)
        KirraContext.setApplication(kirraSpringApplication)
        try {
            filterChain.doFilter(request, response)
        } finally {
            KirraContext.setBaseURI(null)
            KirraContext.setSchemaManagement(null)
            KirraContext.setInstanceManagement(null)
            KirraContext.setApplication(null)
        }
    }
}
