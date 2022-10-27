package com.reteno.core.di.provider

import com.reteno.core.data.repository.ContactRepository
import com.reteno.core.data.repository.ContactRepositoryImpl
import com.reteno.core.di.base.ProviderWeakReference

class ContactRepositoryProvider(private val apiClientProvider: ApiClientProvider) :
    ProviderWeakReference<ContactRepository>() {

    override fun create(): ContactRepository {
        return ContactRepositoryImpl(apiClientProvider.get())
    }
}