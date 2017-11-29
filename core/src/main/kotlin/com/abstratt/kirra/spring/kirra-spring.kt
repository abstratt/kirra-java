package com.abstratt.kirra.spring

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.Id

fun defaultPageRequest(page: Int? = 0, limit: Int? = 10) = PageRequest(
    page?:0, limit?:9999, Sort(Sort.Order("id"))
)

@Entity
abstract class BaseEntity(@Id @GeneratedValue open var id: Long? = null)

@Service
abstract class BaseService<T : BaseEntity> {
    @Autowired
    lateinit var repository : BaseRepository<T>

    constructor(repository : BaseRepository<T>? = null) {
        if (repository != null)
            this.repository = repository
    }

    @Transactional(readOnly = true)
    open fun findById(id: Long): T? {
        return repository.findOne(id)
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
        val existingInstance = repository.findOne(toUpdate.id)
        if (existingInstance == null)
            return null
        return repository.save(toUpdate)
    }

    @Transactional
    open fun delete(id: Long): Boolean {
        val existingInstance = repository.findOne(id)
        if (existingInstance != null) {
            repository.delete(existingInstance)
            return true
        }
        return false
    }

    @Transactional(readOnly = true)
    open fun list(page: Int?, limit: Int?): Page<T> {
        return repository.findAll(defaultPageRequest(page, limit))
    }

}

interface BaseRepository<T : BaseEntity> : JpaRepository<T, Long>
