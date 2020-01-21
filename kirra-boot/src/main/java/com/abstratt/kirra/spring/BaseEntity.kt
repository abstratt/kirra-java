package com.abstratt.kirra.spring

import com.abstratt.kirra.pojo.IBaseEntity
import javax.persistence.GeneratedValue
import javax.persistence.Id
import javax.persistence.MappedSuperclass

@MappedSuperclass
abstract class BaseEntity(
    @Id @GeneratedValue open var id: Long? = null
) : IBaseEntity {

    override fun assignInstanceId(newInstanceId: Long?) {
        this.id = newInstanceId
    }

    override val instanceId: Long?
        get() = id

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
