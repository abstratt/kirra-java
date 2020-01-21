package com.abstratt.kirra.spring

import com.abstratt.kirra.*
import com.abstratt.kirra.pojo.KirraMetamodel
import com.abstratt.kirra.pojo.KirraInstanceBridge
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class KirraSpringInstanceBridge : KirraInstanceBridge() {

    @Autowired
    private lateinit var _schemaManagement: SchemaManagement

    @Autowired
    private lateinit var _kirraMetamodel: KirraMetamodel


    override val schemaManagement: SchemaManagement
        get() = _schemaManagement

    override val kirraMetamodel: KirraMetamodel
        get() = _kirraMetamodel
}