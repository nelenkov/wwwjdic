package org.nick.wwwjdic.history;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.security.SecureRandom;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.json.JSONException;
import org.nick.wwwjdic.model.WwwjdicEntry;
import org.nick.wwwjdic.utils.MediaScannerWrapper;

import android.content.ContentValues;
import android.content.Context;
import android.content.res.AssetManager;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;
import android.util.Log;

public class AnkiGenerator {

    private static final String TAG = AnkiGenerator.class.getSimpleName();

    private static final long DECK_ID = 1L;
    private static final long MODEL_ID = 1360234932699L;

    private static final int FWD_CARD_ORDINAL = 0;

    // private static final int CARD_TYPE_FAILED = 0;
    // private static final int CARD_TYPE_REV = 1;
    private static final int CARD_TYPE_NEW = 2;

    private static final int GUID_UPPER_LIMIT = (int) (Math.pow(2, 61) - 1);

    private Context context;
    private static SecureRandom random = new SecureRandom();


    private long schemaModTime;

    public AnkiGenerator(Context context) {
        this.context = context;
    }

    private void addToZip(String path, String dbPath)
            throws FileNotFoundException, IOException {
        FileInputStream sqliteIn = new FileInputStream(dbPath);
        File zipFile = new File(path);
        ZipOutputStream zip = new ZipOutputStream(new FileOutputStream(zipFile));

        try {
            zip.putNextEntry(new ZipEntry("collection.anki2"));
            int read = -1;
            byte[] buff = new byte[4 * 1024];
            while ((read = sqliteIn.read(buff)) > 0) {
                zip.write(buff, 0, read);
            }
            zip.flush();
        } catch (IOException e) {
            Log.d(TAG, "Zip error, deleting incomplete file.");
            zipFile.delete();
        } finally {
            zip.close();
        }

        MediaScannerWrapper.scanFile(context, zipFile.getAbsolutePath());
    }

    private void execSqlFromFile(SQLiteDatabase db, String resourceName) {
        String schema = readTextAsset(resourceName);
        String[] statements = schema.split(";");
        for (String s : statements) {
            if (s == null) {
                continue;
            }
            s = s.trim();
            Log.d(TAG, "SQL: " + s);
            if (TextUtils.isEmpty(s) || s.startsWith("--")) {
                continue;
            }
            db.execSQL(s);
        }
    }

    public int createAnkiFile(String path, List<WwwjdicEntry> entries)
            throws IOException, JSONException {
        SQLiteDatabase db = null;

        String dbPath = path.replaceAll("\\.apkg", ".db");
        try {
            db = SQLiteDatabase.openDatabase(dbPath, null,
                    SQLiteDatabase.CREATE_IF_NECESSARY);
            db.beginTransaction();

            execSqlFromFile(db, "anki-create-tables.sql");

            schemaModTime = System.currentTimeMillis();
            createAnkiCollection(db, schemaModTime);

            addEnries(entries, db);

            db.setTransactionSuccessful();
            db.endTransaction();
            db.close();

            addToZip(path, dbPath);

            return entries.size();
        } finally {
            deleteDbFiles(dbPath);
            if (db != null) {
                db.close();
            }
        }
    }

    private void deleteDbFiles(String dbPath) {
        File dbFile = new File(dbPath);
        if (dbFile.exists()) {
            boolean result = dbFile.delete();
            if (!result) {
                Log.w(TAG, "Failed to delete : " + dbFile.getAbsolutePath());
            }
        }
        File journalPath = new File(dbPath + "-journal");
        if (journalPath.exists()) {
            boolean result = journalPath.delete();
            if (!result) {
                Log.w(TAG,
                        "Failed to delete : " + journalPath.getAbsolutePath());
            }
        }
    }

    private long createAnkiCollection(SQLiteDatabase db, long schemaModTime)
            throws JSONException {
        ContentValues values = new ContentValues();
        // from https://gist.github.com/sartak/3921255
        //id integer primary key, -- seems to be an autoincrement
        //crt integer not null, -- there's the created timestamp
        values.put("crt", System.currentTimeMillis());
        //mod integer not null, -- last modified in milliseconds
        values.put("mod", System.currentTimeMillis());
        //scm integer not null, -- a timestamp in milliseconds. "schema mod time" - contributed by Fletcher Moore
        values.put("scm", schemaModTime);
        //ver integer not null, -- version? I have "11"
        values.put("ver", 11);
        //dty integer not null, -- 0
        values.put("dty", 0);
        //usn integer not null, -- 0
        values.put("usn", 0);
        //ls  integer not null, -- "last sync time" - contributed by Fletcher Moore
        values.put("ls", 0);
        //conf text not null, -- json blob of configuration
        String confStr = readTextAsset("conf.txt");
        values.put("conf", confStr);
        //models text not null, -- json object with keys being ids(epoch ms), values being configuration
        String modelStr = readTextAsset("deck-model.txt");
        values.put("models", modelStr);
        //decks text not null, -- json object with keys being ids(epoch ms), values being configuration
        String decksStr = readTextAsset("decks.txt");
        values.put("decks", decksStr);
        //dconf text not null, -- json object. deck configuration?
        String dconfStr = readTextAsset("dconf.txt");
        values.put("dconf", dconfStr);
        //tags text not null -- a cache of tags used in this collection (probably for autocomplete etc)
        values.put("tags", "{}");

        return db.insert("col", null, values);
    }

