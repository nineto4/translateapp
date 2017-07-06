package com.howtoandtutorial.translateapp.model;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;
import android.widget.Toast;

/**
 * Created by Dao on 5/25/2017.
 * Database:
 * - Functions:
 * Create databse, insert, delete History
 */

public class Database extends SQLiteOpenHelper {

    private SQLiteDatabase database;
    private SQLiteStatement sqLiteStatement;
    private final static String DB_NAME = "translate.sqlite";
    private static final int DATABASE_VERSION = 1;
    private String sql;

    public Database(Context context) {
        super(context, DB_NAME, null, DATABASE_VERSION);
    }

    public void createTableHistory(){
        sql = "CREATE TABLE IF NOT EXISTS History" +
                "(Id INTEGER PRIMARY KEY AUTOINCREMENT, TextKey NVARCHAR(200), " +
                "TextResult NVARCHAR(200), Lang VARCHAR(20))";
        queryData(sql);
    }

    public void insertTableHistory(String textKey, String textResult, String lang){
        database = getWritableDatabase();
        sqLiteStatement =
                database.compileStatement("INSERT INTO History VALUES(null, ?, ?, ?)");
        sqLiteStatement.bindString(1, textKey);
        sqLiteStatement.bindString(2, textResult);
        sqLiteStatement.bindString(3, lang);
        sqLiteStatement.execute();
        //listHistory();
    }

    public void deleteTableHistory(int id){
        database = getWritableDatabase();
        sqLiteStatement =
                database.compileStatement("DELETE FROM History WHERE Id = ?");
        sqLiteStatement.bindLong(1, id);
        sqLiteStatement.execute();
        //listHistory();
    }

    //truy van khong tra kq: CREATE, INSERT, UPDATE, DELETE
    public void queryData(String sql){
        database = getWritableDatabase();
        database.execSQL(sql);
    }

    public Cursor getData(String sql){
        database = getReadableDatabase();
        return database.rawQuery(sql, null);
    }


    @Override
    public void onCreate(SQLiteDatabase db) {

    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }
}
