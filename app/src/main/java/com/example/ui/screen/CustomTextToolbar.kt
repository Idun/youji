package com.example.ui.screen

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.TextToolbar
import androidx.compose.ui.platform.TextToolbarStatus

class CustomTextToolbar(
    private val onShowMenu: (Rect, (() -> Unit)?, (() -> Unit)?, (() -> Unit)?, (() -> Unit)?) -> Unit,
    private val onHideMenu: () -> Unit
) : TextToolbar {
    override val status: TextToolbarStatus
        get() = TextToolbarStatus.Hidden // Status is managed externally

    override fun hide() {
        onHideMenu()
    }

    override fun showMenu(
        rect: Rect,
        onCopyRequested: (() -> Unit)?,
        onPasteRequested: (() -> Unit)?,
        onCutRequested: (() -> Unit)?,
        onSelectAllRequested: (() -> Unit)?
    ) {
        onShowMenu(rect, onCopyRequested, onPasteRequested, onCutRequested, onSelectAllRequested)
    }
}
