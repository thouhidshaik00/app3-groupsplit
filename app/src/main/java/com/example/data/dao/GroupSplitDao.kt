package com.example.data.dao

import androidx.room.*
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface GroupSplitDao {

    // Groups
    @Query("SELECT * FROM `groups` ORDER BY id DESC")
    fun getAllGroups(): Flow<List<Group>>

    @Query("SELECT * FROM `groups` WHERE id = :groupId LIMIT 1")
    fun getGroupById(groupId: Int): Flow<Group?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroup(group: Group): Long

    @Delete
    suspend fun deleteGroup(group: Group)

    // Users
    @Query("SELECT * FROM users ORDER BY name ASC")
    fun getAllUsers(): Flow<List<User>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: User): Long

    @Update
    suspend fun updateUser(user: User)

    // Group Members
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertGroupMemberCrossRef(crossRef: GroupMemberCrossRef)

    @Query("DELETE FROM group_member_cross_ref WHERE groupId = :groupId AND userId = :userId")
    suspend fun deleteGroupMemberCrossRef(groupId: Int, userId: Int)

    @Query("""
        SELECT users.* FROM users 
        INNER JOIN group_member_cross_ref ON users.id = group_member_cross_ref.userId 
        WHERE group_member_cross_ref.groupId = :groupId
        ORDER BY users.name ASC
    """)
    fun getGroupMembers(groupId: Int): Flow<List<User>>

    // Expenses
    @Query("SELECT * FROM expenses WHERE groupId = :groupId ORDER BY timestamp DESC")
    fun getExpensesForGroup(groupId: Int): Flow<List<Expense>>

    @Query("SELECT * FROM expenses WHERE isSynced = 0")
    suspend fun getUnsyncedExpenses(): List<Expense>

    @Query("UPDATE expenses SET isSynced = :isSynced WHERE id = :expenseId")
    suspend fun updateExpenseSyncStatus(expenseId: Int, isSynced: Boolean)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: Expense): Long

    @Query("DELETE FROM expenses WHERE id = :expenseId")
    suspend fun deleteExpense(expenseId: Int)

    // Expense Splits
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpenseSplits(splits: List<ExpenseSplit>)

    @Query("""
        SELECT expense_splits.* FROM expense_splits
        INNER JOIN expenses ON expense_splits.expenseId = expenses.id
        WHERE expenses.groupId = :groupId
    """)
    fun getExpenseSplitsForGroup(groupId: Int): Flow<List<ExpenseSplit>>

    @Query("SELECT * FROM expense_splits WHERE expenseId = :expenseId")
    fun getExpenseSplitsForExpense(expenseId: Int): Flow<List<ExpenseSplit>>

    @Query("SELECT * FROM expense_splits WHERE expenseId = :expenseId")
    suspend fun getSplitsForExpenseDirect(expenseId: Int): List<ExpenseSplit>
}
