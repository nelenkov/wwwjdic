package org.nick.wwwjdic.history;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.List;
import java.util.Random;

import org.nick.wwwjdic.DictionaryEntry;
import org.nick.wwwjdic.KanjiEntry;

import android.content.ContentValues;
import android.content.Context;
import android.content.res.AssetManager;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;
import android.util.Log;

public class AnkiGenerator {

    private static final String TAG = AnkiGenerator.class.getSimpleName();

    private static final long KANJI_MODEL_ID = 4998196432932412600L;
    private static final long KANJI_FWD_CARD_MODEL_ID = 8257311625381387448L;

    private static final long KANJI_FIELD_ID = -4886934269285393224L;
    private static final long ONYOMI_FIELD_ID = 4393069213469613240L;
    private static final long KUNYOMI_FIELD_ID = 760313581623303912L;
    private static final long NANORI_FIELD_ID = -7227726355099557880L;
    private static final long MEANING_FIELD_ID = -3128184056797208296L;

    private static final int KANJI_FIELD_ORD = 0;
    private static final int ONYOMI_FIELD_ORD = 1;
    private static final int KUNYOMI_FIELD_ORD = 2;
    private static final int NANORI_FIELD_ORD = 3;
    private static final int MEANING_FIELD_ORD = 4;

    private static final long DICT_MODEL_ID = -1051435290320934353L;
    private static final long DICT_FWD_CARD_MODEL_ID = -4952737841158526416L;

    private static final long DICT_HEADWORD_FIELD_ID = 7468722094757145135L;
    private static final long DICT_READING_FIELD_ID = -6512467652926215633L;
    private static final long DICT_MEANING_FIELD_ID = 8937385955465940432L;

    private static final int DICT_HEADWORD_ORD = 0;
    private static final int DICT_READING_ORD = 1;
    private static final int DICT_MEANING_ORD = 2;

    private static final String QA_TEMPLATE = "<span class=\"fm3cf7512c968f9cb8\">%s</span><br>";

    private Context context;
    private Random random;

