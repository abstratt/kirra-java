package com.abstratt.kirra.spring

import javax.persistence.GeneratedValue
import javax.persistence.Id
import javax.persistence.MappedSuperclass

@MappedSuperclass
abstract class BaseEntity(
    @Id @GeneratedValue open var id: Long? = null
) {
    override fun equals(other: Any?): Boolean {
        val basicCheck = super.equals(other)
        if (basicCheck) {
            return true
        }
        return this::class.isInstance(other) && (other as BaseEntity).id == id
    }

    override fun hashCode(): Int {
        return id?.hashCode() ?: super.hashCode()
    }
}
