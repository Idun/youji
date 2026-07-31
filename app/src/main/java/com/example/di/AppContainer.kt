package com.example.di

import android.content.Context
import androidx.room.Room
import com.example.data.local.InspirationDatabase
import com.example.data.repository.InspirationRepositoryImpl
import com.example.domain.repository.InspirationRepository

interface AppContainer {
    val inspirationRepository: InspirationRepository
}

class AppContainerImpl(private val context: Context) : AppContainer {
    private val database: InspirationDatabase by lazy {
        Room.databaseBuilder(
            context,
            InspirationDatabase::class.java,
            "youji_inspirations.db"
        )
        .fallbackToDestructiveMigration()
        .build()
    }

    override val inspirationRepository: InspirationRepository by lazy {
        InspirationRepositoryImpl(database.inspirationDao())
    }
}
