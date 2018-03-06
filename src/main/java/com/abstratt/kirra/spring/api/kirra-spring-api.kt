package com.abstratt.kirra.spring.api

import com.abstratt.kirra.KirraApplication
import com.abstratt.kirra.rest.common.KirraContext
import com.abstratt.kirra.rest.resources.KirraJaxRsApplication
import com.abstratt.kirra.spring.BaseEntity
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Component
import java.io.IOException
import java.net.URI
import javax.ws.rs.ApplicationPath
import javax.ws.rs.container.ContainerRequestContext
import javax.ws.rs.container.ContainerRequestFilter
import javax.ws.rs.container.ContainerResponseContext
import javax.ws.rs.container.ContainerResponseFilter
import javax.ws.rs.ext.Provider
import kotlin.reflect.KClass



/*
@RestController
@RequestMapping("/api/v2")
class KirraSchemaController() {
    @Autowired
    lateinit var applicationContext: ApplicationContext

    @RequestMapping(path = arrayOf("/"), method = arrayOf(RequestMethod.GET))
    open fun entities(): HttpEntity<String> {
        val emFactory = EntityManagerFactoryUtils.findEntityManagerFactory(applicationContext, null)
        val entities = emFactory.metamodel.entities
        val services = applicationContext.getBeansWithAnnotation(Service::class.java)
        return ResponseEntity.status(HttpStatus.OK).body("""
            Services: ${services.values.map { it.javaClass.simpleName }.toTypedArray().joinToString()}
            Entities: ${entities.map { it.name }.toTypedArray().joinToString()}
        """)
    }
}
*/




