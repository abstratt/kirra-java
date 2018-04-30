package com.abstratt.kirra.spring

import com.abstratt.kirra.Schema
import com.abstratt.kirra.spring.testing.sample.Customer
import com.abstratt.kirra.spring.testing.sample.Employee
import com.abstratt.kirra.spring.userprofile.UserProfile
import org.junit.Before
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.annotation.Rollback
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import org.springframework.transaction.annotation.Transactional

@RunWith(SpringJUnit4ClassRunner::class)
@ContextConfiguration(classes = arrayOf(TestConfig::class))
@Transactional
@ActiveProfiles("test")
abstract class TestBase {
    @Autowired
    protected lateinit var schema: Schema

    @Autowired
    protected lateinit var kirraSpringMetamodel: KirraSpringMetamodel
    @Autowired
    protected lateinit var kirraSpringBridge: KirraSpringInstanceBridge

    @Autowired
    protected lateinit var instanceManagement: KirraSpringInstanceManagement

    @Autowired
    protected lateinit var securityService: TestSecurityService

    @Autowired
    protected lateinit var userProfileService: BaseService<UserProfile,*>
    @Autowired()
    private lateinit var customerService: BaseService<Customer,*>
    @Autowired
    private lateinit var employeeService: BaseService<Employee,*>

    protected var user1: UserProfile? = null
    protected var user2: UserProfile? = null
    protected var user3: UserProfile? = null
    protected var user4: UserProfile? = null
    protected var customer1: Customer? = null
    protected var customer2: Customer? = null
    protected var employee1: Employee? = null
    protected var employee2: Employee? = null

    @Before
    @Rollback(false)
    @Transactional()
    fun setUp(): Unit {
        user1 = userProfileService.create(UserProfile(username = "user1", password = ""))
        user2 = userProfileService.create(UserProfile(username = "user2", password = ""))
        user3 = userProfileService.create(UserProfile(username = "user3", password = ""))
        user4 = userProfileService.create(UserProfile(username = "user4", password = ""))
        customer1 = customerService.create(Customer(name = "John", user = user1))
        customer2 = customerService.create(Customer(name = "Mary", user = user2))
        employee1 = employeeService.create(Employee(name = "Peter", user = user3))
        employee2 = employeeService.create(Employee(name = "Sheila", user = user4))
    }
}