package com.example.ui.screen

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.window.PopupPositionProvider

class ToolbarPopupPositionProvider(
    private val rect: Rect,
    private val isSubMenu: Boolean,
    private val preferredPosition: IntOffset?,
    private val onPositionCalculated: (isBelow: Boolean, offset: IntOffset, arrowX: Float) -> Unit
) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize
    ): IntOffset {
        var x = if (isSubMenu) {
            preferredPosition?.x ?: (windowSize.width - popupContentSize.width - 48)
        } else {
            rect.left.toInt() + (rect.width.toInt() - popupContentSize.width) / 2
        }
        
        var y = if (isSubMenu && preferredPosition != null) {
            preferredPosition.y
        } else {
            rect.top.toInt() - popupContentSize.height - 16
        }
        
        var isBelow = false
        
        if (x < 16) x = 16
        if (x + popupContentSize.width > windowSize.width - 16) {
            x = windowSize.width - popupContentSize.width - 16
        }
        
        if (y < 16) {
            y = rect.bottom.toInt() + 16
            isBelow = true
        }
        
        // Also constrain y to prevent going off-screen at the bottom
        if (y + popupContentSize.height > windowSize.height - 16) {
            y = windowSize.height - popupContentSize.height - 16
        }
        if (y < 16) {
            y = 16
        }
        
        val anchorCenterX = rect.left + rect.width / 2f
        val arrowX = anchorCenterX - x

        val finalOffset = IntOffset(x, y)
        onPositionCalculated(isBelow, finalOffset, arrowX)
        return finalOffset
    }
}
