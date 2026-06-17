package com.example.data.repository

import com.example.data.dao.GroupSplitDao
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow

class GroupSplitRepository(private val dao: GroupSplitDao) {

    fun getAllGroups(): Flow<List<Group>> = dao.getAllGroups()

    fun getGroupById(groupId: Int): Flow<Group?> = dao.getGroupById(groupId)

    suspend fun insertGroup(group: Group): Long = dao.insertGroup(group)

    suspend fun deleteGroup(group: Group) = dao.deleteGroup(group)

    fun getAllUsers(): Flow<List<User>> = dao.getAllUsers()

    suspend fun insertUser(user: User): Long = dao.insertUser(user)

    suspend fun updateUser(user: User) = dao.updateUser(user)

    suspend fun addMemberToGroup(groupId: Int, userId: Int) {
        dao.insertGroupMemberCrossRef(GroupMemberCrossRef(groupId, userId))
    }

    suspend fun removeMemberFromGroup(groupId: Int, userId: Int) {
        dao.deleteGroupMemberCrossRef(groupId, userId)
    }

    fun getGroupMembers(groupId: Int): Flow<List<User>> = dao.getGroupMembers(groupId)

    fun getExpensesForGroup(groupId: Int): Flow<List<Expense>> = dao.getExpensesForGroup(groupId)

    fun getExpenseSplitsForGroup(groupId: Int): Flow<List<ExpenseSplit>> = dao.getExpenseSplitsForGroup(groupId)

    suspend fun deleteExpense(expenseId: Int) = dao.deleteExpense(expenseId)

    suspend fun getUnsyncedExpenses(): List<Expense> = dao.getUnsyncedExpenses()

    suspend fun updateExpenseSyncStatus(expenseId: Int, isSynced: Boolean) = dao.updateExpenseSyncStatus(expenseId, isSynced)

    suspend fun getSplitsForExpenseDirect(expenseId: Int): List<ExpenseSplit> = dao.getSplitsForExpenseDirect(expenseId)

    suspend fun createExpenseWithSplits(expense: Expense, splits: List<ExpenseSplit>) {
        val expenseId = dao.insertExpense(expense)
        val updatedSplits = splits.map { it.copy(expenseId = expenseId.toInt()) }
        dao.insertExpenseSplits(updatedSplits)
    }
}
