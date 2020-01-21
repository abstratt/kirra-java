package com.abstratt.kirra.pojo

open class BasePojoEntity(override var instanceId: Long? = null) : IBaseEntity{
    override fun assignInstanceId(newInstanceId: Long?) {
        this.instanceId = newInstanceId
    }
}
