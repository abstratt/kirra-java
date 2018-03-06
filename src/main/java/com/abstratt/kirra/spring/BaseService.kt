package com.abstratt.kirra.spring;

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.Page
import org.springframework.data.jpa.domain.Specification
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.transaction.annotation.Transactional
import kotlin.reflect.KClass

abstract class BaseService<T : BaseEntity, R : JpaRepository<T, Long>>(open val entityClass: KClass<T>) {
    @Autowired
    lateinit open var repository : R

    open fun findById(id: Long): T? {
        return repository.findById(id).orElse(null)
    }

    @Transactional
    open fun create(toCreate: T): T {
        toCreate.id = null
        val savedInstance = repository.save(toCreate)
        return repository.getOne(savedInstance.id)
    }

    @Transactional
    open fun update(toUpdate: T): T? {
        checkNotNull(toUpdate.id, { "id was missing" })
        val existingInstance = repository.findById(toUpdate.id)
        if (!existingInstance.isPresent)
            return null
        return repository.save(toUpdate)
    }

    @Transactional
    open fun delete(id: Long): Boolean {
        val existingInstance = repository.existsById(id)
        if (existingInstance) {
            repository.deleteById(id)
            return true
        }
        return false
    }

    @Transactional(readOnly = true)
    open fun list(page: Int? = null, limit: Int? = null): Page<T> {
        return repository.findAll(defaultPageRequest(page, limit))
    }
}

