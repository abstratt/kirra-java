package com.abstratt.kirra.spring

import com.abstratt.easyalpha.cart.Customer
import com.abstratt.easyalpha.cart.Employee
import com.abstratt.easyalpha.cart.Order
import com.abstratt.kirra.pojo.*
import org.junit.Assert
import org.junit.Test

class AccessControlUT {
    @Test
    fun entity() {
        var capabilities = computeCapabilities(null,
                listOf(Employee()),
                listOf(CapabilityTarget.Entity),
                ConstraintLayer(constraint<Order, Employee>(roles(Employee::class), can(Capability.Create)))
        ).toSet()
        Assert.assertEquals(setOf(Capability.Create), capabilities)
    }

    @Test
    fun entity_multiple() {
        var capabilities = computeCapabilities(null,
                listOf(Employee()),
                listOf(CapabilityTarget.Entity),
                ConstraintLayer(
                        constraint<Order, Employee>(roles(Employee::class), can(Capability.Create)),
                        constraint<Order, Employee>(roles(Employee::class), can(Capability.List))
                )
        ).toSet()
        Assert.assertEquals(setOf(Capability.Create, Capability.List), capabilities)
    }

    @Test
    fun entity_default() {
        var capabilities = computeCapabilities(null,
                listOf(Employee()),
                listOf(CapabilityTarget.Entity)
        ).toSet()
        Assert.assertEquals(setOf(Capability.Create, Capability.List), capabilities)
    }

    @Test
    fun entity_all() {
        var capabilities = computeCapabilities(null,
                listOf(Employee()),
                listOf(CapabilityTarget.Entity),
                ConstraintLayer(constraint<Order, Employee>(roles(Employee::class), can(*ALL_CAPABILITIES)))
        ).toSet()
        Assert.assertEquals(setOf(Capability.Create, Capability.List), capabilities)
    }

    @Test
    fun entity_noConstraintForRole() {
        var capabilities = computeCapabilities(null,
                listOf(Customer()),
                listOf(CapabilityTarget.Entity),
                ConstraintLayer(constraint<Order, Employee>(roles(Employee::class), can(Capability.Create)))
        ).toSet()
        Assert.assertEquals(emptySet<Capability>(), capabilities)
    }

    @Test
    fun operation() {
        var capabilities = computeCapabilities(null,
                listOf(Employee()),
                listOf(CapabilityTarget.Operation),
                ConstraintLayer(constraint<Order, Employee>(roles(Employee::class), can(Capability.Create))),
                ConstraintLayer(constraint<Order, Employee>(roles(Employee::class), can(Capability.Call)))
        ).toSet()
        Assert.assertEquals(setOf(Capability.Call), capabilities)
    }


    @Test
    fun operation_removed() {
        var capabilities = computeCapabilities(null,
                listOf(Employee()),
                listOf(CapabilityTarget.Operation),
                ConstraintLayer(constraint<Order, Employee>(roles(Employee::class), can(Capability.Call))),
                ConstraintLayer(constraint<Order, Employee>(roles(Employee::class), can(Capability.None)))
        ).toSet()
        Assert.assertEquals(emptySet<Capability>(), capabilities)
    }

    @Test
    fun property_inherited() {
        var capabilities = computeCapabilities(null,
                listOf(Employee()),
                listOf(CapabilityTarget.Property),
                ConstraintLayer(
                        constraint<Order, Employee>(roles(Employee::class), can(Capability.Read)),
                        constraint<Order, Employee>(roles(Employee::class), can(Capability.Update))
                )
        ).toSet()
        Assert.assertEquals(setOf(Capability.Read, Capability.Update), capabilities)
    }

    @Test
    fun property_restricted() {
        var capabilities = computeCapabilities(null,
                listOf(Employee()),
                listOf(CapabilityTarget.Property),
                ConstraintLayer(constraint<Order, Employee>(roles(Employee::class), can(Capability.Read, Capability.Update))),
                ConstraintLayer(constraint<Order, Employee>(roles(Employee::class), can(Capability.Read)))
        ).toSet()
        Assert.assertEquals(setOf(Capability.Read), capabilities)
    }


    @Test
    fun operation_all() {
        var capabilities = computeCapabilities(null,
                listOf(Employee()),
                listOf(CapabilityTarget.Operation),
                ConstraintLayer(constraint<Order, Employee>(roles(Employee::class), can(*ALL_CAPABILITIES)))
        ).toSet()
        Assert.assertEquals(setOf(Capability.Call), capabilities)
    }
}