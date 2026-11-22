package com.dizop.print.data.local

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class PrintHistoryRecord(
    val id: Long = 0,
    val title: String,
    val printerName: String,
    val timestamp: Long = System.currentTimeMillis(),
    val status: String
)

data class SavedPrinterRecord(
    val id: Long = 0,
    val name: String,
    val host: String,
    val port: Int,
    val protocol: String
)

class DizopDatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        const val DATABASE_NAME = "dizop_print.db"
        const val DATABASE_VERSION = 1

        const val TABLE_HISTORY = "print_history"
        const val COL_HIST_ID = "id"
        const val COL_HIST_TITLE = "title"
        const val COL_HIST_PRINTER = "printer_name"
        const val COL_HIST_TIMESTAMP = "timestamp"
        const val COL_HIST_STATUS = "status"

        const val TABLE_SAVED_PRINTERS = "saved_printers"
        const val COL_PRINTER_ID = "id"
        const val COL_PRINTER_NAME = "name"
        const val COL_PRINTER_HOST = "host"
        const val COL_PRINTER_PORT = "port"
        const val COL_PRINTER_PROTOCOL = "protocol"
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE $TABLE_HISTORY (
                $COL_HIST_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_HIST_TITLE TEXT NOT NULL,
                $COL_HIST_PRINTER TEXT NOT NULL,
                $COL_HIST_TIMESTAMP INTEGER NOT NULL,
                $COL_HIST_STATUS TEXT NOT NULL
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE $TABLE_SAVED_PRINTERS (
                $COL_PRINTER_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_PRINTER_NAME TEXT NOT NULL,
                $COL_PRINTER_HOST TEXT NOT NULL UNIQUE,
                $COL_PRINTER_PORT INTEGER NOT NULL,
                $COL_PRINTER_PROTOCOL TEXT NOT NULL
            )
            """.trimIndent()
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_HISTORY")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_SAVED_PRINTERS")
        onCreate(db)
    }

    suspend fun insertHistory(record: PrintHistoryRecord): Long = withContext(Dispatchers.IO) {
        val values = ContentValues().apply {
            put(COL_HIST_TITLE, record.title)
            put(COL_HIST_PRINTER, record.printerName)
            put(COL_HIST_TIMESTAMP, record.timestamp)
            put(COL_HIST_STATUS, record.status)
        }
        writableDatabase.insert(TABLE_HISTORY, null, values)
    }

    suspend fun getRecentHistory(limit: Int = 20): List<PrintHistoryRecord> = withContext(Dispatchers.IO) {
        val list = mutableListOf<PrintHistoryRecord>()
        val cursor = readableDatabase.query(
            TABLE_HISTORY,
            null,
            null,
            null,
            null,
            null,
            "$COL_HIST_TIMESTAMP DESC",
            limit.toString()
        )
        cursor.use { c ->
            while (c.moveToNext()) {
                list.add(
                    PrintHistoryRecord(
                        id = c.getLong(c.getColumnIndexOrThrow(COL_HIST_ID)),
                        title = c.getString(c.getColumnIndexOrThrow(COL_HIST_TITLE)),
                        printerName = c.getString(c.getColumnIndexOrThrow(COL_HIST_PRINTER)),
                        timestamp = c.getLong(c.getColumnIndexOrThrow(COL_HIST_TIMESTAMP)),
                        status = c.getString(c.getColumnIndexOrThrow(COL_HIST_STATUS))
                    )
                )
            }
        }
        list
    }

    suspend fun savePrinter(printer: SavedPrinterRecord): Long = withContext(Dispatchers.IO) {
        val values = ContentValues().apply {
            put(COL_PRINTER_NAME, printer.name)
            put(COL_PRINTER_HOST, printer.host)
            put(COL_PRINTER_PORT, printer.port)
            put(COL_PRINTER_PROTOCOL, printer.protocol)
        }
        writableDatabase.insertWithOnConflict(
            TABLE_SAVED_PRINTERS,
            null,
            values,
            SQLiteDatabase.CONFLICT_REPLACE
        )
    }

    suspend fun getAllSavedPrinters(): List<SavedPrinterRecord> = withContext(Dispatchers.IO) {
        val list = mutableListOf<SavedPrinterRecord>()
        val cursor = readableDatabase.query(
            TABLE_SAVED_PRINTERS,
            null,
            null,
            null,
            null,
            null,
            "$COL_PRINTER_NAME ASC"
        )
        cursor.use { c ->
            while (c.moveToNext()) {
                list.add(
                    SavedPrinterRecord(
                        id = c.getLong(c.getColumnIndexOrThrow(COL_PRINTER_ID)),
                        name = c.getString(c.getColumnIndexOrThrow(COL_PRINTER_NAME)),
                        host = c.getString(c.getColumnIndexOrThrow(COL_PRINTER_HOST)),
                        port = c.getInt(c.getColumnIndexOrThrow(COL_PRINTER_PORT)),
                        protocol = c.getString(c.getColumnIndexOrThrow(COL_PRINTER_PROTOCOL))
                    )
                )
            }
        }
        list
    }
}
