package com.abstratt.kirra.spring.api;

import com.abstratt.kirra.rest.common.KirraContext
import kotlin.jvm.Throws;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.net.URI
import javax.ws.rs.container.ContainerRequestContext
import javax.ws.rs.container.ContainerRequestFilter
import javax.ws.rs.container.ContainerResponseContext
import javax.ws.rs.container.ContainerResponseFilter


@Component
@Provider
@Lazy
class KirraResponseFilter : ContainerResponseFilter {
    @Throws(IOException::class)
    override fun filter(requestContext: ContainerRequestContext, responseContext: ContainerResponseContext) {
        print("Response filter *******************")
        KirraContext.setBaseURI(null)
    }
}
