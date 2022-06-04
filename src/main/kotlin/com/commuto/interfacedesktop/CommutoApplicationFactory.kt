package com.commuto.interfacedesktop

import dagger.Component
import javax.inject.Singleton

@Singleton
@Component
interface CommutoApplicationFactory {
    fun commutoApplicationFactory(): CommutoApplication
}