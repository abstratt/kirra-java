package com.abstratt.kirra.spring

import com.abstratt.kirra.*
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


open class InstanceTests : TestBase() {

    @Test
    fun testGetCurrentUser() {
        securityService.selectedUsername = employee1!!.user!!.username
        val currentUser = instanceManagement.currentUser
        assertNotNull(currentUser)

        val userProfile = instanceManagement.getInstance(currentUser!!.reference, InstanceManagement.DataProfile.Full)
        assertNotNull(userProfile.links[EmployeeService::roleAsEmployee.name])
    }

}