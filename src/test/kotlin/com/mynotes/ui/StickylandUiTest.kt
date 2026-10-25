package com.mynotes.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import com.mynotes.data.AppPaths
import com.mynotes.data.DatabaseFactory
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.nio.file.Files

/**
 * Compose Desktop UI tests — Playwright-style find / click / type / assert.
 *
 * Playwright itself targets browsers; Stickyland is a Compose Desktop JVM app,
 * so these official Compose UI tests are the correct equivalent.
 */
class StickylandUiTest {

    @get:Rule
    val rule = createComposeRule()

    private lateinit var tempDir: java.io.File
    private lateinit var viewModel: AppViewModel

    @Before
    fun setUp() {
        tempDir = Files.createTempDirectory("stickyland-ui").toFile()
        AppPaths.redirectForTests(tempDir)
        DatabaseFactory.init()
        viewModel = AppViewModel().also { it.loadInitialData() }
    }

    @After
    fun tearDown() {
        viewModel.dispose()
        AppPaths.clearTestRedirect()
        tempDir.deleteRecursively()
    }

    private fun setApp() {
        rule.setContent { App(viewModel) }
        rule.waitUntil(timeoutMillis = 5_000) {
            rule.onAllNodesWithTag(TestTags.APP_ROOT).fetchSemanticsNodes().isNotEmpty()
        }
        rule.waitUntil(timeoutMillis = 5_000) {
            viewModel.state.suites.any { it.name == "General" }
        }
    }

    @Test
    fun loadsDefaultSuiteAndNotesChrome() {
        setApp()
        rule.onNodeWithTag(TestTags.APP_ROOT).assertIsDisplayed()
        rule.onNodeWithTag(TestTags.MODE_NOTES).assertIsDisplayed()
        rule.onNodeWithTag(TestTags.MODE_SCHEMA).assertIsDisplayed()
        rule.onNodeWithTag(TestTags.BTN_NEW_SUITE).assertIsDisplayed()
        rule.onNodeWithTag(TestTags.SEARCH_FIELD).assertIsDisplayed()
        rule.onNodeWithText("SUITES").assertIsDisplayed()

        val suiteId = viewModel.state.suites.first { it.name == "General" }.id
        rule.onNodeWithTag(TestTags.suite(suiteId)).assertIsDisplayed()
    }

    @Test
    fun createsSuiteViaDialog() {
        setApp()
        rule.onNodeWithTag(TestTags.BTN_NEW_SUITE).performClick()
        rule.onNodeWithText("New suite").assertIsDisplayed()
        rule.onNodeWithTag(TestTags.DIALOG_INPUT).performTextInput("AWS")
        rule.onNodeWithTag(TestTags.DIALOG_CONFIRM).performClick()

        rule.waitUntil(timeoutMillis = 5_000) {
            viewModel.state.suites.any { it.name == "AWS" }
        }
        val awsId = viewModel.state.suites.first { it.name == "AWS" }.id
        rule.onNodeWithTag(TestTags.suite(awsId)).assertIsDisplayed()
    }

    @Test
    fun createsNoteAndEditsTitle() {
        setApp()
        rule.onNodeWithTag(TestTags.BTN_NEW_NOTE).performClick()

        rule.waitUntil(timeoutMillis = 5_000) {
            rule.onAllNodesWithTag(TestTags.NOTE_EDITOR).fetchSemanticsNodes().isNotEmpty()
        }
        rule.onNodeWithTag(TestTags.NOTE_EDITOR).assertIsDisplayed()
        rule.onNodeWithTag(TestTags.NOTE_TITLE).performClick()
        rule.onNodeWithTag(TestTags.NOTE_TITLE).performTextClearance()
        rule.onNodeWithTag(TestTags.NOTE_TITLE).performTextInput("My first note")

        rule.waitUntil(timeoutMillis = 5_000) {
            viewModel.state.currentNote?.note?.title == "My first note"
        }
        rule.onNodeWithTag(TestTags.NOTE_TITLE).assertTextEquals("My first note")
    }

    @Test
    fun switchesToSchemaPanel() {
        setApp()
        rule.onNodeWithTag(TestTags.MODE_SCHEMA).performClick()
        rule.waitUntil(timeoutMillis = 5_000) {
            rule.onAllNodesWithTag(TestTags.SCHEMA_PANEL).fetchSemanticsNodes().isNotEmpty()
        }
        rule.onNodeWithTag(TestTags.SCHEMA_PANEL).assertIsDisplayed()
    }

    @Test
    fun searchFindsNoteByTitle() {
        setApp()
        rule.onNodeWithTag(TestTags.BTN_NEW_NOTE).performClick()
        rule.waitUntil(timeoutMillis = 5_000) {
            rule.onAllNodesWithTag(TestTags.NOTE_TITLE).fetchSemanticsNodes().isNotEmpty()
        }
        rule.onNodeWithTag(TestTags.NOTE_TITLE).performClick()
        rule.onNodeWithTag(TestTags.NOTE_TITLE).performTextClearance()
        rule.onNodeWithTag(TestTags.NOTE_TITLE).performTextInput("UniqueSearchHit")

        rule.waitUntil(timeoutMillis = 5_000) {
            viewModel.state.notes.any { it.title == "UniqueSearchHit" }
        }
        // Title edits are debounced to SQLite (~400ms); search reads the DB.
        Thread.sleep(800)
        rule.waitForIdle()

        rule.onNodeWithTag(TestTags.SEARCH_FIELD).performTextInput("UniqueSearch")
        rule.waitUntil(timeoutMillis = 5_000) {
            viewModel.state.isSearching && viewModel.state.searchResults.any {
                it.first.title == "UniqueSearchHit"
            }
        }
        // SectionHeader uppercases labels.
        rule.onNodeWithText("SEARCH RESULTS").assertIsDisplayed()
        org.junit.Assert.assertTrue(
            rule.onAllNodesWithText("UniqueSearchHit").fetchSemanticsNodes().isNotEmpty()
        )
    }
}
