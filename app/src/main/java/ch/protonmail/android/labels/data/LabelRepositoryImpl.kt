/*
 * Copyright (c) 2020 Proton Technologies AG
 *
 * This file is part of ProtonMail.
 *
 * ProtonMail is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * ProtonMail is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with ProtonMail. If not, see https://www.gnu.org/licenses/.
 */

package ch.protonmail.android.labels.data

import androidx.paging.DataSource
import ch.protonmail.android.api.ProtonMailApi
import ch.protonmail.android.labels.data.db.LabelDao
import ch.protonmail.android.labels.data.db.LabelEntity
import ch.protonmail.android.labels.data.mapper.LabelsMapper
import ch.protonmail.android.labels.data.model.LabelId
import ch.protonmail.android.labels.data.model.LabelType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.runBlocking
import me.proton.core.domain.entity.UserId
import timber.log.Timber
import javax.inject.Inject

internal class LabelRepositoryImpl @Inject constructor(
    private val labelDao: LabelDao,
    private val api: ProtonMailApi,
    private val labelMapper: LabelsMapper
) : LabelRepository {

    override fun observeAllLabels(userId: UserId): Flow<List<LabelEntity>> =
        labelDao.observeAllLabels(userId)
            .onStart {
                Timber.v("Fetching fresh labels")
                fetchAndSaveAllLabels(userId)
            }

    override suspend fun findAllLabels(userId: UserId): List<LabelEntity> {
        val dbData = labelDao.findAllLabels(userId)
        return if (dbData.isEmpty()) {
            Timber.v("Fetching fresh labels")
            fetchAndSaveAllLabels(userId)
        } else {
            dbData
        }
    }

    override fun observeLabels(userId: UserId, labelsIds: List<LabelId>): Flow<List<LabelEntity>> =
        labelDao.observeLabelsById(userId, labelsIds)

    override suspend fun findLabels(userId: UserId, labelsIds: List<LabelId>): List<LabelEntity> =
        labelDao.findLabelsById(userId, labelsIds)

    override suspend fun findLabel(labelId: LabelId): LabelEntity? =
        labelDao.findLabelById(labelId)

    override fun findLabelBlocking(labelId: LabelId): LabelEntity? {
        return runBlocking {
            findLabel(labelId)
        }
    }

    override suspend fun findContactGroups(userId: UserId): List<LabelEntity> =
        labelDao.findLabelsByType(userId, LabelType.CONTACT_GROUP.typeInt)

    override suspend fun saveLabels(labels: List<LabelEntity>) {
        Timber.v("Save labels: ${labels.map { it.id.id }}")
        labelDao.insertOrUpdate(*labels.toTypedArray())
    }

    override suspend fun saveLabel(label: LabelEntity) =
        saveLabels(listOf(label))

    override suspend fun deleteLabel(labelId: LabelId) {
        labelDao.deleteLabelById(labelId)
    }

    override suspend fun deleteAllLabels(userId: UserId) {
        labelDao.deleteAllLabels(userId)
    }

    override fun findAllLabelsPaged(userId: UserId): DataSource.Factory<Int, LabelEntity> =
        labelDao.findAllLabelsPaged(userId)

    override fun findAllFoldersPaged(userId: UserId): DataSource.Factory<Int, LabelEntity> =
        labelDao.findAllFoldersPaged(userId)

    override suspend fun deleteContactGroups(userId: UserId) {
        labelDao.deleteContactGroups(userId)
    }

    private suspend fun fetchAndSaveAllLabels(
        userId: UserId
    ): List<LabelEntity> {
        val serverLabels = api.fetchLabels(userId).valueOrThrow.labels
        val serverFolders = api.fetchFolders(userId).valueOrThrow.labels
        val serverContactGroups = api.fetchContactGroups(userId).valueOrThrow.labels
        val labelList = serverLabels.map { labelMapper.mapLabelToLabelEntity(it, userId) }
        val foldersList = serverFolders.map { labelMapper.mapLabelToLabelEntity(it, userId) }
        val groupsList = serverContactGroups.map { labelMapper.mapLabelToLabelEntity(it, userId) }
        val allLabels = labelList + foldersList + groupsList
        saveLabels(allLabels)
        return allLabels
    }
}
