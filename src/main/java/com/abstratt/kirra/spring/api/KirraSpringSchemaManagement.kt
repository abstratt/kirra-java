package com.abstratt.kirra.spring.api

import com.abstratt.kirra.*

class KirraSpringSchemaManagement : SchemaManagement {
    override fun getOpposite(relationship: Relationship?): Relationship {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getEntityRelationships(namespace: String?, name: String?): MutableList<Relationship> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getAllTupleTypes(): MutableList<TupleType> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getEntityNames(): MutableCollection<TypeRef> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getTupleTypes(namespace: String?): MutableList<TupleType> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getSchema(): Schema {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getTopLevelEntities(namespace: String?): MutableList<Entity> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getEntities(namespace: String?): MutableList<Entity> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getNamespaces(): MutableList<String> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getEntity(namespace: String?, name: String?): Entity {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getEntity(typeRef: TypeRef?): Entity {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getService(namespace: String?, name: String?): Service {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getService(typeRef: TypeRef?): Service {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getAllServices(): MutableList<Service> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getTupleType(namespace: String?, name: String?): TupleType {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getTupleType(typeRef: TypeRef?): TupleType {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getNamespace(namespaceName: String?): Namespace {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getAllEntities(): MutableList<Entity> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getServices(namespace: String?): MutableList<Service> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getApplicationName(): String {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getEntityProperties(namespace: String?, name: String?): MutableList<Property> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getBuild(): String {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getEntityOperations(namespace: String?, name: String?): MutableList<Operation> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

}