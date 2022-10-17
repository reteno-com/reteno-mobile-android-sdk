package com.reteno.di.provider

import com.reteno.data.remote.ds.ContactRepository
import com.reteno.data.remote.ds.ContactRepositoryImpl
import com.reteno.di.base.ProviderWeakReference

class ContactRepositoryProvider(private val apiClientProvider: ApiClientProvider) :
    ProviderWeakReference<ContactRepository>() {

    override fun create(): ContactRepository {
        return ContactRepositoryImpl(apiClientProvider.get())
    }
}