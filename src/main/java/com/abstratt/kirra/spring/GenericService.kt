package com.abstratt.kirra.spring

import org.springframework.data.jpa.repository.JpaRepository
import kotlin.reflect.KClass

class GenericService<E : BaseEntity>(entityClass : KClass<E>) : BaseService<E, JpaRepository<E, Long>>(entityClass){
}