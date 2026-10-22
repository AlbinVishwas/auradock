package com.auradock.di

import android.content.Context
import androidx.room.Room
import com.auradock.data.db.AuraDockDatabase
import com.auradock.data.db.SessionDao
import com.auradock.data.db.WidgetDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Singleton
    @Provides
    fun provideDatabase(@ApplicationContext context: Context): AuraDockDatabase =
        Room.databaseBuilder(context, AuraDockDatabase::class.java, "auradock.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideWidgetDao(db: AuraDockDatabase): WidgetDao = db.widgetDao()

    @Provides
    fun provideSessionDao(db: AuraDockDatabase): SessionDao = db.sessionDao()
}
