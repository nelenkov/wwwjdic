package org.nick.wwwjdic.history;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import android.content.Context;
import android.content.res.AssetManager;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;
import android.util.Log;

public class AnkiGenerator {

    private static final String TAG = AnkiGenerator.class.getSimpleName();

    private Context context;

    public AnkiGenerator(Context context) {
        this.context = context;
    }

    public void createAnkiFile(String path) {
        SQLiteDatabase db = null;

        try {
            db = SQLiteDatabase.openDatabase(path, null,
                    SQLiteDatabase.CREATE_IF_NECESSARY);
            String schema = readSchema();
            String[] statements = schema.split(";");
            db.beginTransaction();
            for (String s : statements) {
                Log.d(TAG, "SQL: " + s);
                if (TextUtils.isEmpty(s) || "\n".equals(s)) {
                    continue;
                }
                db.execSQL(s.trim());
            }
            db.setTransactionSuccessful();

        } finally {
            if (db != null) {
                db.endTransaction();
                db.close();
            }
        }
    }

    private String readSchema() {
        AssetManager assetManager = context.getAssets();

        InputStream in = null;
        try {
            in = assetManager.open("anki-create.sql");

            return readTextFile(in);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException ignored) {
                }
            }
        }

    }

    private String readTextFile(InputStream in) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte buff[] = new byte[1024];

        int len = -1;
        while ((len = in.read(buff)) != -1) {
            baos.write(buff, 0, len);
        }

        return baos.toString("ASCII");
    }
}