    public AnkiGenerator(Context context) {
        this.context = context;
        try {
            this.random = SecureRandom.getInstance("SHA1PRNG");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    public int createKanjiAnkiFile(String path, List<KanjiEntry> kanjis) {
        SQLiteDatabase db = null;

        try {
            db = SQLiteDatabase.openDatabase(path, null,
                    SQLiteDatabase.CREATE_IF_NECESSARY);
            db.beginTransaction();

            execSqlFromFile(db, "anki-create-tables.sql");

            execSqlFromFile(db, "anki-kanji-model.sql");

            addKanjis(kanjis, db);

            db.setTransactionSuccessful();

            return kanjis.size();
        } finally {
            if (db != null) {
                db.endTransaction();
                db.close();
            }
        }
    }

    private void addKanjis(List<KanjiEntry> kanjis, SQLiteDatabase db) {
        for (KanjiEntry k : kanjis) {
            addKanji(db, k);
        }
    }

    private void addKanji(SQLiteDatabase db, KanjiEntry k) {
        long factId = insertFact(db, KANJI_MODEL_ID);
        String question = generateQuestion(k.getHeadword());
        String answer = generateKanjiAnswer(k);
        insertCard(db, factId, KANJI_FWD_CARD_MODEL_ID, question, answer);

        insertField(db, k.getKanji(), factId, KANJI_FIELD_ID, KANJI_FIELD_ORD);
        if (k.getOnyomi() != null) {
            insertField(db, k.getOnyomi(), factId, ONYOMI_FIELD_ID,
                    ONYOMI_FIELD_ORD);
        }
        if (k.getKunyomi() != null) {
            insertField(db, k.getKunyomi(), factId, KUNYOMI_FIELD_ID,
                    KUNYOMI_FIELD_ORD);
        }
        if (k.getNanori() != null) {
            insertField(db, k.getNanori(), factId, NANORI_FIELD_ID,
                    NANORI_FIELD_ORD);
        }
        if (k.getMeaningsAsString() != null) {
            insertField(db, k.getMeaningsAsString(), factId, MEANING_FIELD_ID,
                    MEANING_FIELD_ORD);
        }
    }

    private void execSqlFromFile(SQLiteDatabase db, String resourceName) {
        String schema = readSchema(resourceName);
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

    public int createDictAnkiFile(String path, List<DictionaryEntry> words) {
        SQLiteDatabase db = null;

        try {
            db = SQLiteDatabase.openDatabase(path, null,
                    SQLiteDatabase.CREATE_IF_NECESSARY);
            db.beginTransaction();

            execSqlFromFile(db, "anki-create-tables.sql");

            execSqlFromFile(db, "anki-dict-model.sql");

            addWords(words, db);

            db.setTransactionSuccessful();

            return words.size();
        } finally {
            if (db != null) {
                db.endTransaction();
                db.close();
            }
        }
    }

    private void addWords(List<DictionaryEntry> words, SQLiteDatabase db) {
        for (DictionaryEntry w : words) {
            addWord(db, w);
        }
    }

    private void addWord(SQLiteDatabase db, DictionaryEntry d) {
        long factId = insertFact(db, DICT_MODEL_ID);
        String question = generateQuestion(d.getHeadword());
        String answer = generateDictAnswer(d);
        insertCard(db, factId, DICT_FWD_CARD_MODEL_ID, question, answer);

        insertField(db, d.getHeadword(), factId, DICT_HEADWORD_FIELD_ID,
                DICT_HEADWORD_ORD);
        if (d.getReading() != null) {
            insertField(db, d.getReading(), factId, DICT_READING_FIELD_ID,
                    DICT_READING_ORD);
        }
        if (d.getMeaningsAsString() != null) {
            insertField(db, d.getMeaningsAsString(), factId,
                    DICT_MEANING_FIELD_ID, DICT_MEANING_ORD);
        }
    }

    private long insertFact(SQLiteDatabase db, long modelId) {
        ContentValues fact = new ContentValues();
        long factId = generateId();
        fact.put("id", factId);
        fact.put("modelId", modelId);
        long now = System.currentTimeMillis();
        fact.put("created", now);
        fact.put("modified", now);
        fact.put("tags", "");
        fact.put("spaceUntil", 0.0);
        db.insert("facts", null, fact);

        return factId;
    }

    private long insertCard(SQLiteDatabase db, long factId, long cardModelId,
            String question, String answer) {
        ContentValues card = new ContentValues();
        long cardId = generateId();
        long now = System.currentTimeMillis();
        card.put("id", cardId);
        card.put("factId", factId);
        card.put("cardModelId", cardModelId);
        card.put("created", now);
        card.put("modified", now);
        card.put("tags", "");
        card.put("ordinal", 0);
        card.put("question", question);
        card.put("answer", answer);
        card.put("priority", 2);
        card.put("interval", 0.0);
        card.put("lastInterval", 0.0);
        card.put("due", 0);
        card.put("lastDue", 0);
        card.put("factor", 2.5);
        card.put("lastFactor", 2.5);
        card.put("firstAnswered", 0);
        card.put("reps", 0);
        card.put("successive", 0);
        card.put("averageTime", 0);
        card.put("reviewTime", 0);
        card.put("youngEase0", 0);
        card.put("youngEase1", 0);
        card.put("youngEase2", 0);
        card.put("youngEase3", 0);
        card.put("youngEase4", 0);
        card.put("matureEase0", 0);
        card.put("matureEase1", 0);
        card.put("matureEase2", 0);
        card.put("matureEase3", 0);
        card.put("matureEase4", 0);
        card.put("yesCount", 0);
        card.put("noCount", 0);
        card.put("spaceUntil", 0);
        card.put("relativeDelay", 0);

        card.put("isDue", true);
        card.put("type", 2);
        card.put("combinedDue", now);
        db.insert("cards", null, card);

        return cardId;
    }

    private void insertField(SQLiteDatabase db, String value, long factId,
            long fieldModelId, int ordinal) {
        ContentValues field = new ContentValues();
        field.put("id", generateId());
        field.put("factId", factId);
        field.put("fieldModelId", fieldModelId);
        field.put("ordinal", ordinal);
        field.put("value", value);
        db.insert("fields", null, field);
    }

    private String generateKanjiAnswer(KanjiEntry k) {
        StringBuffer buff = new StringBuffer();
        if (k.getOnyomi() != null) {
            buff.append(String.format(QA_TEMPLATE, k.getOnyomi()));
        }
        if (k.getKunyomi() != null) {
            buff.append(String.format(QA_TEMPLATE, k.getKunyomi()));
        }
        if (k.getNanori() != null) {
            buff.append(String.format(QA_TEMPLATE, k.getNanori()));
        }
        if (k.getMeaningsAsString() != null) {
            buff.append(String.format(QA_TEMPLATE, k.getMeaningsAsString()));
        }

        return buff.toString();
    }

    private String generateDictAnswer(DictionaryEntry d) {
        StringBuffer buff = new StringBuffer();
        if (d.getReading() != null) {
            buff.append(String.format(QA_TEMPLATE, d.getReading()));
        }
        if (d.getMeaningsAsString() != null) {
            buff.append(String.format(QA_TEMPLATE, d.getMeaningsAsString()));
        }

        return buff.toString();
    }

    private String generateQuestion(String question) {
        return String.format(QA_TEMPLATE, question);
    }

    private long generateId() {
        long time = System.currentTimeMillis();
        int rand = random.nextInt(2 ^ 23);
        long id = rand << 41 | time;

        return id;
    }

    private String readSchema(String name) {
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
}
