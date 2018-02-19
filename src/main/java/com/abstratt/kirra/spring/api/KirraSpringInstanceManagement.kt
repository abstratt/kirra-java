package com.abstratt.kirra.spring.api

import com.abstratt.kirra.*

class KirraSpringInstanceManagement : InstanceManagement {
    override fun createInstance(instance: Instance?): Instance {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun linkInstances(relationship: Relationship?, sourceId: String?, destinationId: InstanceRef?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun zap() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getEnabledEntityActions(entity: Entity?): MutableList<Operation> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun updateInstance(instance: Instance?): Instance {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getRelationshipDomain(entity: Entity?, objectId: String?, relationship: Relationship?): MutableList<Instance> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun saveContext() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getCurrentUser(): Instance? {
        return null
    }

    override fun getCurrentUserRoles(): MutableList<Instance> {
        return emptyList<Instance>().toMutableList()
    }

    override fun filterInstances(criteria : MutableMap<String, MutableList<Any>>?, namespace : String?, name : String?, profile : InstanceManagement.DataProfile?): MutableList<Instance> {
        return getInstances(namespace, name, profile);
    }

    override fun getInstances(namespace: String?, name: String?, dataProfile: InstanceManagement.DataProfile?): MutableList<Instance> {
        return emptyList<Instance>().toMutableList()
    }

    //public List<Instance> filterInstances(Map<String, List<Object>> criteria, String namespace, String name, DataProfile profile) {}

    override fun isRestricted(): Boolean {
        return false
    }

    override fun executeOperation(operation: Operation?, externalId: String?, arguments: MutableList<*>?): MutableList<*> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getParameterDomain(entity: Entity?, externalId: String?, action: Operation?, parameter: Parameter?): MutableList<Instance> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun newInstance(namespace: String?, name: String?): Instance {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun validateInstance(toValidate: Instance?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getRelatedInstances(namespace: String?, name: String?, externalId: String?, relationship: String?, dataProfile: InstanceManagement.DataProfile?): MutableList<Instance> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun unlinkInstances(relationship: Relationship?, sourceId: String?, destinationId: InstanceRef?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun deleteInstance(instance: Instance?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun deleteInstance(namespace: String?, name: String?, id: String?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getInstance(namespace: String?, name: String?, externalId: String?, dataProfile: InstanceManagement.DataProfile?): Instance {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

}