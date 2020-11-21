package com.jijith.alexa.hmi.registration

import android.content.Context
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.jijith.alexa.vo.User

class RegistrationViewModel() : ViewModel() {

    private lateinit var context: Context
    private lateinit var mainRepository: RegistrationRepository

    var loadingVisibility: MutableLiveData<Int> = MutableLiveData()
    var errorMessage: MutableLiveData<String> = MutableLiveData()
    var success: MutableLiveData<Boolean> = MutableLiveData()

    var user = MutableLiveData<User>()

    constructor(context: Context, mainRepository: RegistrationRepository) : this() {
        this.context = context
        this.mainRepository = mainRepository
        user = mainRepository.user
        loadingVisibility = mainRepository.loading
        errorMessage = mainRepository.errrorMessage
        success = mainRepository.success
    }

    fun startCBL() {
        mainRepository.startCBL()
    }
}