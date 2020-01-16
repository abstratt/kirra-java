package com.abstratt.kirra.spring;

import com.abstratt.kirra.KirraException
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.Page
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.transaction.annotation.Transactional
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1

abstract class GenericBaseService<T : BaseEntity>(entityClass: KClass<T>) : BaseService<T, BaseRepository<T>>(entityClass)

abstract class BaseService<T : BaseEntity, R : JpaRepository<T, Long>>(open val entityClass: KClass<T>) {
    @Autowired
    lateinit open var repository : R

    @Transactional(readOnly = true)
    open fun findById(id: Long): T? {
        return repository.findById(id).orElse(null)
    }

    @Transactional(readOnly = true)
    fun <R : BaseEntity> getRelated(id: Long, ktProperty: KProperty1<T, R>): Iterable<R> {
        val existingInstance = repository.findById(id)
        KirraException.ensure(existingInstance.isPresent, KirraException.Kind.OBJECT_NOT_FOUND, null)
        return ktProperty.call(existingInstance.get()) as Iterable<R> ?: emptyList()
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

