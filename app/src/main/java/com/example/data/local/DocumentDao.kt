package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.VaultDocumentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DocumentDao {
    @Query("SELECT * FROM vault_documents ORDER BY isFavorite DESC, updatedAt DESC")
    fun observeAllDocuments(): Flow<List<VaultDocumentEntity>>

    @Query("SELECT * FROM vault_documents WHERE id = :id LIMIT 1")
    suspend fun getDocumentById(id: String): VaultDocumentEntity?

    @Query("SELECT * FROM vault_documents ORDER BY updatedAt DESC")
    suspend fun getAllDocumentsOnce(): List<VaultDocumentEntity>

    @Query("SELECT * FROM vault_documents WHERE syncStatus != 'SYNCED'")
    suspend fun getPendingSyncDocuments(): List<VaultDocumentEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDocument(document: VaultDocumentEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDocuments(documents: List<VaultDocumentEntity>)

    @Update
    suspend fun updateDocument(document: VaultDocumentEntity)

    @Query("UPDATE vault_documents SET isFavorite = :isFavorite, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateFavorite(id: String, isFavorite: Boolean, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE vault_documents SET isOfflinePinned = :isOfflinePinned, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateOfflinePinned(id: String, isOfflinePinned: Boolean, updatedAt: Long = System.currentTimeMillis())

    @Query(
        """
        UPDATE vault_documents 
        SET syncStatus = :syncStatus, 
            cloudStorageUrl = COALESCE(:cloudUrl, cloudStorageUrl),
            cloudStoragePath = COALESCE(:cloudPath, cloudStoragePath),
            ownerUid = :ownerUid,
            ownerEmail = :ownerEmail,
            updatedAt = :updatedAt 
        WHERE id = :id
        """
    )
    suspend fun updateSyncState(
        id: String,
        syncStatus: String,
        cloudUrl: String?,
        cloudPath: String?,
        ownerUid: String,
        ownerEmail: String,
        updatedAt: Long = System.currentTimeMillis()
    )

    @Query(
        """
        UPDATE vault_documents 
        SET ownerUid = :uid, ownerEmail = :email 
        WHERE ownerUid = 'local_vault' OR ownerUid = ''
        """
    )
    suspend fun claimLocalDocumentsForUser(uid: String, email: String)

    @Query("DELETE FROM vault_documents WHERE id = :id")
    suspend fun deleteDocumentById(id: String)
}
