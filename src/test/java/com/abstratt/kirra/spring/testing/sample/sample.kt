package com.abstratt.kirra.spring.testing.sample

import com.abstratt.kirra.spring.BaseEntity
import com.abstratt.kirra.spring.Named
import com.abstratt.kirra.spring.user.RoleEntity
import javax.persistence.*

interface SampleMarker

@Entity
@Named(description = "Products that can be added to a car")
class Product : BaseEntity() {
    @Column(nullable = false)
    var name : String? = null
    @Column(nullable = false)
    var price : Double? = null
}

@Entity
class OrderItem (
        @ManyToOne(optional = false, cascade = arrayOf(CascadeType.ALL))
        var order : Order? = null,
        @ManyToOne(optional = false)
        var product : Product? = null,
        @Column(nullable = false)
        var quantity : Int? = 1
) : BaseEntity() {
    val price get() = (this.product?.price ?: 0.0) * (this.quantity ?: 0)
}

@Entity
class Order(override var id: Long? = null) : BaseEntity(id) {
    @ManyToOne(cascade = arrayOf(CascadeType.ALL), optional = false)
    var customer : Person? = null
    @OneToMany(orphanRemoval = true, mappedBy = "order")
    var items : MutableCollection<OrderItem> = ArrayList()

    fun addItem(product : Product, quantity : Int) : OrderItem {
        val newItem = OrderItem(order = this, product = product, quantity = quantity)
        items.add(newItem)
        return newItem
    }
}

@Entity
abstract class Person(
    override var id: Long? = null,
    @Column(unique = true)
    var name: String? = null
) : RoleEntity(id)

@Entity
class Transfer(
        override var id: Long? = null,
        @ManyToOne
    var source : Account? = null,
        @ManyToOne
    var destination : Account? = null
) : BaseEntity(id)

@Entity
class Account(
        override var id: Long? = null,
        @ManyToOne
    var owner : Person? = null,
        var balance : Double? = null,
        @OneToMany(mappedBy="source")
    var sent : Collection<Transfer>? = null,
        @OneToMany(mappedBy="destination")
    var received : Collection<Transfer>? = null,
        @Enumerated(value = EnumType.STRING)
    var type : AccountType? = null
) : BaseEntity(id)

enum class AccountType {
    Checking, Savings
}