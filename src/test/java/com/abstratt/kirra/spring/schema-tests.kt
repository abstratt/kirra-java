package com.abstratt.kirra.spring

import com.abstratt.kirra.Relationship
import com.abstratt.kirra.Schema
import com.abstratt.kirra.TypeRef
import com.abstratt.kirra.spring.testing.sample.*
import org.apache.commons.lang3.StringUtils
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import kotlin.reflect.full.findAnnotation


@RunWith(SpringJUnit4ClassRunner::class)
@ContextConfiguration(classes = arrayOf(TestConfig::class))
class SchemaTests {

    @Autowired
    private lateinit var schema: Schema

    @Autowired
    private lateinit var kirraSpringMetamodel : KirraSpringMetamodel

    /**
     * Ensures we can find constraint classes in the classpath.
     */
    @Test
    fun testClassScanning() {
        val namespaces = schema.namespaces
        assertEquals(2, namespaces.size)
        namespaces.sortBy { it.name }
        assertEquals("sample", namespaces[0].name)
        assertEquals("user", namespaces[1].name)
        val entities = namespaces[0].entities
        assertTrue(entities.size >= 1)
        val order = entities.find { it.name == Person::class.simpleName }
        assertNotNull(order)
    }

    @Test
    fun testEntity() {
        val entity = schema.allEntities.find { it.name == Person::class.simpleName }
        assertNotNull(entity)
        val personEntity = entity!!
        val expectedTypeRef = kirraSpringMetamodel.getTypeRef(Person::class.java, TypeRef.TypeKind.Entity)
        assertEquals(expectedTypeRef, personEntity.typeRef)
        assertEquals("sample", personEntity.entityNamespace)
        val properties = personEntity.properties
        assertEquals(1, properties.size)
        assertEquals(Person::name.name, properties[0].name)
        assertEquals("String", properties[0].type)
        assertEquals(TypeRef.TypeKind.Primitive, properties[0].typeRef.kind)
        assertEquals("kirra.String", properties[0].typeRef.fullName)
    }

    @Test
    fun testNamed() {
        val entity = schema.allEntities.find { it.name == Product::class.simpleName }!!
        val namedAnnotation : Named = Product::class.findAnnotation()!!
        assertNotNull(namedAnnotation)
        assertNotNull(StringUtils.trimToNull(namedAnnotation.description))
        assertEquals(namedAnnotation?.description, entity.description)
    }

    @Test
    fun testEnumeration() {
        val accountEntity = schema.allEntities.find { it.name == Account::class.simpleName }!!
        val accountType = accountEntity.getProperty(Account::type.name)
        assertNotNull(accountType)
        assertEquals(TypeRef.TypeKind.Enumeration, accountType.typeRef.kind)
        val expectedEnumeration = kirraSpringMetamodel.getTypeRef(AccountType::class.java, TypeRef.TypeKind.Enumeration)
        assertEquals(expectedEnumeration, accountType.typeRef)
        assertEquals(listOf("Checking", "Savings"), accountType.enumerationLiterals.map { it.key })
    }


    @Test
    fun testRelationship_OrderCustomer() {
        val order = schema.allEntities.find { it.name == Order::class.simpleName }!!
        val person = schema.allEntities.find { it.name == Person::class.simpleName }!!
        val customerRelationship = order.relationships.find { it.name == Order::customer.name }!!
        assertEquals(person.typeRef, customerRelationship.typeRef)
        assertTrue(customerRelationship.isRequired)
        assertFalse(customerRelationship.isMultiple)
        assertEquals(Relationship.Style.LINK, customerRelationship.style)
    }

    @Test
    fun testInstanceAction() {
        val order = schema.allEntities.find { it.name == Order::class.simpleName }!!
        val addItemAction = order.operations.find { it.name == Order::addItem.name }!!
    }


    @Test
    fun testRelationship_Child() {
        val order = schema.allEntities.find { it.name == Order::class.simpleName }!!
        val orderItem = schema.allEntities.find { it.name == OrderItem::class.simpleName }!!
        val itemsRelationship = order.relationships.find { it.name == Order::items.name }!!
        assertEquals(orderItem.typeRef, itemsRelationship.typeRef)
        assertFalse(itemsRelationship.isRequired)
        assertTrue(itemsRelationship.isMultiple)
        assertEquals(Relationship.Style.CHILD, itemsRelationship.style!!)
    }

    @Test
    fun testRelationship_Parent() {
        val order = schema.allEntities.find { it.name == Order::class.simpleName }!!
        val orderItem = schema.allEntities.find { it.name == OrderItem::class.simpleName }!!
        val orderRelationship = orderItem.getRelationship(OrderItem::order.name)!!
        val itemsRelationship = order.getRelationship(Order::items.name)!!

        assertEquals(order.typeRef, orderRelationship.typeRef)
        assertTrue(orderRelationship.isRequired)
        assertFalse(orderRelationship.isMultiple)
        assertEquals(Relationship.Style.PARENT, orderRelationship.style!!)

        assertEquals(orderRelationship.name, itemsRelationship.opposite)
        assertEquals(itemsRelationship.name, orderRelationship.opposite)

        assertTrue(itemsRelationship.isOppositeRequired)
        assertTrue(itemsRelationship.isOppositeReadOnly)

        assertFalse(orderRelationship.isOppositeRequired)
        assertFalse(orderRelationship.isOppositeReadOnly)
    }

    @Test
    fun testRelationship_AccountTransfer() {
        val transfer = schema.allEntities.find { it.name == Transfer::class.simpleName }!!
        val account = schema.allEntities.find { it.name == Account::class.simpleName }!!
        val sentRelationship = account.getRelationship(Account::sent.name)!!
        val receivedRelationship = account.getRelationship(Account::received.name)!!
        val sourceRelationship = transfer.getRelationship(Transfer::source.name)!!
        val destinationRelationship = transfer.getRelationship(Transfer::destination.name)!!

        assertEquals(sourceRelationship.name, sentRelationship.opposite)
        assertEquals(sentRelationship.name, sourceRelationship.opposite)
        assertEquals(destinationRelationship.name, receivedRelationship.opposite)
        assertEquals(receivedRelationship.name, destinationRelationship.opposite)
        assertEquals(Relationship.Style.LINK, sentRelationship.style)
        assertEquals(Relationship.Style.LINK, sourceRelationship.style)
        assertEquals(Relationship.Style.LINK, receivedRelationship.style)
        assertEquals(Relationship.Style.LINK, destinationRelationship.style)
    }
}