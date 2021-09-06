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

package ch.protonmail.android.mailbox.domain.model

import ch.protonmail.android.api.models.messages.receive.MessagesResponse
import ch.protonmail.android.data.local.model.Message
import me.proton.core.domain.entity.UserId

/**
 * Representation of Rest Query Parameters for 'mail/v4/messages' endpoint
 * Documentation at '*\/Slim-API/mail/#operation/get_mail-v4-messages'
 */
data class GetAllMessagesParameters(
    val userId: UserId,
    val page: Int? = null,
    val pageSize: Int = 50,
    val labelId: String? = null,
    val sortBy: SortBy = SortBy.TIME,
    val sortDirection: SortDirection = SortDirection.DESCENDANT,
    val begin: Long? = null,
    val end: Long? = null,
    val beginId: String? = null,
    val endId: String? = null,
    val keyword: String? = null
) {

    enum class SortBy(val stringValue: String) {
        TIME("Time")
    }

    enum class SortDirection(val intValue: Int) {
        ASCENDANT(0),
        DESCENDANT(1)
    }
}

/**
 * @return a [GetAllMessagesParameters] created from [MessagesResponse] if [MessagesResponse.serverMessages] list is
 *  not empty, otherwise [currentParameters]
 *
 * Note: since we're using [Message.time] for fetch progressively, we assume that the messages are already ordered by
 *  time, so we pick directly the last in the list, without adding unneeded computation
 */
fun MessagesResponse.createBookmarkParametersOr(
    currentParameters: GetAllMessagesParameters
): GetAllMessagesParameters {
    val messages = serverMessages ?: emptyList()
    return if (messages.isNotEmpty()) {
        val lastMessage = messages.last()
        currentParameters.copy(
            end = lastMessage.time,
            endId = checkNotNull(lastMessage.id) { "Can't create params: messageId is null" }
        )
    } else {
        currentParameters
    }
}