package com.covildev.pulso.feature_perfil.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_profile")
data class UserEntity(
    @PrimaryKey
    val id: Int = 1,
    val name: String,
    val age: Int,
)
