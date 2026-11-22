package com.inyogeshwar.metaclean.di

import android.content.Context
import androidx.room.Room
import com.inyogeshwar.metaclean.data.local.AppDatabase
import com.inyogeshwar.metaclean.data.local.HistoryDao
import com.inyogeshwar.metaclean.data.repository.HistoryRepositoryImpl
import com.inyogeshwar.metaclean.data.repository.MetadataRepositoryImpl
import com.inyogeshwar.metaclean.data.repository.SettingsRepositoryImpl
import com.inyogeshwar.metaclean.domain.repository.HistoryRepository
import com.inyogeshwar.metaclean.domain.repository.MetadataRepository
import com.inyogeshwar.metaclean.domain.repository.SettingsRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "metaclean.db"
        ).fallbackToDestructiveMigration().build()
    }

    @Provides
    @Singleton
    fun provideHistoryDao(database: AppDatabase): HistoryDao {
        return database.historyDao()
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindHistoryRepository(impl: HistoryRepositoryImpl): HistoryRepository

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(impl: SettingsRepositoryImpl): SettingsRepository

    @Binds
    @Singleton
    abstract fun bindMetadataRepository(impl: MetadataRepositoryImpl): MetadataRepository
}
