package com.abstratt.kirra.spring

import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort

fun defaultPageRequest(page: Int? = 0, limit: Int? = 10) = PageRequest(
    page?:0, limit?:9999, Sort(Sort.Order("id"))
)

annotation class Named (
        val label : String = "",
        val description : String = "",
        val name : String = "",
        val symbol : String = ""
)

interface KirraSpringMarker