    private void addEnries(List<WwwjdicEntry> entries, SQLiteDatabase db) {
        for (WwwjdicEntry entry : entries) {
            long noteId = insertNote(db, MODEL_ID, entry);
            insertCard(db, noteId);
        }
    }

    private long insertNote(SQLiteDatabase db, long modelId, WwwjdicEntry entry) {
        ContentValues note = new ContentValues();

        long noteId = generateId();
        //-- epoch seconds of when the note was created
        note.put("id", noteId);
        //-- globally unique id, almost certainly used for syncing
        // weird GUID
        note.put("guid", guid64());
        //-- model id
        note.put("mid", modelId);
        //-- modified timestamp, epoch seconds
        note.put("mod", System.currentTimeMillis() / 1000);
        //-- -1 for all my notes
        note.put("usn", 1);
        //-- space-separated string of tags. seems to include space at the beginning and end of the field, almost certainly for LIKE "% tag %" queries
        note.put("tags", "");
        //-- the values of the fields in this note. separated by 0x1f (31).
        String fields = entry.getHeadword() + createFieldSeparator()
                + nonNullString(entry.getReading()) + "\n"
                + meaningsOnNewLines(entry);
        note.put("flds", fields);
        //-- the text of the first field, used for anki2's new (simplistic) uniqueness checking
        note.put("sfld", entry.getHeadword());
        //-- dunno. not a unique field, but very few repeats
        // schema mod time?
        note.put("csum", schemaModTime);
        //-- 0 for all my notes
        note.put("flags", 0);
        //-- empty string for all my notes
        note.put("data", "");

        db.insert("notes", null, note);

        return noteId;
    }

    private static String nonNullString(String str) {
        return str == null ? "" : str;
    }

    private static String meaningsOnNewLines(WwwjdicEntry entry) {
        List<String> meanings = entry.getMeanings();
        StringBuffer buff = new StringBuffer();
        for (int i = 0; i < meanings.size(); i++) {
            buff.append(meanings.get(i));
            if (i != meanings.size() - 1) {
                buff.append("\n");
            }
        }

        return buff.toString();
    }

    private String createFieldSeparator() {
        try {
            return new String(new byte[] { 0x1f }, "ASCII");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    private long insertCard(SQLiteDatabase db, long noteId) {
        ContentValues card = new ContentValues();
        long cardId = System.currentTimeMillis();//generateId();
        double now = now();

        //-- the epoch milliseconds of when the card was created
        card.put("id", cardId);
        //-- notes.id
        card.put("nid", noteId);
        //-- deck id (available in col table)
        // always 1
        card.put("did", DECK_ID);
        //-- ordinal, seems like. for when a model has multiple templates, or thereabouts
        card.put("ord", FWD_CARD_ORDINAL);
        //-- modified time as epoch seconds
        card.put("mod", (int) now);
        //-- "From the source code it appears Anki increments this number each time you synchronize with AnkiWeb and applies this number to the cards that were synchronized. My database is up to 1230 for the collection and my cards have various numbers up to 1229." -- contributed by Fletcher Moore
        card.put("usn", 0);
        //-- in anki1, type was whether the card was suspended, etc. seems to be the same. values are 0 (suspended), 1 (maybe "learning"?), 2 (normal)
        card.put("type", CARD_TYPE_NEW);
        //-- "queue in the cards table refers to if the card is "new" = 0, "learning" = 1 or 3, "review" = 2 (I don't understand how a 3 occurs, but I have 3 notes out of 23,000 with this value.)" -- contributed by Fletcher Moore
        card.put("queue", 0);
        // ???
        card.put("due", (int) now);
        //-- interval (used in SRS algorithm)
        card.put("ivl", 4);
        //-- factor (used in SRS algorithm)
        card.put("factor", 2500);
        //-- number of reviews
        card.put("reps", 0);
        //-- possibly the number of times the card went from a "was answered correctly" to "was answered incorrectly" state
        card.put("lapses", 0);
        //-- 0 for all my cards
        card.put("left", 0);
        //-- 0 for all my cards
        card.put("odue", 0);
        //-- 0 for all my cards
        card.put("odid", 0);
        //-- 0 for all my cards
        card.put("flags", 0);
        //-- currently unused for decks imported from anki1. maybe extra data for plugins?
        card.put("data", "");

        db.insert("cards", null, card);

        return cardId;
    }

    private long generateId() {
        long time = System.currentTimeMillis();
        int rand = random.nextInt(2 ^ 23);
        long id = rand << 41 | time;

        return id;
    }

    private String readTextAsset(String name) {
        AssetManager assetManager = context.getAssets();

        InputStream in = null;
        try {
            in = assetManager.open(name);

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

    private static double now() {
        return System.currentTimeMillis() / 1000.0;
    }

    private static final String BASE91_CHARS = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz!#$%&()*+,-./:;<=>?@[]^_`{|}~";

    private static String base91(int num) {
        String chars = BASE91_CHARS;
        int len = chars.length();
        StringBuilder buff = new StringBuilder();
        int mod = 0;
        while (num != 0) {
            mod = num % len;
            buff.append(chars.substring(mod, mod + 1));
            num /= len;
        }

        return buff.toString();
    }


    // base91-encoded 32bit random
    private static String guid64() {
        return base91(random.nextInt(GUID_UPPER_LIMIT));
    }

}
