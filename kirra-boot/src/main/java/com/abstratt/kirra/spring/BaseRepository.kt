package com.abstratt.kirra.spring;

import com.abstratt.kirra.pojo.IBaseEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.repository.NoRepositoryBean

@NoRepositoryBean
interface BaseRepository<T : IBaseEntity> : JpaRepository<T, Long>, JpaSpecificationExecutor<T>

