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
package ch.protonmail.android.uitests.robots.mailbox.composer

import androidx.annotation.IdRes
import ch.protonmail.android.R
import ch.protonmail.android.uitests.testsHelper.MockAddAttachmentIntent
import me.proton.core.test.android.instrumented.Robot

/**
 * Class represents Message Attachments.
 */
open class MessageAttachmentsRobot : Robot {

    fun addImageCaptureAttachment(@IdRes drawable: Int): ComposerRobot =
        mockCameraImageCapture(drawable)

    fun addTwoImageCaptureAttachments(
        @IdRes firstDrawable: Int,
        @IdRes secondDrawable: Int
    ): ComposerRobot =
        mockCameraImageCapture(firstDrawable)
            .attachments()
            .mockCameraImageCapture(secondDrawable)

    fun addFileAttachment(@IdRes drawable: Int): MessageAttachmentsRobot {
        mockFileAttachment(drawable)
        return this
    }

    fun removeLastAttachment(): MessageAttachmentsRobot {
        return this
    }

    fun goBackToComposer(): ComposerRobot {
        return ComposerRobot()
    }

    private fun mockCameraImageCapture(@IdRes drawableId: Int): ComposerRobot {
        view.withId(takePhotoIconId).checkDisplayed()
        MockAddAttachmentIntent.mockCameraImageCapture(takePhotoIconId, drawableId)
        return ComposerRobot()
    }

    private fun mockFileAttachment(@IdRes drawable: Int): ComposerRobot {
        view.withId(addAttachmentIconId).checkDisplayed()
        MockAddAttachmentIntent.mockChooseAttachment(addAttachmentIconId, drawable)
        return ComposerRobot()
    }

    companion object {
        private const val takePhotoIconId = R.id.take_photo
        private const val addAttachmentIconId = R.id.attach_file
    }
}