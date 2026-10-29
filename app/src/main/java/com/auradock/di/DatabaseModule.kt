package com.auradock.di

import android.content.Context
import androidx.room.Room
import com.auradock.data.AuraDockDatabase
import com.auradock.tasks.TaskDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Tells Hilt how to build the database and the DAO, once for the whole app. */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Singleton
    @Provides
    fun provideDatabase(@ApplicationContext context: Context): AuraDockDatabase =
        Room.databaseBuilder(context, AuraDockDatabase::class.java, "auradock.db").build()

    @Provides
    fun provideTaskDao(db: AuraDockDatabase): TaskDao = db.taskDao()
}
