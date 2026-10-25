package com.mynotes.ui

/** Stable semantics tags for Compose UI tests (Playwright-style selectors). */
object TestTags {
    const val APP_ROOT = "app_root"
    const val MODE_NOTES = "mode_notes"
    const val MODE_SCHEMA = "mode_schema"
    const val SEARCH_FIELD = "search_field"
    const val BTN_NEW_SUITE = "btn_new_suite"
    const val BTN_NEW_NOTE = "btn_new_note"
    const val NOTE_LIST = "note_list"
    const val NOTE_TITLE = "note_title"
    const val NOTE_EDITOR = "note_editor"
    const val SCHEMA_PANEL = "schema_panel"
    const val DIALOG_INPUT = "dialog_input"
    const val DIALOG_CONFIRM = "dialog_confirm"
    const val DIALOG_CANCEL = "dialog_cancel"

    fun suite(id: String) = "suite_$id"
    fun note(id: String) = "note_$id"
}
