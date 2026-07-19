package com.mynotes.data

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption

object AppPaths {
    private const val DATA_DIR_ENV = "STICKYLAND_DATA_DIR"
    private const val DATA_FOLDER = "Stickyland"

    /** Root folder for notes.db and images/ */
    val dataDir: File = resolveDataDir()

    val databaseFile: File = dataDir.resolve("notes.db")
    val imagesDir: File = dataDir.resolve("images").also { it.mkdirs() }

    fun noteImagesDir(noteId: String): File =
        imagesDir.resolve(noteId).also { it.mkdirs() }

    private fun resolveDataDir(): File {
        val dir = dataDirFromEnv() ?: defaultDataDirForOs()
        ensureMigratedFromLegacy(dir)
        dir.mkdirs()
        return dir
    }

    private fun dataDirFromEnv(): File? {
        val custom = System.getenv(DATA_DIR_ENV)?.trim().orEmpty()
            .ifEmpty { System.getenv("MYNOTES_DATA_DIR")?.trim().orEmpty() }
        if (custom.isEmpty()) return null
        return File(custom)
    }

    private fun defaultDataDirForOs(): File {
        val home = System.getProperty("user.home")
        val os = System.getProperty("os.name").lowercase()
        return when {
            os.contains("win") -> windowsDataDir(home)
            os.contains("mac") -> File(home, "Library/Application Support/$DATA_FOLDER")
            else -> File(home, ".stickyland")
        }
    }

    private fun windowsDataDir(home: String): File {
        val dDriveDir = File("D:\\$DATA_FOLDER")
        if (driveAvailable("D:\\") && isWritableOrCreatable(dDriveDir)) {
            return dDriveDir
        }
        val localAppData = System.getenv("LOCALAPPDATA") ?: home
        return File(localAppData, DATA_FOLDER)
    }

    private fun driveAvailable(root: String): Boolean =
        runCatching { File(root).exists() }.getOrDefault(false)

    private fun isWritableOrCreatable(dir: File): Boolean =
        runCatching {
            dir.mkdirs()
            dir.exists() && dir.canWrite()
        }.getOrDefault(false)

    private fun legacyCandidates(): List<File> {
        val home = System.getProperty("user.home")
        val os = System.getProperty("os.name").lowercase()
        val localAppData = System.getenv("LOCALAPPDATA") ?: home
        return when {
            os.contains("win") -> listOf(
                File("D:\\MyNotesNotion"),
                File(localAppData, "MyNotesNotion"),
                File(localAppData, "Stickyland")
            )
            os.contains("mac") -> listOf(
                File(home, "Library/Application Support/MyNotesNotion"),
                File(home, "Library/Application Support/Stickyland")
            )
            else -> listOf(
                File(home, ".mynotes-notion"),
                File(home, ".stickyland")
            )
        }
    }

    /** Copy notes from previous app folders on first run after rename / path change. */
    private fun ensureMigratedFromLegacy(targetDir: File) {
        val targetDb = targetDir.resolve("notes.db")
        if (targetDb.exists()) return

        for (legacyDir in legacyCandidates()) {
            if (targetDir.absolutePath.equals(legacyDir.absolutePath, ignoreCase = true)) continue
            val legacyDb = legacyDir.resolve("notes.db")
            if (!legacyDb.exists()) continue

            targetDir.mkdirs()
            Files.copy(legacyDb.toPath(), targetDb.toPath(), StandardCopyOption.REPLACE_EXISTING)
            val legacyImages = legacyDir.resolve("images")
            if (legacyImages.exists()) {
                legacyImages.copyRecursively(targetDir.resolve("images"), overwrite = true)
            }
            return
        }
    }
}

object DatabaseFactory {
    fun init() {
        val url = "jdbc:sqlite:${AppPaths.databaseFile.absolutePath}"
        Database.connect(url, driver = "org.sqlite.JDBC")
        transaction {
            SchemaUtils.createMissingTablesAndColumns(
                SuitesTable,
                NotesTable,
                NoteImagesTable,
                SchemaNodesTable,
                SchemaEdgesTable
            )
        }
    }
}
