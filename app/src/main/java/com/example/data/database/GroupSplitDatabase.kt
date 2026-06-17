package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.dao.GroupSplitDao
import com.example.data.model.*

@Database(
    entities = [
        Group::class,
        User::class,
        GroupMemberCrossRef::class,
        Expense::class,
        ExpenseSplit::class
    ],
    version = 1,
    exportSchema = false
)
abstract class GroupSplitDatabase : RoomDatabase() {
    abstract fun groupSplitDao(): GroupSplitDao

    companion object {
        @Volatile
        private var INSTANCE: GroupSplitDatabase? = null

        fun getDatabase(context: Context): GroupSplitDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    GroupSplitDatabase::class.java,
                    "groupsplit_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
