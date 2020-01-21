package com.abstratt.kirra.spring

import com.abstratt.kirra.pojo.IBaseEntity
import org.springframework.data.jpa.repository.support.SimpleJpaRepository
import javax.persistence.EntityManager
import kotlin.reflect.KClass

open class GenericRepository<E : IBaseEntity>(entityClass: KClass<E>, entityManager: EntityManager)
    : BaseRepository<E>, SimpleJpaRepository<E, Long>(entityClass.java, entityManager)

