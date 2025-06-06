package com.example.tiendasuplementacion.interfaces

import com.example.tiendasuplementacion.model.User
import com.example.tiendasuplementacion.model.LoginResponse
import com.example.tiendasuplementacion.model.UserDetail
import retrofit2.http.*

interface UserApiService {

    @GET("/api/users")
    suspend fun getUsers(): List<User>

    @GET("/api/users/{id}")
    suspend fun getUserById(@Path("id") id: Long): User

    @POST("/api/users")
    suspend fun createUser(@Body user: User): User

    @PUT("/api/users/{id}")
    suspend fun updateUser(@Path("id") id: Long, @Body user: User): User

    @DELETE("/api/users/{id}")
    suspend fun deleteUser(@Path("id") id: Long)

    @POST("/api/auth/login")
    suspend fun login(@Body credentials: Map<String, String>): LoginResponse

    @GET("/api/user-details/{id}")
    suspend fun getUserDetails(@Path("id") id: Long): UserDetail

    @GET("/api/user-details/role/{roleId}")
    suspend fun getUserDetailsByRole(@Path("roleId") roleId: Long): List<UserDetail>
}
