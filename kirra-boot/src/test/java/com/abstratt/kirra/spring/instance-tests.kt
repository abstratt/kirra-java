package com.abstratt.kirra.spring

import com.abstratt.kirra.InstanceManagement
import com.abstratt.kirra.spring.testing.sample.*
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test


open class InstanceTests : TestBase() {

    @Test
    fun testGetCurrentUser() {
        securityService.selectedUsername = employee1!!.user!!.username
        val currentUser = instanceManagement.currentUser
        assertNotNull(currentUser)

        val userProfile = instanceManagement.getInstance(currentUser!!.reference, InstanceManagement.DataProfile.Full)
        assertNotNull(userProfile.links[EmployeeService::roleAsEmployee.name])
    }

    @Test
    fun testGetParameterDomain() {
        val orderEntity = schema.allEntities.find { it.name == Order::class.simpleName }!!
        val addItemAction = orderEntity.operations.find { it.name == Order::addItem.name }!!
        val productParameter = addItemAction.parameters.find { it.name == "product" }!!

        val customer = customerService.create(Customer().apply { name = "customer1" })
        val category = categoryService.create(Category().apply { name = "category1" })
        val order = orderService.create(
            Order().apply {
                this.customer = customer
            }
        )
        val product1 = productService.create(Product().apply {
                name = "product1"
                price = 10.0
                this.category = category
            }
        )
        val product2 = productService.create(
            Product().apply {
                name = "product2"
                price = 10.0
                this.category = category
                available = false
            }
        )
        val addItemProductDomain = instanceManagement.getParameterDomain(orderEntity, order.id.toExternalId(), addItemAction, productParameter)
        assertNotNull(addItemProductDomain.find { it.objectId == product1.id.toExternalId() })
        assertNull(addItemProductDomain.find { it.objectId == product2.id.toExternalId() })
    }

    private fun Long?.toExternalId(): String? = kirraSpringBridge.toExternalId(this)
}