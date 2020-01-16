package com.abstratt.kirra.spring

import org.springframework.data.jpa.repository.support.SimpleJpaRepository
import javax.persistence.EntityManager
import kotlin.reflect.KClass

open class GenericRepository<E : BaseEntity>(entityClass: KClass<E>, entityManager: EntityManager)
    : BaseRepository<E>, SimpleJpaRepository<E, Long>(entityClass.java, entityManager)

