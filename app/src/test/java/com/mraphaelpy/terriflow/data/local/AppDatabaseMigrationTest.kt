package com.mraphaelpy.terriflow.data.local

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = android.app.Application::class)
class AppDatabaseMigrationTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private lateinit var currentSchema: List<SchemaObject>

    @Before
    fun readCurrentSchema() {
        val db = Room.databaseBuilder(context, AppDatabase::class.java, REFERENCE_DB).build()
        currentSchema = db.openHelper.writableDatabase.query(
            "SELECT type, tbl_name, sql FROM sqlite_master WHERE sql IS NOT NULL " +
                "AND name NOT LIKE 'sqlite_%' AND name NOT LIKE 'android_%' AND tbl_name != 'room_master_table'"
        ).use { cursor ->
            buildList { while (cursor.moveToNext()) add(SchemaObject(cursor.getString(0), cursor.getString(1), cursor.getString(2))) }
        }
        db.close()
        context.deleteDatabase(REFERENCE_DB)
    }

    @After
    fun cleanUp() {
        context.deleteDatabase(MIGRATED_DB)
    }

    @Test
    fun `todas as versoes antigas migram ate a atual mantendo os dados`() {
        for (startVersion in 1 until LATEST_VERSION) {
            createDatabaseAt(startVersion)

            val db = Room.databaseBuilder(context, AppDatabase::class.java, MIGRATED_DB)
                .addMigrations(*AppDatabase.ALL_MIGRATIONS)
                .build()
            val sqlite = db.openHelper.writableDatabase

            assertEquals("versão final a partir da $startVersion", LATEST_VERSION, sqlite.version)
            for (table in SAMPLE_TABLES) {
                val ids = sqlite.query("SELECT id FROM $table").use { c -> buildList { while (c.moveToNext()) add(c.getString(0)) } }
                assertEquals("dados de $table a partir da versão $startVersion", listOf("sample"), ids)
            }
            db.close()
            context.deleteDatabase(MIGRATED_DB)
        }
    }

    @Test
    fun `migracoes registradas cobrem todas as versoes sem lacunas`() {
        val steps = AppDatabase.ALL_MIGRATIONS.map { it.startVersion to it.endVersion }
        assertEquals((1 until LATEST_VERSION).map { it to it + 1 }, steps)
    }

    private fun createDatabaseAt(version: Int) {
        val removed = COLUMNS_ADDED.filterKeys { it > version }.values.flatten()
        val db = SQLiteDatabase.openOrCreateDatabase(context.getDatabasePath(MIGRATED_DB).apply { parentFile?.mkdirs() }, null)
        currentSchema.forEach { obj ->
            val sql = if (obj.type == "table") withoutColumns(obj.sql, removed.filter { it.first == obj.table }.map { it.second }) else obj.sql
            if (obj.type == "index" && removed.any { it.first == obj.table && obj.sql.contains("`${it.second}`") }) return@forEach
            db.execSQL(sql)
        }
        SAMPLE_TABLES.forEach { table -> insertSample(db, table) }
        db.version = version
        db.close()
    }

    private fun insertSample(db: SQLiteDatabase, table: String) {
        val columns = db.rawQuery("PRAGMA table_info(`$table`)", null).use { c ->
            buildList { while (c.moveToNext()) add(c.getString(1) to c.getString(2)) }
        }
        val values = columns.map { (name, type) ->
            when {
                name == "id" -> "'sample'"
                type.equals("TEXT", ignoreCase = true) -> "''"
                else -> "0"
            }
        }
        db.execSQL("INSERT INTO `$table` (${columns.joinToString { "`${it.first}`" }}) VALUES (${values.joinToString()})")
    }

    private fun withoutColumns(createSql: String, columns: List<String>): String {
        if (columns.isEmpty()) return createSql
        val open = createSql.indexOf('(')
        val body = createSql.substring(open + 1, createSql.lastIndexOf(')'))
        val kept = splitTopLevel(body).filter { definition -> columns.none { definition.trim().startsWith("`$it`") } }
        return createSql.substring(0, open + 1) + kept.joinToString(",") + ")"
    }

    private fun splitTopLevel(body: String): List<String> {
        val parts = mutableListOf<String>()
        var depth = 0
        var start = 0
        body.forEachIndexed { i, ch ->
            when (ch) {
                '(' -> depth++
                ')' -> depth--
                ',' -> if (depth == 0) { parts += body.substring(start, i); start = i + 1 }
            }
        }
        parts += body.substring(start)
        return parts
    }

    private data class SchemaObject(val type: String, val table: String, val sql: String)

    private companion object {
        const val LATEST_VERSION = 10
        const val REFERENCE_DB = "reference.db"
        const val MIGRATED_DB = "migrated.db"
        val SAMPLE_TABLES = listOf("users", "territories", "territory_events")

        val COLUMNS_ADDED: Map<Int, List<Pair<String, String>>> = mapOf(
            2 to listOf("territories" to "latitude", "territories" to "longitude"),
            3 to listOf("territories" to "boundaryPoints"),
            4 to listOf("users" to "photoUrl"),
            5 to listOf("territories" to "blockPolygons"),
            6 to listOf("territories" to "currentResponsiblePhotoUrl"),
            7 to listOf("users" to "congregationId"),
            8 to listOf("territory_events" to "congregationId"),
            9 to listOf("territories" to "pastResponsibleIds"),
            10 to listOf("territories" to "completedBlocks")
        )
    }
}
