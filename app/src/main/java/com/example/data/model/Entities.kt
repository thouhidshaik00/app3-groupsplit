package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(tableName = "groups")
data class Group(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val description: String = ""
)

@Entity(tableName = "users")
data class User(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String
)

@Entity(
    tableName = "group_member_cross_ref",
    primaryKeys = ["groupId", "userId"],
    indices = [
        Index(value = ["groupId"]),
        Index(value = ["userId"])
    ]
)
data class GroupMemberCrossRef(
    val groupId: Int,
    val userId: Int
)

@Entity(
    tableName = "expenses",
    foreignKeys = [
        ForeignKey(
            entity = Group::class,
            parentColumns = ["id"],
            childColumns = ["groupId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["groupId"])]
)
data class Expense(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val groupId: Int,
    val title: String,
    val amount: Double,
    val paidByUserId: Int,
    val timestamp: Long = System.currentTimeMillis(),
    val isSynced: Boolean = true
)

@Entity(
    tableName = "expense_splits",
    primaryKeys = ["expenseId", "userId"],
    foreignKeys = [
        ForeignKey(
            entity = Expense::class,
            parentColumns = ["id"],
            childColumns = ["expenseId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["expenseId"])]
)
data class ExpenseSplit(
    val expenseId: Int,
    val userId: Int,
    val shareAmount: Double
)
