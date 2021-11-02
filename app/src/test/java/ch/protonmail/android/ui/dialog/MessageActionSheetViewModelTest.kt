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

package ch.protonmail.android.ui.dialog

import androidx.lifecycle.SavedStateHandle
import ch.protonmail.android.core.Constants
import ch.protonmail.android.data.local.model.Message
import ch.protonmail.android.labels.domain.model.LabelType
import ch.protonmail.android.mailbox.domain.usecase.MoveMessagesToFolder
import ch.protonmail.android.mailbox.domain.ChangeConversationsReadStatus
import ch.protonmail.android.mailbox.domain.ChangeConversationsStarredStatus
import ch.protonmail.android.mailbox.domain.DeleteConversations
import ch.protonmail.android.mailbox.domain.MoveConversationsToFolder
import ch.protonmail.android.mailbox.domain.model.ConversationsActionResult
import ch.protonmail.android.mailbox.presentation.ConversationModeEnabled
import ch.protonmail.android.repository.MessageRepository
import ch.protonmail.android.ui.actionsheet.MessageActionSheetAction
import ch.protonmail.android.ui.actionsheet.MessageActionSheetState
import ch.protonmail.android.ui.actionsheet.MessageActionSheetViewModel
import ch.protonmail.android.ui.actionsheet.model.ActionSheetTarget
import ch.protonmail.android.usecase.delete.DeleteMessage
import ch.protonmail.android.usecase.message.ChangeMessagesReadStatus
import ch.protonmail.android.usecase.message.ChangeMessagesStarredStatus
import io.mockk.Called
import io.mockk.MockKAnnotations
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runBlockingTest
import me.proton.core.accountmanager.domain.AccountManager
import me.proton.core.domain.entity.UserId
import me.proton.core.test.android.ArchTest
import me.proton.core.test.kotlin.CoroutinesTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class MessageActionSheetViewModelTest : ArchTest, CoroutinesTest {

    @MockK
    private lateinit var deleteMessage: DeleteMessage

    @MockK
    private lateinit var moveMessagesToFolder: MoveMessagesToFolder

    @MockK
    private lateinit var messageRepository: MessageRepository

    @MockK
    private lateinit var changeMessagesReadStatus: ChangeMessagesReadStatus

    @MockK
    private lateinit var changeConversationsReadStatus: ChangeConversationsReadStatus

    @MockK
    private lateinit var changeMessagesStarredStatus: ChangeMessagesStarredStatus

    @MockK
    private lateinit var changeConversationsStarredStatus: ChangeConversationsStarredStatus

    @MockK
    private lateinit var conversationModeEnabled: ConversationModeEnabled

    @MockK
    private lateinit var moveConversationsToFolder: MoveConversationsToFolder

    @MockK
    private lateinit var deleteConversations: DeleteConversations

    @MockK
    private lateinit var accountManager: AccountManager

    @MockK
    private lateinit var savedStateHandle: SavedStateHandle

    private lateinit var viewModel: MessageActionSheetViewModel

    private val testUserId = UserId("testUser")

    @BeforeTest
    fun setUp() {
        MockKAnnotations.init(this)
        every { accountManager.getPrimaryUserId() } returns flowOf(testUserId)
        viewModel = MessageActionSheetViewModel(
            savedStateHandle,
            deleteMessage,
            deleteConversations,
            moveMessagesToFolder,
            moveConversationsToFolder,
            messageRepository,
            changeMessagesReadStatus,
            changeConversationsReadStatus,
            changeMessagesStarredStatus,
            changeConversationsStarredStatus,
            conversationModeEnabled,
            accountManager
        )
    }

    @Test
    fun verifyShowLabelsManagerActionIsExecutedForLabels() = runBlockingTest {
        // given
        val messageId1 = "messageId1"
        val labelId1 = "labelId1"
        val messageId2 = "messageId2"
        val labelId2 = "labelId2"
        val messageIds = listOf(messageId1, messageId2)
        val currentLocation = Constants.MessageLocationType.INBOX
        val labelsSheetType = LabelType.MESSAGE_LABEL
        val expected = MessageActionSheetAction.ShowLabelsManager(
            messageIds,
            currentLocation.messageLocationTypeValue,
            labelsSheetType,
            ActionSheetTarget.MESSAGE_ITEM_IN_DETAIL_SCREEN
        )
        val message1 = mockk<Message> {
            every { messageId } returns messageId1
            every { labelIDsNotIncludingLocations } returns listOf(labelId1)
        }
        val message2 = mockk<Message> {
            every { messageId } returns messageId2
            every { labelIDsNotIncludingLocations } returns listOf(labelId2)
        }
        coEvery { messageRepository.findMessageById(messageId1) } returns message1
        coEvery { messageRepository.findMessageById(messageId2) } returns message2
        every {
            savedStateHandle.get<ActionSheetTarget>("extra_arg_action_sheet_actions_target")
        } returns ActionSheetTarget.MESSAGE_ITEM_IN_DETAIL_SCREEN

        // when
        viewModel.showLabelsManager(messageIds, currentLocation)

        // then
        assertEquals(expected, viewModel.actionsFlow.value)
    }

    @Test
    fun verifyShowLabelsManagerActionIsExecutedForFolders() = runBlockingTest {
        // given
        val messageId1 = "messageId1"
        val labelId1 = "labelId1"
        val messageId2 = "messageId2"
        val labelId2 = "labelId2"
        val messageIds = listOf(messageId1, messageId2)
        val currentLocation = Constants.MessageLocationType.INBOX
        val labelsSheetType = LabelType.FOLDER
        val expected = MessageActionSheetAction.ShowLabelsManager(
            messageIds,
            currentLocation.messageLocationTypeValue,
            labelsSheetType,
            ActionSheetTarget.MAILBOX_ITEMS_IN_MAILBOX_SCREEN
        )
        val message1 = mockk<Message> {
            every { messageId } returns messageId1
            every { labelIDsNotIncludingLocations } returns listOf(labelId1)
        }
        val message2 = mockk<Message> {
            every { messageId } returns messageId2
            every { labelIDsNotIncludingLocations } returns listOf(labelId2)
        }
        coEvery { messageRepository.findMessageById(messageId1) } returns message1
        coEvery { messageRepository.findMessageById(messageId2) } returns message2
        every {
            savedStateHandle.get<ActionSheetTarget>("extra_arg_action_sheet_actions_target")
        } returns ActionSheetTarget.MAILBOX_ITEMS_IN_MAILBOX_SCREEN

        // when
        viewModel.showLabelsManager(messageIds, currentLocation, LabelType.FOLDER)

        // then
        assertEquals(expected, viewModel.actionsFlow.value)
    }

    @Test
    fun verifyShowMessageHeadersActionProcessing() {

        // given
        val messageId1 = "messageId1"
        val messageHeader = "testMessageHeader"
        val message1 = mockk<Message> {
            every { messageId } returns messageId1
            every { header } returns messageHeader
        }
        coEvery { messageRepository.findMessageById(messageId1) } returns message1
        val expected = MessageActionSheetAction.ShowMessageHeaders(messageHeader)

        // when
        viewModel.showMessageHeaders(messageId1)

        // then
        assertEquals(expected, viewModel.actionsFlow.value)
    }

    @Test
    fun verifyMoveToInboxEmitsShouldDismissActionThatDoesNotDismissBackingActivityWhenBottomActionSheetTargetIsConversationDetails() {
        // given
        val messageId = "messageId1"
        val expected = MessageActionSheetAction.DismissActionSheet(false)
        every { conversationModeEnabled(any()) } returns true
        coEvery { moveMessagesToFolder.invoke(any(), any(), any(), any()) } just Runs
        every {
            savedStateHandle.get<ActionSheetTarget>("extra_arg_action_sheet_actions_target")
        } returns ActionSheetTarget.MESSAGE_ITEM_WITHIN_CONVERSATION_DETAIL_SCREEN

        // when
        viewModel.moveToInbox(
            listOf(messageId),
            Constants.MessageLocationType.ARCHIVE
        )

        // then
        assertEquals(expected, viewModel.actionsFlow.value)
        coVerify { moveMessagesToFolder.invoke(listOf(messageId), "0", "6", testUserId) }
    }

    @Test
    fun verifyMoveToInboxEmitsShouldDismissActionThatDismissesBackingActivityWhenBottomActionSheetTargetIsMessageDetails() {
        // given
        val messageId = "messageId2"
        val expected = MessageActionSheetAction.DismissActionSheet(true)
        every { conversationModeEnabled(any()) } returns false
        coEvery { moveMessagesToFolder.invoke(any(), any(), any(), any()) } just Runs
        every {
            savedStateHandle.get<ActionSheetTarget>("extra_arg_action_sheet_actions_target")
        } returns ActionSheetTarget.MESSAGE_ITEM_IN_DETAIL_SCREEN

        // when
        viewModel.moveToInbox(
            listOf(messageId),
            Constants.MessageLocationType.SPAM
        )

        // then
        assertEquals(expected, viewModel.actionsFlow.value)
        coVerify { moveMessagesToFolder.invoke(listOf(messageId), "0", "4", testUserId) }
    }

    @Test
    fun verifyMoveToInboxEmitsCouldNotCompleteActionErrorIfMoveConversationsToFolderReturnsErrorResultWhenInConversationMode() {
        // given
        val conversationId = "conversationId"
        val expectedResult = MessageActionSheetAction.CouldNotCompleteActionError
        every { conversationModeEnabled(any()) } returns true
        coEvery { moveConversationsToFolder.invoke(any(), any(), any()) } returns ConversationsActionResult.Error

        // when
        viewModel.moveToInbox(listOf(conversationId), Constants.MessageLocationType.SPAM)

        // then
        assertEquals(expectedResult, viewModel.actionsFlow.value)
    }

    @Test
    fun verifyUnStarMessageCallsChangeConversationStarredStatusWhenConversationModeIsEnabledAndAConversationIsBeingUnStarred() {
        // given
        val conversationId = "conversationId01"
        val expected = MessageActionSheetAction.ChangeStarredStatus(starredStatus = false, isSuccessful = true)
        val userId = UserId("userId")
        val unstarAction = ChangeConversationsStarredStatus.Action.ACTION_UNSTAR
        every { conversationModeEnabled(any()) } returns true
        every { accountManager.getPrimaryUserId() } returns flowOf(userId)
        every {
            savedStateHandle.get<ActionSheetTarget>("extra_arg_action_sheet_actions_target")
        } returns ActionSheetTarget.CONVERSATION_ITEM_IN_DETAIL_SCREEN
        coEvery {
            changeConversationsStarredStatus.invoke(
                listOf(conversationId), userId, unstarAction
            )
        } returns ConversationsActionResult.Success

        // when
        viewModel.unStarMessage(
            listOf(conversationId),
            Constants.MessageLocationType.LABEL_FOLDER
        )

        // then
        assertEquals(expected, viewModel.actionsFlow.value)
        coVerify { changeConversationsStarredStatus.invoke(listOf(conversationId), userId, unstarAction) }
        verify { messageRepository wasNot Called }
    }

    @Test
    fun verifyUnstarMessageEmitsResultWithIsSuccessfulFlagFalseIfChangeConversationStarredStatusReturnsErrorResultInConversationMode() {
        // given
        val conversationId = "conversationId"
        val expectedResult = MessageActionSheetAction.ChangeStarredStatus(starredStatus = false, isSuccessful = false)
        val userId = UserId("userId")
        val unstarAction = ChangeConversationsStarredStatus.Action.ACTION_UNSTAR
        every { conversationModeEnabled(any()) } returns true
        coEvery {
            changeConversationsStarredStatus.invoke(listOf(conversationId), userId, unstarAction)
        } returns ConversationsActionResult.Error

        // when
        viewModel.unStarMessage(
            listOf(conversationId),
            Constants.MessageLocationType.LABEL_FOLDER
        )

        // then
        assertEquals(expectedResult, viewModel.actionsFlow.value)
    }

    @Test
    fun verifyMarkReadCallsChangeConversationReadStatusWhenConversationModeIsEnabledAndAConversationIsBeingMarkedAsRead() {
        // given
        val conversationId = "conversationId02"
        val expected = MessageActionSheetAction.DismissActionSheet(true)
        val userId = UserId("userId02")
        val markReadAction = ChangeConversationsReadStatus.Action.ACTION_MARK_READ
        val location = Constants.MessageLocationType.ALL_MAIL
        every { conversationModeEnabled(any()) } returns true
        every { accountManager.getPrimaryUserId() } returns flowOf(userId)
        every {
            savedStateHandle.get<ActionSheetTarget>("extra_arg_action_sheet_actions_target")
        } returns ActionSheetTarget.CONVERSATION_ITEM_IN_DETAIL_SCREEN
        coEvery {
            changeConversationsReadStatus.invoke(
                listOf(conversationId),
                markReadAction,
                userId,
                location.messageLocationTypeValue.toString()
            )
        } returns ConversationsActionResult.Success

        // when
        viewModel.markRead(
            listOf(conversationId),
            location,
            location.messageLocationTypeValue.toString()
        )

        // then
        assertEquals(expected, viewModel.actionsFlow.value)
        coVerify {
            changeConversationsReadStatus.invoke(
                listOf(conversationId),
                markReadAction,
                userId,
                location.messageLocationTypeValue.toString()
            )
        }
        verify { messageRepository wasNot Called }
    }

    @Test
    fun verifyMarkReadEmitsCouldNotCompleteActionErrorIfChangeConversationsReadStatusReturnsErrorResultWhenInConversationMode() {
        // given
        val conversationId = "conversationId"
        val expectedResult = MessageActionSheetAction.CouldNotCompleteActionError
        val userId = UserId("userId02")
        val markReadAction = ChangeConversationsReadStatus.Action.ACTION_MARK_READ
        val location = Constants.MessageLocationType.ALL_MAIL
        every { conversationModeEnabled(any()) } returns true
        coEvery {
            changeConversationsReadStatus.invoke(
                listOf(conversationId),
                markReadAction,
                userId,
                location.messageLocationTypeValue.toString()
            )
        } returns ConversationsActionResult.Error

        // when
        viewModel.markRead(
            listOf(conversationId),
            location,
            location.messageLocationTypeValue.toString()
        )

        // then
        assertEquals(expectedResult, viewModel.actionsFlow.value)
    }

    @Test
    fun verifyStarMessageCallsMessageRepositoryToStarMessageWhenConversationIsEnabledAndActionIsBeingAppliedToASpecificMessageInAConversation() {
        // given
        val messageId = "messageId3"
        val expected = MessageActionSheetAction.ChangeStarredStatus(true, isSuccessful = true)
        val userId = UserId("userId")
        every { conversationModeEnabled(any()) } returns true
        coEvery {
            changeMessagesStarredStatus(
                userId,
                listOf(messageId),
                ChangeMessagesStarredStatus.Action.ACTION_STAR
            )
        } just Runs
        every {
            savedStateHandle.get<ActionSheetTarget>("extra_arg_action_sheet_actions_target")
        } returns ActionSheetTarget.MESSAGE_ITEM_WITHIN_CONVERSATION_DETAIL_SCREEN
        every { accountManager.getPrimaryUserId() } returns flowOf(userId)
        coEvery { changeConversationsStarredStatus(any(), any(), any()) } returns ConversationsActionResult.Success

        // when
        viewModel.starMessage(
            listOf(messageId),
            Constants.MessageLocationType.INBOX
        )

        // then
        assertEquals(expected, viewModel.actionsFlow.value)
        coVerify {
            changeMessagesStarredStatus(
                userId,
                listOf(messageId),
                ChangeMessagesStarredStatus.Action.ACTION_STAR
            )
        }
    }

    @Test
    fun verifyStarMessageEmitsResultWithIsSuccessfulFlagFalseIfChangeConversationStarredStatusReturnsErrorResultInConversationMode() {
        // given
        val conversationId = "conversationId"
        val expectedResult = MessageActionSheetAction.ChangeStarredStatus(true, isSuccessful = false)
        every { conversationModeEnabled(any()) } returns true
        coEvery { changeConversationsStarredStatus(any(), any(), any()) } returns ConversationsActionResult.Error

        // when
        viewModel.starMessage(
            listOf(conversationId),
            Constants.MessageLocationType.INBOX
        )

        // then
        assertEquals(expectedResult, viewModel.actionsFlow.value)
    }

    @Test
    fun verifyMarkUnreadCallsMessageRepositoryToMarkUnreadWhenConversationIsEnabledAndActionIsBeingAppliedToASpecificMessageInAConversation() {
        // given
        val messageId = "messageId4"
        val expected = MessageActionSheetAction.DismissActionSheet(false)
        every { conversationModeEnabled(any()) } returns true
        coEvery {
            changeMessagesReadStatus.invoke(
                listOf(messageId),
                ChangeMessagesReadStatus.Action.ACTION_MARK_UNREAD,
                any()
            )
        } just Runs
        every {
            savedStateHandle.get<ActionSheetTarget>("extra_arg_action_sheet_actions_target")
        } returns ActionSheetTarget.MESSAGE_ITEM_WITHIN_CONVERSATION_DETAIL_SCREEN

        // when
        viewModel.markUnread(
            listOf(messageId),
            Constants.MessageLocationType.INBOX,
            Constants.MessageLocationType.INBOX.messageLocationTypeValue.toString()
        )

        // then
        assertEquals(expected, viewModel.actionsFlow.value)
        coVerify {
            changeMessagesReadStatus.invoke(
                listOf(messageId),
                ChangeMessagesReadStatus.Action.ACTION_MARK_UNREAD,
                any()
            )
        }
    }

    @Test
    fun verifyMarkUnreadEmitsCouldNotCompleteActionErrorIfChangeConversationsReadStatusReturnsErrorResultWhenInConversationMode() {
        // given
        val conversationId = "conversationId"
        val expectedResult = MessageActionSheetAction.CouldNotCompleteActionError
        every { conversationModeEnabled(any()) } returns true
        coEvery {
            changeConversationsReadStatus.invoke(any(), any(), any(), any())
        } returns ConversationsActionResult.Error

        // when
        viewModel.markUnread(
            listOf(conversationId),
            Constants.MessageLocationType.INBOX,
            Constants.MessageLocationType.INBOX.messageLocationTypeValue.toString()
        )

        // then
        assertEquals(expectedResult, viewModel.actionsFlow.value)
    }

    @Test
    fun verifyDeleteCallsDeleteMessageWhenCalledForAMessageInsideAConversation() {
        val messageIds = listOf("messageId5")
        val currentFolder = Constants.MessageLocationType.TRASH
        val userId = UserId("userId")
        every { accountManager.getPrimaryUserId() } returns flowOf(userId)
        every { conversationModeEnabled(any()) } returns true
        every {
            savedStateHandle.get<ActionSheetTarget>("extra_arg_action_sheet_actions_target")
        } returns ActionSheetTarget.MESSAGE_ITEM_WITHIN_CONVERSATION_DETAIL_SCREEN

        viewModel.delete(
            messageIds,
            currentFolder,
            true
        )

        coVerify {
            deleteMessage(messageIds, currentFolder.messageLocationTypeValue.toString(), userId)
        }
    }

    @Test
    fun verifyDeleteCallsDeleteConversationWhenCalledForAConversation() {
        val messageIds = listOf("messageId6")
        val currentFolder = Constants.MessageLocationType.SPAM
        val userId = UserId("userId6")
        every { accountManager.getPrimaryUserId() } returns flowOf(userId)
        every { conversationModeEnabled(any()) } returns true
        every {
            savedStateHandle.get<ActionSheetTarget>("extra_arg_action_sheet_actions_target")
        } returns ActionSheetTarget.CONVERSATION_ITEM_IN_DETAIL_SCREEN

        viewModel.delete(
            messageIds,
            currentFolder,
            true
        )

        coVerify {
            deleteConversations(
                messageIds,
                userId,
                currentFolder.messageLocationTypeValue.toString()
            )
        }
    }

    @Test
    fun verifyDeleteEmitsDismissActionSheetWithDismissBackingActivityTrueWhenTargetIsConversationInConversationDetails() {
        // given
        val conversationIds = listOf("conversationId")
        val currentFolder = Constants.MessageLocationType.SPAM
        every { conversationModeEnabled(any()) } returns true
        every {
            savedStateHandle.get<ActionSheetTarget>("extra_arg_action_sheet_actions_target")
        } returns ActionSheetTarget.CONVERSATION_ITEM_IN_DETAIL_SCREEN

        // when
        viewModel.delete(
            conversationIds,
            currentFolder,
            true
        )

        // then
        assertEquals(MessageActionSheetAction.DismissActionSheet(true), viewModel.actionsFlow.value)
    }

    @Test
    fun verifySetupViewStateReturnsMoveSectionStateWithShowMoveToInboxActionTrueWhenActionTargetIsMessageAndMessageLocationIsTrash() {
        val messageIds = listOf("messageId7")
        val currentFolder = Constants.MessageLocationType.TRASH
        val actionsTarget = ActionSheetTarget.MESSAGE_ITEM_WITHIN_CONVERSATION_DETAIL_SCREEN

        viewModel.setupViewState(messageIds, currentFolder, actionsTarget)

        val expected = MessageActionSheetState.MoveSectionState(
            messageIds,
            currentFolder,
            actionsTarget = actionsTarget,
            showMoveToInboxAction = true,
            showMoveToTrashAction = false,
            showMoveToArchiveAction = true,
            showMoveToSpamAction = false,
            showDeleteAction = true
        )
        assertEquals(MessageActionSheetState.Data(expected), viewModel.stateFlow.value)
    }

    @Test
    fun verifySetupViewStateReturnsMoveSectionStateWithShowMoveToInboxActionFalseWhenActionTargetIsMessageAndMessageLocationIsNotArchiveSpamOrTrash() {
        val messageIds = listOf("messageId7")
        val currentFolder = Constants.MessageLocationType.SENT
        val actionsTarget = ActionSheetTarget.MESSAGE_ITEM_WITHIN_CONVERSATION_DETAIL_SCREEN

        viewModel.setupViewState(messageIds, currentFolder, actionsTarget)

        val expected = MessageActionSheetState.MoveSectionState(
            messageIds,
            currentFolder,
            actionsTarget = actionsTarget,
            showMoveToInboxAction = false,
            showMoveToTrashAction = true,
            showMoveToArchiveAction = true,
            showMoveToSpamAction = false,
            showDeleteAction = true
        )
        assertEquals(MessageActionSheetState.Data(expected), viewModel.stateFlow.value)
    }

    @Test
    fun verifySetupViewStateReturnsMoveSectionStateWithShowMoveToInboxActionTrueWhenActionTargetIsConversation() {
        val messageIds = listOf("messageId8")
        val currentFolder = Constants.MessageLocationType.INBOX
        val actionsTarget = ActionSheetTarget.CONVERSATION_ITEM_IN_DETAIL_SCREEN

        viewModel.setupViewState(messageIds, currentFolder, actionsTarget)

        val expected = MessageActionSheetState.MoveSectionState(
            messageIds,
            currentFolder,
            actionsTarget = actionsTarget,
            showMoveToInboxAction = true,
            showMoveToTrashAction = true,
            showMoveToArchiveAction = true,
            showMoveToSpamAction = true,
            showDeleteAction = false
        )
        assertEquals(MessageActionSheetState.Data(expected), viewModel.stateFlow.value)
    }

    @Test
    fun verifySetupViewStateReturnsMoveSectionStateWithShowMoveToTrashActionTrueWhenMessageLocationIsNotTrash() {
        val messageIds = listOf("messageId8")
        val currentFolder = Constants.MessageLocationType.ARCHIVE
        val actionsTarget = ActionSheetTarget.MESSAGE_ITEM_WITHIN_CONVERSATION_DETAIL_SCREEN

        viewModel.setupViewState(messageIds, currentFolder, actionsTarget)

        val expected = MessageActionSheetState.MoveSectionState(
            messageIds,
            currentFolder,
            actionsTarget = actionsTarget,
            showMoveToInboxAction = true,
            showMoveToTrashAction = true,
            showMoveToArchiveAction = false,
            showMoveToSpamAction = true,
            showDeleteAction = false
        )
        assertEquals(MessageActionSheetState.Data(expected), viewModel.stateFlow.value)
    }

    @Test
    fun verifySetupViewStateReturnsMoveSectionStateWithShowMoveToTrashActionFalseWhenMessageLocationIsTrash() {
        val messageIds = listOf("messageId10")
        val currentFolder = Constants.MessageLocationType.TRASH
        val actionsTarget = ActionSheetTarget.MESSAGE_ITEM_WITHIN_CONVERSATION_DETAIL_SCREEN

        viewModel.setupViewState(messageIds, currentFolder, actionsTarget)

        val expected = MessageActionSheetState.MoveSectionState(
            messageIds,
            currentFolder,
            actionsTarget = actionsTarget,
            showMoveToInboxAction = true,
            showMoveToTrashAction = false,
            showMoveToArchiveAction = true,
            showMoveToSpamAction = false,
            showDeleteAction = true
        )
        assertEquals(MessageActionSheetState.Data(expected), viewModel.stateFlow.value)
    }

    @Test
    fun verifySetupViewStateReturnsMoveSectionStateWithShowMoveToArchiveActionTrueWhenActionTargetIsMessageAndMessageLocationIsNotArchiveOrSpam() {
        val messageIds = listOf("messageId11")
        val currentFolder = Constants.MessageLocationType.LABEL
        val actionsTarget = ActionSheetTarget.MESSAGE_ITEM_WITHIN_CONVERSATION_DETAIL_SCREEN

        viewModel.setupViewState(messageIds, currentFolder, actionsTarget)

        val expected = MessageActionSheetState.MoveSectionState(
            messageIds,
            currentFolder,
            actionsTarget = actionsTarget,
            showMoveToInboxAction = false,
            showMoveToTrashAction = true,
            showMoveToArchiveAction = true,
            showMoveToSpamAction = true,
            showDeleteAction = false
        )
        assertEquals(MessageActionSheetState.Data(expected), viewModel.stateFlow.value)
    }

    @Test
    fun verifySetupViewStateReturnsMoveSectionStateWithShowMoveToArchiveActionFalseWhenActionTargetIsMessageAndMessageLocationIsSpam() {
        val messageIds = listOf("messageId12")
        val currentFolder = Constants.MessageLocationType.SPAM
        val actionsTarget = ActionSheetTarget.MESSAGE_ITEM_WITHIN_CONVERSATION_DETAIL_SCREEN

        viewModel.setupViewState(messageIds, currentFolder, actionsTarget)

        val expected = MessageActionSheetState.MoveSectionState(
            messageIds,
            currentFolder,
            actionsTarget = actionsTarget,
            showMoveToInboxAction = true,
            showMoveToTrashAction = true,
            showMoveToArchiveAction = false,
            showMoveToSpamAction = false,
            showDeleteAction = true
        )
        assertEquals(MessageActionSheetState.Data(expected), viewModel.stateFlow.value)
    }

    @Test
    fun verifySetupViewStateReturnsMoveSectionStateWithShowMoveToSpamActionTrueWhenActionTargetIsMessageAndMessageLocationIsNotSpamDraftSentOrTrash() {
        val messageIds = listOf("messageId13")
        val currentFolder = Constants.MessageLocationType.INBOX
        val actionsTarget = ActionSheetTarget.MESSAGE_ITEM_WITHIN_CONVERSATION_DETAIL_SCREEN

        viewModel.setupViewState(messageIds, currentFolder, actionsTarget)

        val expected = MessageActionSheetState.MoveSectionState(
            messageIds,
            currentFolder,
            actionsTarget = actionsTarget,
            showMoveToInboxAction = false,
            showMoveToTrashAction = true,
            showMoveToArchiveAction = true,
            showMoveToSpamAction = true,
            showDeleteAction = false
        )
        assertEquals(MessageActionSheetState.Data(expected), viewModel.stateFlow.value)
    }

    @Test
    fun verifySetupViewStateReturnsMoveSectionStateWithShowMoveToSpamActionFalseWhenActionTargetIsMessageAndMessageLocationIsDraft() {
        val messageIds = listOf("messageId14")
        val currentFolder = Constants.MessageLocationType.DRAFT
        val actionsTarget = ActionSheetTarget.MESSAGE_ITEM_WITHIN_CONVERSATION_DETAIL_SCREEN

        viewModel.setupViewState(messageIds, currentFolder, actionsTarget)

        val expected = MessageActionSheetState.MoveSectionState(
            messageIds,
            currentFolder,
            actionsTarget = actionsTarget,
            showMoveToInboxAction = false,
            showMoveToTrashAction = true,
            showMoveToArchiveAction = true,
            showMoveToSpamAction = false,
            showDeleteAction = true
        )
        assertEquals(MessageActionSheetState.Data(expected), viewModel.stateFlow.value)
    }

    @Test
    fun verifySetupViewStateReturnsMoveSectionStateWithShowDeleteActionTrueWhenMessageLocationIsEitherOfSpamDraftSentOrTrash() {
        val messageIds = listOf("messageId15")
        val currentFolder = Constants.MessageLocationType.DRAFT
        val actionsTarget = ActionSheetTarget.MESSAGE_ITEM_WITHIN_CONVERSATION_DETAIL_SCREEN

        viewModel.setupViewState(messageIds, currentFolder, actionsTarget)

        val expected = MessageActionSheetState.MoveSectionState(
            messageIds,
            currentFolder,
            actionsTarget = actionsTarget,
            showMoveToInboxAction = false,
            showMoveToTrashAction = true,
            showMoveToArchiveAction = true,
            showMoveToSpamAction = false,
            showDeleteAction = true
        )
        assertEquals(MessageActionSheetState.Data(expected), viewModel.stateFlow.value)
    }

    @Test
    fun verifySetupViewStateReturnsMoveSectionStateWithShowDeleteActionFalseWhenMessageLocationIsArchive() {
        val messageIds = listOf("messageId16")
        val currentFolder = Constants.MessageLocationType.ARCHIVE
        val actionsTarget = ActionSheetTarget.MESSAGE_ITEM_WITHIN_CONVERSATION_DETAIL_SCREEN

        viewModel.setupViewState(messageIds, currentFolder, actionsTarget)

        val expected = MessageActionSheetState.MoveSectionState(
            messageIds,
            currentFolder,
            actionsTarget = actionsTarget,
            showMoveToInboxAction = true,
            showMoveToTrashAction = true,
            showMoveToArchiveAction = false,
            showMoveToSpamAction = true,
            showDeleteAction = false
        )
        assertEquals(MessageActionSheetState.Data(expected), viewModel.stateFlow.value)
    }

}
