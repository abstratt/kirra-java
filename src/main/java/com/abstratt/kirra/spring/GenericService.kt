package com.abstratt.kirra.spring

import org.springframework.data.jpa.repository.JpaRepository
import kotlin.reflect.KClass

class GenericService(entityClass : KClass<BaseEntity>) : BaseService<BaseEntity, JpaRepository<BaseEntity, Long>>(entityClass){
}