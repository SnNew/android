package com.example.tiendasuplementacion.viewmodel

import androidx.lifecycle.*
import com.example.tiendasuplementacion.model.UserDetail
import com.example.tiendasuplementacion.repository.UserDetailRepository
import kotlinx.coroutines.launch

class UserDetailViewModel : ViewModel() {
    private val repository = UserDetailRepository()
    
    private val _userDetail = MutableLiveData<UserDetail>()
    val userDetail: LiveData<UserDetail> = _userDetail

    private val _userDetailsList = MutableLiveData<List<UserDetail>>()
    val userDetailsList: LiveData<List<UserDetail>> = _userDetailsList

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    fun fetchUserDetails(id: Long) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null
                _userDetail.value = repository.getUserDetails(id)
            } catch (e: Exception) {
                _error.value = e.message ?: "Error al cargar los detalles del usuario"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun fetchUserDetailsByRole(roleId: Long) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null
                _userDetailsList.value = repository.getUserDetailsByRole(roleId)
            } catch (e: Exception) {
                _error.value = e.message ?: "Error al cargar los detalles de los usuarios"
                _userDetailsList.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearError() {
        _error.value = null
    }
} 