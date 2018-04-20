package com.abstratt.kirra.spring

import com.abstratt.kirra.Schema
import com.abstratt.kirra.SchemaManagementSnapshot
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class KirraSchemaManagement (@Autowired schema : Schema)
    : SchemaManagementSnapshot(schema) {
}