package com.abstratt.kirra.spring.api;

import com.abstratt.kirra.Schema
import com.abstratt.kirra.SchemaBuilder
import com.abstratt.kirra.SchemaManagementSnapshot
import com.abstratt.kirra.rest.common.KirraContext
import org.springframework.beans.factory.annotation.Autowired
import kotlin.jvm.Throws;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.net.URI
import javax.annotation.PostConstruct
import javax.ws.rs.container.ContainerRequestContext
import javax.ws.rs.container.ContainerRequestFilter

@Component
@Provider
@Lazy
class KirraRequestFilter : ContainerRequestFilter {
    @Autowired
    private lateinit var schemaBuilder : SchemaBuilder

    private lateinit var schemaManagement: SchemaManagementSnapshot
    private lateinit var instanceManagement: KirraSpringInstanceManagement

    @PostConstruct
    private fun init() {
        this.schemaManagement = SchemaManagementSnapshot(schemaBuilder.build())
        this.instanceManagement = KirraSpringInstanceManagement()
    }
    
    @Throws(IOException::class)
    override fun filter(requestContext: ContainerRequestContext) {
        print("Request filter *******************")
        KirraContext.setBaseURI(URI.create("/"))
        KirraContext.setSchemaManagement(schemaManagement)
        KirraContext.setInstanceManagement(instanceManagement)
    }
}
