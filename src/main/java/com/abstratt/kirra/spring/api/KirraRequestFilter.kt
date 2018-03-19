package com.abstratt.kirra.spring.api;

import com.abstratt.kirra.SchemaManagementSnapshot
import com.abstratt.kirra.rest.common.KirraContext
import com.abstratt.kirra.spring.KirraSpringInstanceManagement
import org.springframework.beans.factory.annotation.Autowired
import kotlin.jvm.Throws;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.net.URI
import javax.annotation.PostConstruct
import javax.servlet.http.HttpServletRequest
import javax.ws.rs.container.ContainerRequestContext
import javax.ws.rs.container.ContainerRequestFilter
import javax.ws.rs.core.Context

@Component
@Provider
@Lazy
class KirraRequestFilter : ContainerRequestFilter {
    @Autowired
    private lateinit var schemaManagement: SchemaManagementSnapshot
    @Autowired
    private lateinit var instanceManagement: KirraSpringInstanceManagement

    @Context
    private lateinit var servletRequest: HttpServletRequest


    @PostConstruct
    private fun init() {
    }
    
    @Throws(IOException::class)
    override fun filter(requestContext: ContainerRequestContext) {
        val requestUri = requestContext.uriInfo.requestUri
        val baseUri = URI.create(this.servletRequest!!.requestURI)
        KirraContext.setBaseURI(baseUri)
        KirraContext.setSchemaManagement(schemaManagement)
        KirraContext.setInstanceManagement(instanceManagement)
    }
}
