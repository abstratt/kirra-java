package com.abstratt.kirra.spring

import com.abstratt.kirra.Schema
import com.abstratt.kirra.spring.testing.sample.Customer
import com.abstratt.kirra.spring.testing.sample.Employee
import com.abstratt.kirra.spring.userprofile.UserProfile
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import javax.persistence.EntityManager
import javax.persistence.PersistenceContext

open class AccessControlTests : TestBase() {
    @Autowired
    private lateinit var schema: Schema

    @Autowired
    private lateinit var kirraSpringMetamodel: KirraSpringMetamodel
    @Autowired
    private lateinit var kirraSpringBridge: KirraSpringInstanceBridge

    @Autowired
    private lateinit var instanceManagement: KirraSpringInstanceManagement

    @Autowired
    private lateinit var securityService: TestSecurityService


    @PersistenceContext
    private lateinit var entityManager: EntityManager

    private var user1: UserProfile? = null
    private var user2: UserProfile? = null
    private var user3: UserProfile? = null
    private var user4: UserProfile? = null
    private var customer1: Customer? = null
    private var customer2: Customer? = null
    private var employee1: Employee? = null
    private var employee2: Employee? = null

    @Before
    fun setUp(): Unit {
        user1 = persist(UserProfile(username = "user1", password = ""))
        user2 = persist(UserProfile(username = "user2", password = ""))
        user3 = persist(UserProfile(username = "user3", password = ""))
        user4 = persist(UserProfile(username = "user4", password = ""))
        customer1 = persist(Customer(name = "John", user = user1))
        customer2 = persist(Customer(name = "Mary", user = user2))
        employee1 = persist(Employee(name = "Peter", user = user3))
        employee2 = persist(Employee(name = "Sheila", user = user4))
        entityManager.flush()
    }

    private fun <E : BaseEntity> persist(e: E): E? {
        entityManager.persist(e)
        return e
    }

    @Test
    fun customerInstanceCapabilities(): Unit {
        val customerAsInstance1 = kirraSpringBridge.toInstance(customer1!!)

        securityService.selectedUsername = customer1!!.user!!.username

        val instanceCapabilities = instanceManagement.getInstanceCapabilities(customerAsInstance1.typeRef, customerAsInstance1.objectId)
        Assert.assertEquals(sortedSetOf("Read", "Update"), instanceCapabilities.instance.toSortedSet())

        securityService.selectedUsername = customer2!!.user!!.username

        val instanceCapabilities2 = instanceManagement.getInstanceCapabilities(customerAsInstance1.typeRef, customerAsInstance1.objectId)
        Assert.assertEquals(sortedSetOf("Read"), instanceCapabilities2.instance.toSortedSet())
    }

    @Test
    fun customerEntityCapabilities(): Unit {
        securityService.selectedUsername = employee1!!.user!!.username

        val customerTypeRef = getTypeRef(Customer::class.java)
        val capabilities = instanceManagement.getEntityCapabilities(customerTypeRef)
        Assert.assertEquals(sortedSetOf("Create", "List"), capabilities.entity.toSortedSet())

        Assert.assertEquals(sortedSetOf("StaticCall"), capabilities.queries["allCustomers"]!!.toSortedSet())

        securityService.selectedUsername = customer1!!.user!!.username

        val capabilities2 = instanceManagement.getEntityCapabilities(customerTypeRef)
        Assert.assertEquals(sortedSetOf("List"), capabilities2.entity.toSortedSet())

        Assert.assertEquals(sortedSetOf("StaticCall"), capabilities.queries["allCustomers"]!!.toSortedSet())
    }

    @Test
    fun categoryCapabilities(): Unit {
        securityService.selectedUsername = employee1!!.user!!.username

        val userProfileTypeRef = getTypeRef(UserProfile::class.java)
        val entityCapabilities = instanceManagement.getEntityCapabilities(userProfileTypeRef)
        Assert.assertEquals(sortedSetOf("Create", "List"), entityCapabilities.entity.toSortedSet())

        val instanceCapabilities = instanceManagement.getInstanceCapabilities(userProfileTypeRef, user1!!.id.toString())
        Assert.assertEquals(sortedSetOf("Delete", "Read", "Update"), instanceCapabilities.instance.toSortedSet())
    }
}