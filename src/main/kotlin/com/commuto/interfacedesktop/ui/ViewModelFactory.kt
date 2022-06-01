package com.commuto.interfacedesktop.ui

import dagger.Component
import javax.inject.Singleton

@Singleton
@Component
interface ViewModelFactory {
    fun offersViewModel(): OffersViewModel
}