package com.abstratt.kirra.spring.testing.sample

import com.abstratt.kirra.spring.*
import com.abstratt.kirra.spring.user.RoleEntity
import com.abstratt.kirra.spring.user.UserRole
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.data.domain.Page
import org.springframework.stereotype.Repository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import javax.persistence.*

interface SampleMarker

@SpringBootApplication
open class SampleApplication : KirraJavaApplication(SampleRole.values().asIterable())


enum class SampleRole : UserRole {
    Customer, Employee;
}

@Entity
@Named(description = "Products that can be added to a car")
class Product : BaseEntity() {
    @Column(nullable = false)
    var name: String? = null
    @Column(nullable = false)
    var price: Double? = null
    @Column(nullable = true)
    var category: String? = null
}

@Entity
class OrderItem(
        @ManyToOne(optional = false, cascade = arrayOf(CascadeType.ALL))
        var order: Order? = null,
        @ManyToOne(optional = false)
        var product: Product? = null,
        @Column(nullable = false)
        var quantity: Int? = 1
) : BaseEntity() {
    val price get() = (this.product?.price ?: 0.0) * (this.quantity ?: 0)
}

enum class OrderStatus {
    Open, Closed, Canceled
}

@Entity
class Order(override var id: Long? = null) : BaseEntity(id) {
    @Enumerated(EnumType.STRING)
    var status : OrderStatus? = OrderStatus.Open

    @ManyToOne(cascade = arrayOf(CascadeType.ALL), optional = false)
    var customer: Customer? = null

    @OneToMany(orphanRemoval = true, mappedBy = "order")
    var items: MutableCollection<OrderItem> = ArrayList()

    fun addItem(product: Product, quantity: Int): OrderItem {
        val newItem = OrderItem(order = this, product = product, quantity = quantity)
        items.add(newItem)
        return newItem
    }
}

@Repository
interface OrderRepository : BaseRepository<Order> {
    fun findAllByStatus(toMatch : OrderStatus) : Iterable<Order>
}

@Service
open class OrderService : BaseService<Order, OrderRepository>(Order::class) {
    @QueryOperation
    open fun byStatus(toMatch : OrderStatus) = repository.findAllByStatus(toMatch)
}

@Entity
abstract class Person(
        override var id: Long? = null,
        @Column(unique = true)
        var name: String? = null
) : RoleEntity(id)


@Entity
class Employee : Person() {
    override fun getRole(): UserRole = SampleRole.Employee
}

@Entity
class Customer : Person() {
    @OneToMany(orphanRemoval = false, mappedBy = "customer")
    var orders: MutableCollection<Order> = ArrayList()

    override fun getRole(): UserRole = SampleRole.Customer
}

@Entity
class Transfer(
        override var id: Long? = null,
        @ManyToOne
        var source: Account? = null,
        @ManyToOne
        var destination: Account? = null
) : BaseEntity(id)

@Entity
class Account(
        override var id: Long? = null,
        @ManyToOne
        var owner: Customer? = null,
        var balance: Double? = null,
        @OneToMany(mappedBy = "source")
        var sent: Collection<Transfer>? = null,
        @OneToMany(mappedBy = "destination")
        var received: Collection<Transfer>? = null,
        @Enumerated(value = EnumType.STRING)
        var type: AccountType? = null
) : BaseEntity(id)

enum class AccountType {
    Checking, Savings
}