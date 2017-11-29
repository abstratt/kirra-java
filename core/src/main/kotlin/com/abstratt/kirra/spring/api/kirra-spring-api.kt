package com.abstratt.kirra.spring.api

import com.abstratt.kirra.KirraApplication
import org.springframework.stereotype.Component
import java.io.IOException
import java.net.URI
import com.abstratt.kirra.rest.common.*
import com.abstratt.kirra.rest.resources.*
import com.abstratt.kirra.spring.BaseEntity
import com.sun.xml.internal.fastinfoset.util.StringArray
import org.reflections.Reflections
import org.reflections.util.ConfigurationBuilder
import org.springframework.context.annotation.Lazy
import javax.ws.rs.ApplicationPath
import javax.ws.rs.core.Application
import javax.ws.rs.ext.Provider
import javax.ws.rs.container.ContainerRequestFilter
import javax.ws.rs.container.ContainerResponseFilter
import javax.ws.rs.container.ContainerRequestContext
import javax.ws.rs.container.ContainerResponseContext
import kotlin.reflect.KClass

@Component
@Provider
@Lazy
class KirraRequestFilter : ContainerRequestFilter {
    @Throws(IOException::class)
    override fun filter(requestContext: ContainerRequestContext) {
        print("Request filter *******************")
        KirraContext.setBaseURI(URI.create("/"))
        KirraContext.setSchemaManagement(null)
    }
}


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
@Component
@Lazy
@ApplicationPath("/api/")
class CustomJaxRsApplication: KirraJaxRsApplication() {
    override fun getClasses(): MutableSet<Class<*>> {
        val classes = LinkedHashSet(super.getClasses())
        classes.add(KirraRequestFilter::class.java)
        classes.add(KirraResponseFilter::class.java)
        return classes
    }
}

class KirraSpringApplication(name : String, val javaPackages: Array<String>) : KirraApplication(name) {
    constructor(name : String, javaClasses: Array<KClass<out BaseEntity>>) : this(name, toPackageNames(javaClasses))
}

fun toPackageNames(javaClasses : Array<KClass<out BaseEntity>>) : Array<String> =
    javaClasses.map { it.java.`package`.name }.toSet().toTypedArray()
