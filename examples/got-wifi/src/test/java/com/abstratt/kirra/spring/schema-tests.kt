package com.abstratt.kirra.spring

import com.abstratt.kirra.spring.api.KirraSpringSchemaBuilder
import org.junit.Test
import org.junit.Assert.*

class SchemaTests {
    @Test
    fun testSimple() {
        val samplePackage = sample.Order::class.java.`package`
        val entities = KirraSpringSchemaBuilder().getEntities(samplePackage)
        assertEquals(1, entities.size)
        assertEquals("Order", entities[0].name)
        assertEquals("sample", entities[0].entityNamespace)
    }
}