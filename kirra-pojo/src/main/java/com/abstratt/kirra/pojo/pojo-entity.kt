package com.abstratt.kirra.pojo

import kotlin.reflect.KProperty1

interface IBaseEntity {
    val instanceId : Long?
    fun assignInstanceId(newInstanceId : Long?)
}
interface IInstancePage<T : IBaseEntity> {
    val instances: List<T>
}

interface IBaseService<T : IBaseEntity> {
    open fun findById(id: Long): IBaseEntity?

    fun <R : IBaseEntity> getRelated(id: Long, ktProperty: KProperty1<T, R>): Iterable<R>

    open fun create(toCreate: T): T

    open fun update(toUpdate: T): T?

    open fun delete(id: Long): Boolean

    open fun list(page: Int? = null, limit: Int? = null): IInstancePage<T>
}

abstract class BasePojoService<T : IBaseEntity> : IBaseService<T> {
    override fun create(toCreate: T): T {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun delete(id: Long): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun findById(id: Long): IBaseEntity? {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun <R : IBaseEntity> getRelated(id: Long, ktProperty: KProperty1<T, R>): Iterable<R> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun list(page: Int?, limit: Int?): IInstancePage<T> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun update(toUpdate: T): T? {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}