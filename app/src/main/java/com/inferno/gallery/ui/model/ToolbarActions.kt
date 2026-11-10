package com.inferno.gallery.ui.model

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.inferno.gallery.R

sealed class ViewerToolbarAction(
    val id: String,
    @param:StringRes val labelRes: Int,
    @param:DrawableRes val iconRes: Int
) {
    object Share : ViewerToolbarAction("share", R.string.detail_share, R.drawable.ic_ms_share)
    object Favorite : ViewerToolbarAction("favorite", R.string.detail_favorite, R.drawable.ic_ms_favorite)
    object Edit : ViewerToolbarAction("edit", R.string.detail_edit, R.drawable.ic_ms_tune)
    object Info : ViewerToolbarAction("info", R.string.detail_details, R.drawable.ic_ms_info)
    object Delete : ViewerToolbarAction("delete", R.string.detail_delete, R.drawable.ic_ms_delete)
    object Move : ViewerToolbarAction("move", R.string.action_move, R.drawable.ic_ms_drive_file_move)
    object Vault : ViewerToolbarAction("vault", R.string.action_hide, R.drawable.ic_ms_visibility_off)
    object Wallpaper : ViewerToolbarAction("wallpaper", R.string.detail_set_wallpaper, R.drawable.ic_ms_image)
    object Rotate : ViewerToolbarAction("rotate", R.string.action_rotate, R.drawable.ic_ms_rotate_right)
    object Copy : ViewerToolbarAction("copy", R.string.action_copy, R.drawable.ic_ms_content_copy)

    companion object {
        val ALL: List<ViewerToolbarAction> = listOf(
            Share, Favorite, Edit, Info, Delete, Move, Vault, Wallpaper, Rotate, Copy
        )

        val DEFAULTS: List<String> = listOf("share", "favorite", "edit", "info", "delete")

        fun fromId(id: String): ViewerToolbarAction? = ALL.find { it.id == id }

        fun sanitize(actions: List<String>): List<String> {
            val valid = actions.filter { fromId(it) != null }.distinct()
            return if (valid.size < 2) {
                DEFAULTS
            } else {
                valid.take(6)
            }
        }
    }
}

sealed class SelectionDockAction(
    val id: String,
    @param:StringRes val labelRes: Int,
    @param:DrawableRes val iconRes: Int
) {
    object Share : SelectionDockAction("share", R.string.detail_share, R.drawable.ic_ms_share)
    object Copy : SelectionDockAction("copy", R.string.action_copy, R.drawable.ic_ms_content_copy)
    object Move : SelectionDockAction("move", R.string.action_move, R.drawable.ic_ms_drive_file_move)
    object Create : SelectionDockAction("create", R.string.action_create, R.drawable.ic_ms_add)
    object Delete : SelectionDockAction("delete", R.string.detail_delete, R.drawable.ic_ms_delete)
    object Vault : SelectionDockAction("vault", R.string.action_hide, R.drawable.ic_ms_visibility_off)
    object SelectAll : SelectionDockAction("selectAll", R.string.action_select_all, R.drawable.ic_ms_select_all)

    companion object {
        val ALL: List<SelectionDockAction> = listOf(
            Share, Copy, Move, Create, Delete, Vault, SelectAll
        )

        val DEFAULTS: List<String> = listOf("share", "copy", "move", "create", "delete")

        fun fromId(id: String): SelectionDockAction? = ALL.find { it.id == id }

        fun sanitize(actions: List<String>): List<String> {
            val valid = actions.filter { fromId(it) != null }.distinct()
            return if (valid.size < 2) {
                DEFAULTS
            } else {
                valid.take(6)
            }
        }
    }
}
