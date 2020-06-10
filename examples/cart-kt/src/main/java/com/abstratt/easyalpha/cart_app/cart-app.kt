package com.abstratt.easyalpha.cart_app

import com.abstratt.easyalpha.cart.ShoppingCartMarker
import com.abstratt.easyalpha.cart.ShoppingCartRole
import com.abstratt.kirra.spring.KirraApplicationConfiguration
import com.abstratt.kirra.spring.KirraJavaApplication
import com.abstratt.kirra.spring.KirraSpringMarker
import com.abstratt.kirra.spring.api.KirraSpringAPIMarker
import com.abstratt.kirra.spring.runApplication
import com.abstratt.kirra.spring.userprofile.UserProfileMarker

@KirraApplicationConfiguration([
    UserProfileMarker::class,
    KirraSpringMarker::class,
    KirraSpringAPIMarker::class,
    ShoppingCartMarker::class
])
class ShoppingApplication : KirraJavaApplication(ShoppingCartRole.values().toSet())


fun main(args: Array<String>) {
    assert(System.getProperty("spring.profiles.active") != null)
    runApplication(ShoppingApplication::class, args)
}


