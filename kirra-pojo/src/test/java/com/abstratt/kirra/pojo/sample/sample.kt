//package com.abstratt.kirra.pojo.sample
//
//import com.abstratt.kirra.pojo.*
//
//interface SampleMarker
//
////TODO-RC how is the application built?
////@KirraApplicationConfiguration(basePackageClasses = [UserProfileMarker::class, KirraSpringMarker::class, KirraSpringAPIMarker::class, SampleApplication::class])
////open class SampleApplication : KirraJavaApplication(SampleRole.values().asIterable())
//
//
//enum class SampleRole : IUserRole {
//    Customer, Employee
//}
//
//@EntityClass
//class Category : BasePojoEntity() {
//    var name: String? = null
//}
//
//@ServiceClass
//open class CategoryService : BasePojoService<Category>() {
//    @RelationshipAccessor
//    fun products(category : Category) : Collection<Product> =
//        productRepository.findAllByCategory(category)
//}
//
//@EntityClass
//@Named(description = "Products that can be added to a car")
//class Product : BasePojoEntity() {
//    val moniker : String get() = this.name ?: "N/A"
//    var name: String? = null
//    var price: Double? = null
//    var category: Category? = null
//    var available : Boolean? = true
//}
//
//@EntityClass
//class OrderItem(
//        var order: Order? = null,
//        var product: Product? = null,
//        var quantity: Int? = 1
//) : BasePojoEntity() {
//    val price get() = (this.product?.price ?: 0.0) * (this.quantity ?: 0)
//}
//
//enum class OrderStatus {
//    Open, Closed, Canceled
//}
//
//@EntityClass
//class Order(id: Long? = null) : BasePojoEntity(id) {
//    var status : OrderStatus? = OrderStatus.Open
//    var customer: Customer? = null
//    var items: MutableCollection<OrderItem> = ArrayList()
//
//    @ActionOp
//    fun addItem(product: Product, quantity: Int): OrderItem {
//        val newItem = OrderItem(order = this, product = product, quantity = quantity)
//        items.add(newItem)
//        return newItem
//    }
//}
//
//@ServiceClass
//open class OrderService : BasePojoService<Order>() {
//
//    @QueryOp
//    open fun byStatus(toMatch : OrderStatus) = repository.findAllByStatus(toMatch)
//
//    @DomainAccessor
//    fun addItem_product(order : Order, pageRequest: PageRequest?) : Page<Product> =
//        productRepository.findAllByAvailableIsTrue(pageRequest)
//}
//
//@EntityClass
//abstract class Person(
//        id: Long? = null,
//        var name: String? = null,
//        user : IUserProfile? = null
//) : BasePojoEntity(), IRoleEntity
//
//
//@EntityClass
//class Employee(id : Long? = null, name : String? = null, user : IUserProfile? = null) : Person(id, name, user) {
//    override fun getRole(): IUserRole = SampleRole.Employee
//}
//
//@ServiceClass
//open class EmployeeService : IRoleEntityService<Employee>, BasePojoService<Employee>() {
//    @RelationshipAccessor
//    open fun roleAsEmployee(profile : IUserProfile) = findByUser(profile)
//}
//
//@EntityClass
//class Customer(id : Long? = null, name : String? = null, user : IUserProfile? = null) : Person(id, name, user) {
//    var orders: MutableCollection<Order> = ArrayList()
//
//    override fun getRole(): IUserRole = SampleRole.Customer
//
//    object accessControl : AccessControl<Customer, Person>(
//            constraint(
//                    roles(Customer::class),
//                    can(Capability.Update),
//                    provided { e, re -> e == re }
//            ),
//            constraint(
//                    roles(Customer::class),
//                    can(Capability.List, Capability.Read)
//            ),
//            constraint(
//                    roles(Employee::class),
//                    can(Capability.List, Capability.Read, Capability.Create)
//            ),
//            constraint(
//                    CustomerService::allCustomers,
//                    roles(Employee::class),
//                    can(Capability.Call)
//            )
//    )
//}
//
//@ServiceClass
//open class CustomerService : BasePojoService<Customer>() {
//    @QueryOp
//    fun allCustomers(pageable : Pageable? = null) : Page<Customer> = repository.findAll(pageable)
//
//    @RelationshipAccessor
//    open fun roleAsCustomer(profile : IUserProfile) = repository.findByUser(profile)
//}
//
//@EntityClass
//class Transfer(
//        id: Long? = null,
//        var source: Account? = null,
//        var destination: Account? = null
//) : BasePojoEntity(id)
//
//@EntityClass
//class Account(
//        id: Long? = null,
//        var owner: Customer? = null,
//        var balance: Double? = null,
//        var sent: Collection<Transfer>? = null,
//        var received: Collection<Transfer>? = null,
//        var type: AccountType? = null
//) : BasePojoEntity(id)
//
//enum class AccountType {
//    Checking, Savings
//}