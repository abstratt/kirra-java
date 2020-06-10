package com.abstratt.kirra.spring.testing.sample

import com.abstratt.easyalpha.cart.Customer
import com.abstratt.easyalpha.cart.ShoppingCartMarker
import com.abstratt.easyalpha.cart.ShoppingCartRole
import com.abstratt.kirra.spring.*
import com.abstratt.kirra.spring.api.KirraSpringAPIMarker
import com.abstratt.kirra.spring.userprofile.UserProfileMarker
import org.springframework.stereotype.Repository
import javax.persistence.*


@KirraApplicationConfiguration(basePackageClasses = [UserProfileMarker::class, KirraSpringMarker::class, KirraSpringAPIMarker::class, SampleMarker::class, ShoppingCartMarker::class])
open class SampleApplication : KirraJavaApplication(ShoppingCartRole.values().asIterable())

interface SampleMarker

@Repository
interface AccountRepository : BaseRepository<Account>

@Repository
interface TransferRepository : BaseRepository<Transfer>

@Entity
class Transfer(
        id: Long? = null,
        @ManyToOne
        var source: Account? = null,
        @ManyToOne
        var destination: Account? = null
) : BaseEntity(id)

@Entity
class Account(
        id: Long? = null,
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