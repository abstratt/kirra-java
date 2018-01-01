/*
package com.abstratt.easyalpha.cart

import com.abstratt.kirra.kt.Children
import java.util.stream.Stream
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.context.annotation.Configuration
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.transaction.annotation.EnableTransactionManagement


@Entity
class Cart {
    val items = Children.of(CartItem::class)

    fun addItem(product: Product, quantity: Int) : CartItem {
        // TODO: add item to list of items
        return CartItem(this, product, quantity)
    }

    companion object {
        fun mine() : Cart? {
            return Cart()
        }
    }

}

@Entity
class CartItem (
    val cart : Cart,
    val product : Product,
    var quantity : Int
) {
    val price : Double?
        get() = (this.product?.price ?: 0.0) * (this.quantity ?: 0)
}


@Entity
class Product (
    var name : String? = null,
    var price : Double? = null,
    var category : ProductCategory? = null,
    var available : Boolean? = null
) {
    companion object {
        fun available() : Stream<Product> {
            return Stream.of(Product())
        }
    }
}

@Entity
class ProductCategory (
    var name : String? = null
)

@Configuration
@EnableJpaRepositories(basePackages = arrayOf("com.abstratt.easyalpha.cart"))
@EntityScan(basePackages = arrayOf("com.abstratt.easyalpha.cart"))
@EnableTransactionManagement
open class CartAppConfig

@SpringBootApplication
open class CartApp

fun main(args: Array<String>) {
    SpringApplication.run(CartApp::class.java, *args)
}
*/
