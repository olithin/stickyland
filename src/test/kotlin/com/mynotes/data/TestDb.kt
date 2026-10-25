package com.mynotes.data

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import java.nio.file.Files

/**
 * Fresh temp data dir + SQLite for each test so repository tests stay isolated.
 */
abstract class RepositoryTestBase {
    protected lateinit var repo: NotesRepository
    private lateinit var tempDir: java.io.File

    @BeforeEach
    fun setUpDb() {
        tempDir = Files.createTempDirectory("stickyland-test").toFile()
        AppPaths.redirectForTests(tempDir)
        DatabaseFactory.init()
        repo = NotesRepository()
    }

    @AfterEach
    fun tearDownDb() {
        AppPaths.clearTestRedirect()
        tempDir.deleteRecursively()
    }
}
