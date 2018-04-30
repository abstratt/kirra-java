package com.abstratt.kirra.spring

import com.abstratt.kirra.Operation
import com.abstratt.kirra.Relationship
import com.abstratt.kirra.Schema
import com.abstratt.kirra.TypeRef
import com.abstratt.kirra.spring.testing.sample.*
import com.abstratt.kirra.spring.userprofile.UserProfile
import org.apache.commons.lang3.StringUtils
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import kotlin.reflect.full.findAnnotation


open class SchemaTests : TestBase() {
    /**
     * Ensures we can find constraint classes in the classpath.
     */
    @Test
    fun testClassScanning() {
        val namespaces = schema.namespaces
        assertEquals(2, namespaces.size)
        namespaces.sortBy { it.name }
        assertEquals("sample", namespaces[0].name)
        assertEquals("userprofile", namespaces[1].name)
        val entities = namespaces[0].entities
        assertTrue(entities.size >= 1)
        val order = entities.find { it.name == Order::class.simpleName }
        assertNotNull(order)
    }

    @Test
    fun testEntity() {
        val entity = schema.allEntities.find { it.name == Product::class.simpleName }
        assertNotNull(entity)
        val productEntity = entity!!
        val expectedTypeRef = getTypeRef(Product::class.java, TypeRef.TypeKind.Entity)
        assertEquals(expectedTypeRef, productEntity.typeRef)
        assertEquals("sample", productEntity.entityNamespace)
        assertTrue(productEntity.isTopLevel)
        assertTrue(productEntity.isStandalone)
        assertTrue(productEntity.isInstantiable)
        assertTrue(productEntity.isConcrete)
    }

    @Test
    fun testEntity_otherPackages() {
        val entity = schema.allEntities.find { it.name == UserProfile::class.simpleName }
        assertNotNull(entity)
    }

    @Test
    fun testProperties() {
        val entity = schema.allEntities.find { it.name == Product::class.simpleName }
        assertNotNull(entity)
        val properties = entity!!.properties
        assertEquals(3, properties.size)

        val nameProperty = properties.find { it.name == Product::name.name }!!
        assertEquals("String", nameProperty.type)
        assertEquals(TypeRef.TypeKind.Primitive, nameProperty.typeRef.kind)
        assertEquals("kirra.String", nameProperty.typeRef.fullName)
        assertTrue(nameProperty.isRequired)

        val priceProperty = properties.find { it.name == Product::price.name }!!
        assertEquals("Double", priceProperty.type)
        assertEquals(TypeRef.TypeKind.Primitive, priceProperty.typeRef.kind)
        assertEquals("kirra.Double", priceProperty.typeRef.fullName)
        assertTrue(priceProperty.isRequired)

        val monikerProperty = properties.find { it.name == Product::moniker.name }!!
        assertEquals("String", monikerProperty.type)
        assertEquals(TypeRef.TypeKind.Primitive, monikerProperty.typeRef.kind)
        assertEquals("kirra.String", monikerProperty.typeRef.fullName)
        assertFalse(monikerProperty.isRequired)
        assertTrue(monikerProperty.isDerived)
    }

    @Test
    fun testRelationships_categoryProducts() {
        val categoryEntity = schema.allEntities.first { it.name == Category::class.simpleName }
        val productsRelationship = categoryEntity.getRelationship("products")
        assertNotNull(productsRelationship)
        assertEquals(Product::class.simpleName, productsRelationship.typeRef.typeName)
    }

    @Test
    fun testRelationships_profileAsEmployee() {
        val profileEntity = schema.allEntities.first { it.name == UserProfile::class.simpleName }
        val asEmployeeRelationship = profileEntity.getRelationship("roleAsEmployee")
        assertNotNull(asEmployeeRelationship)
        assertEquals(Employee::class.simpleName, asEmployeeRelationship.typeRef.typeName)
        assertFalse(asEmployeeRelationship.isMultiple)
        assertFalse(asEmployeeRelationship.isRequired)
    }

    @Test
    fun testQueries() {
        val entity = schema.allEntities.find { it.name == Order::class.simpleName }
        assertNotNull(entity)
        val entityOperations = entity!!.operations

        val queries = entityOperations.filter { it.kind == Operation.OperationKind.Finder }
        assertEquals(1, queries.size)

        val byStatusQuery = queries.find { it.name == OrderService::byStatus.name }
        assertNotNull(byStatusQuery)
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
        val expectedEnumeration = getTypeRef(AccountType::class.java, TypeRef.TypeKind.Enumeration)
        assertEquals(expectedEnumeration, accountType.typeRef)
        assertEquals(listOf("Checking", "Savings"), accountType.enumerationLiterals.map { it.key })
    }


    @Test
    fun testRelationship_OrderCustomer() {
        val order = schema.allEntities.find { it.name == Order::class.simpleName }!!
        val person = schema.allEntities.find { it.name == Customer::class.simpleName }!!
        val customerRelationship = order.relationships.find { it.name == Order::customer.name }!!
        assertEquals(person.typeRef, customerRelationship.typeRef)
        assertTrue(customerRelationship.isRequired)
        assertFalse(customerRelationship.isMultiple)
        assertEquals(Relationship.Style.LINK, customerRelationship.style)
    }

    @Test
    fun testInstanceAction() {
        val order = schema.allEntities.find { it.name == Order::class.simpleName }!!
        val addItemAction = order.operations.find { it.name == Order::addItem.name }
        assertNotNull(addItemAction)
        assertTrue(addItemAction!!.isInstanceOperation)
        assertEquals(Operation.OperationKind.Action, addItemAction!!.kind)
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