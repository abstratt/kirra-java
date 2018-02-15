package com.abstratt.kirra.spring;


import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;

@MappedSuperclass
abstract class BaseEntity(
    @Id @GeneratedValue open var id: Long? = null
)
