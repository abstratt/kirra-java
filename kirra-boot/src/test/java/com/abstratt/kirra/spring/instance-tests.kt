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
    fun testMatchOperation() {
            val orderEntity = schema.allEntities.find { it.name == Order::class.simpleName }!!
            val addItemAction = orderEntity.operations.find { it.name == Order::addItem.name }!!
            val customer = createCustomer("customer1")
            val category = createCategory("category1")
            val product = createProduct("product1", 200.0, category)
            val order = createOrder(customer)
            val match = instanceManagement.matchOperation(addItemAction, order.id.toExternalId(), listOf(product), emptyMap())
            assertNotNull(match)
        }


    @Test
    fun testGetParameterDomain() {
        val orderEntity = schema.allEntities.find { it.name == Order::class.simpleName }!!
        val addItemAction = orderEntity.operations.find { it.name == Order::addItem.name }!!
        val productParameter = addItemAction.parameters.find { it.name == "product" }!!

        val customer = createCustomer("customer1")
        val category = createCategory("category1")
        val order = createOrder(customer)
        val product1 = createProduct("product1", 200.0, category)
        val product2 = createProduct("product2", 10.0, category, false)

        val addItemProductDomain = instanceManagement.getParameterDomain(orderEntity, order.id.toExternalId(), addItemAction, productParameter)
        assertNotNull(addItemProductDomain.find { it.objectId == product1.id.toExternalId() })
        assertNull(addItemProductDomain.find { it.objectId == product2.id.toExternalId() })
    }

    private fun createCategory(categoryName: String) =
                    categoryService.create(Category().apply { name = categoryName })
    private fun createCustomer(name: String) = customerService.create(Customer().apply { this.name = name })
    private fun createOrder(customer: Customer): Order {
            return orderService.create(
                            Order().apply {
                                    this.customer = customer
                                }
                            )
        }

    private fun createProduct(name : String, price : Double, category: Category, available : Boolean = true): Product {
            return productService.create(Product().apply {
                    this.name = name
                    this.price = price
                    this.category = category
                    this.available = available
                })
        }


    private fun Long?.toExternalId(): String? = kirraSpringBridge.toExternalId(this)
}