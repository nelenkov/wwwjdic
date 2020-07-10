package org.nick.wwwjdic.app.kanjivg;

import java.io.InputStream;
import java.util.logging.Logger;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

public class KanjivgParser {

    private Logger logger = Logger.getLogger("KanjivgParser");

    private XMLStreamReader reader;
    private int eventType;

    public KanjivgParser(InputStream in) {
        try {
            XMLInputFactory factory = XMLInputFactory.newInstance();
            reader = factory.createXMLStreamReader(in);
        } catch (XMLStreamException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean hasNext() {
        try {
            return reader.hasNext();
        } catch (XMLStreamException e) {
            throw new RuntimeException(e);
        }
    }

    public int countKanji() {
        int count = 0;
        while(hasNext()) {
            if (next() != null) {
                count++;
            }
        }

        return count;
    }

    public void fastForward(int idx) {
        logger.info("fastForward: " + idx);

        eventType = reader.getEventType();

        int currentIdx = 0;
        String name;

        try {
            while (eventType != XMLStreamConstants.END_DOCUMENT) {
                switch (eventType) {
                case XMLStreamConstants.START_DOCUMENT:
                    break;
                case XMLStreamConstants.START_ELEMENT:
                    name = reader.getName().getLocalPart();
                    if (name.equalsIgnoreCase("kanji")) {
                        if (currentIdx == idx) {
                            return;
                        }
                        String id = reader.getAttributeValue(null,"id");
                        String[] parts = id.split("_");
                        int unicode = Integer.parseInt(parts[1], 16);
                        String s = Character.toString((char)unicode);
                        logger.info(String.format("currentIdx: %d, unicode: %04x, kanji: %s", currentIdx, unicode, s));

                        currentIdx++;
                    }
                    break;
                case XMLStreamConstants.END_ELEMENT:
                    break;
                }
                eventType = reader.next();
            }
        } catch (XMLStreamException e) {
            throw new RuntimeException(e);
        }
    }

    public Kanji next() {
        logger.info("next");

        try {
            if (!reader.hasNext()) {
                return null;
            }

            Kanji result = null;
            String name = null;
            int strokeNumber = -1;
            eventType = reader.getEventType();
            boolean done = false;

            boolean inKanji = false;
            boolean inStrokes = false;

            while (eventType != XMLStreamConstants.END_DOCUMENT && !done) {
                switch (eventType) {
                case XMLStreamConstants.START_DOCUMENT:
                    break;
                case XMLStreamConstants.START_ELEMENT:
                    name = reader.getName().getLocalPart();
                    if (name.equalsIgnoreCase("kanji")) {
                        String id = reader.getAttributeValue(null,"id");
                        String[] parts = id.split("_");
                        int unicode = Integer.parseInt(parts[1], 16);
                        String s = Character.toString((char)unicode);
                        logger.info("character: " + s);
                        String unicodeNumber = Integer.toHexString(unicode | 0x10000)
                                .substring(1);
                        result = new Kanji(s, unicodeNumber);
                        strokeNumber = 1;
                    }
                    if (name.equalsIgnoreCase("g")) {
                        String id = reader.getAttributeValue(null, "id");
                        logger.info("id: " + id);
                        if (id.contains("kanji_")) {
                            inKanji = true;
                            inStrokes = false;
                            String kanji = reader.getAttributeValue(null, "element");
                            char c = kanji.charAt(0);
                            String unicodeNumber = Integer.toHexString(c | 0x10000)
                                    .substring(1);
                            result = new Kanji(kanji, unicodeNumber);
                            strokeNumber = 1;
                            inKanji = false;
                        } else {
                            inKanji = false;
                            inStrokes = true;
                        }
                        String k = reader.getAttributeValue("kvg", "element");
                    }

                    if (name.equals("path") && inStrokes) {
                        String path = reader.getAttributeValue(null, "d");
                        logger.info("path: " + path);
                        String type = null;
                        Stroke stroke = new Stroke(strokeNumber, type, path);
                        result.getStrokes().add(stroke);
                        strokeNumber++;
                    }

                    if (name.equalsIgnoreCase("stroke")) {
                        String path = reader.getAttributeValue(null, "path");
                        String type = reader.getAttributeValue(null, "type");
                        Stroke stroke = new Stroke(strokeNumber, type, path);
                        result.getStrokes().add(stroke);
                        strokeNumber++;
                    }
                    break;
                case XMLStreamConstants.END_ELEMENT:
                    name = reader.getName().getLocalPart();
                    if (name.equalsIgnoreCase("kanji")) {
                        eventType = reader.next();
                        return result;
                    }
                }
                eventType = reader.next();
            }

            return null;
        } catch (XMLStreamException e) {
            throw new RuntimeException(e);
        }
    }
}
