package com.abstratt.kirra.spring

import com.abstratt.kirra.pojo.IBaseEntity
import org.springframework.data.jpa.repository.JpaRepository
import kotlin.reflect.KClass

class GenericService<E : IBaseEntity>(entityClass : KClass<E>) : BaseService<E, JpaRepository<E, Long>>(entityClass){
}