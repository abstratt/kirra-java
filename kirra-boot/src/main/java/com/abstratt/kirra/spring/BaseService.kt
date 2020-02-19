package com.abstratt.kirra.spring;

import com.abstratt.kirra.KirraException
import com.abstratt.kirra.pojo.IBaseEntity
import com.abstratt.kirra.pojo.IBaseService
import com.abstratt.kirra.pojo.IInstancePage
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.Page
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.transaction.annotation.Transactional
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1

abstract class GenericBaseService<T : BaseEntity>(entityClass: KClass<T>) : BaseService<T, BaseRepository<T>>(entityClass)

abstract class BaseService<T : IBaseEntity, R : JpaRepository<T, Long>>(open val entityClass: KClass<T>) : IBaseService<T> {
    @Autowired
    lateinit open var repository : R

    @Transactional(readOnly = true)
    override fun findById(id: Long): T? {
        return repository.findById(id).orElse(null)
    }

    @Transactional(readOnly = true)
    override fun <R : IBaseEntity> getRelated(id: Long, ktProperty: KProperty1<T, R>): Iterable<R> {
        val existingInstance = repository.findById(id)
        KirraException.ensure(existingInstance.isPresent, KirraException.Kind.OBJECT_NOT_FOUND, null)
        return ktProperty.call(existingInstance.get()) as Iterable<R> ?: emptyList()
    }

    @Transactional
    override fun create(toCreate: T): T {
        toCreate.assignInstanceId(null)
        val savedInstance = repository.save(toCreate)
        return repository.getOne(savedInstance.instanceId!!)
    }

    @Transactional
    override fun update(toUpdate: T): T? {
        checkNotNull(toUpdate.instanceId, { "id was missing" })
        val existingInstance = repository.findById(toUpdate.instanceId!!)
        if (!existingInstance.isPresent)
            return null
        return repository.save(toUpdate)
    }

    @Transactional
    override fun delete(id: Long): Boolean {
        val existingInstance = repository.existsById(id)
        if (existingInstance) {
            repository.deleteById(id)
            return true
        }
        return false
    }

    @Transactional(readOnly = true)
    override fun list(page: Int?, limit: Int?): IInstancePage<T> {
        return InstancePage(repository.findAll(defaultPageRequest(page, limit)))
    }
}

class InstancePage<T : IBaseEntity>(springPage : Page<T>): IInstancePage<T>, Page<T> by springPage {
    override val instances : List<T> get() = content.toList()!!
}