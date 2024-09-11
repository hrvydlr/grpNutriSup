package com.myapp.grpnutrisup

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_VERSION = 1
        private const val DATABASE_NAME = "nutrisup.db"
        private const val TABLE_USERS = "users"

        private const val COLUMN_ID = "id"
        private const val COLUMN_USERNAME = "username"
        private const val COLUMN_PASSWORD = "password"
        private const val COLUMN_AGE = "age"
        private const val COLUMN_GENDER = "gender"
        private const val COLUMN_HEIGHT = "height"
        private const val COLUMN_WEIGHT = "weight"
    }

    override fun onCreate(db: SQLiteDatabase?) {
        val createUsersTable = ("CREATE TABLE $TABLE_USERS ("
                + "$COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "$COLUMN_USERNAME TEXT, "
                + "$COLUMN_PASSWORD TEXT, "
                + "$COLUMN_AGE INTEGER, "
                + "$COLUMN_GENDER TEXT, "
                + "$COLUMN_HEIGHT REAL, "
                + "$COLUMN_WEIGHT REAL)")
        db?.execSQL(createUsersTable)
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        db?.execSQL("DROP TABLE IF EXISTS $TABLE_USERS")
        onCreate(db)
    }

    // Insert user login credentials into the database
    fun addUser(username: String, password: String): Long {
        val db = this.writableDatabase
        val values = ContentValues()
        values.put(COLUMN_USERNAME, username)
        values.put(COLUMN_PASSWORD, password)

        return db.insert(TABLE_USERS, null, values)
    }

    // Insert user details into the database
    fun addUserDetails(username: String, age: Int, gender: String, height: Float, weight: Float): Long {
        val db = this.writableDatabase
        val values = ContentValues()
        values.put(COLUMN_AGE, age)
        values.put(COLUMN_GENDER, gender)
        values.put(COLUMN_HEIGHT, height)
        values.put(COLUMN_WEIGHT, weight)

        // Update the existing user with additional details
        return db.update(TABLE_USERS, values, "$COLUMN_USERNAME = ?", arrayOf(username)).toLong()
    }

    // Check if a user exists in the database
    fun checkUser(username: String, password: String): Boolean {
        val db = this.readableDatabase
        val cursor: Cursor = db.query(
            TABLE_USERS,
            arrayOf(COLUMN_ID),
            "$COLUMN_USERNAME = ? AND $COLUMN_PASSWORD = ?",
            arrayOf(username, password),
            null,
            null,
            null
        )

        val count = cursor.count
        cursor.close()
        db.close()

        return count > 0
    }

    // Retrieve user details (for example, to display them in a profile)
    fun getUserDetails(username: String): Cursor? {
        val db = this.readableDatabase
        return db.query(
            TABLE_USERS,
            arrayOf(COLUMN_AGE, COLUMN_GENDER, COLUMN_HEIGHT, COLUMN_WEIGHT),
            "$COLUMN_USERNAME = ?",
            arrayOf(username),
            null,
            null,
            null
        )
    }
}
