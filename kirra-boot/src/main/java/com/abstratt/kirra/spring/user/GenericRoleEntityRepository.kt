package com.abstratt.kirra.spring.user

import com.abstratt.kirra.spring.GenericRepository
import javax.persistence.EntityManager
import kotlin.reflect.KClass

class GenericRoleEntityRepository<RE : RoleEntity>(entityClass : KClass<RE>, entityManager : EntityManager)
    : GenericRepository<RE>(entityClass, entityManager)