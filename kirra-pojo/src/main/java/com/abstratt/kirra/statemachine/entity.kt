package com.abstratt.kirra.statemachine


/*

 */

interface Source

class add(vararg values: Source) : Source

class attribute(val target: Source, val attribute: AttributeRef) : Source

class AttributeRef(entity: EntityRef) : PropertyRef(entity)

class self : Source

class sum(val toSum: Source) : Source

class Type

interface Typed {
    val type: Type
    val isRequired: Boolean
    val isMultivalue: Boolean
}

open class PropertyRef(val entity: EntityRef)

class EntityRef

class Entity(val attributes: List<Attribute>, val relationships: List<Relationship>)

class Action(val entity : Entity)

class Query(val entity : Entity, val source : Source)

class Attribute

class Relationship

class project(val property: PropertyRef, val source: Source) : Source

class relationship(val relationship: RelationshipRef, val target: Source) : Source

class RelationshipRef(entity: EntityRef) : PropertyRef(entity)

fun main() {
    val account = EntityRef()
    val lastBalance = AttributeRef(account)
    val amount = AttributeRef(account)
    val transactions = RelationshipRef(account)
    // self.lastBalance + self.transactions.amount.sum()
    add(
            attribute(self(), lastBalance),
            sum(
                    project(
                            amount,
                            relationship(transactions, self())
                    )
            )
    )
}
