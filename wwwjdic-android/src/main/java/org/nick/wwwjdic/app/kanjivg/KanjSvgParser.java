package org.nick.wwwjdic.app.kanjivg;

import java.io.InputStream;
import java.util.logging.Logger;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

public class KanjSvgParser {

    private Logger log = Logger.getLogger(KanjSvgParser.class.getSimpleName());

    private static final String NS_KVG = "http://kanjivg.tagaini.net";

    private XMLStreamReader reader;
    private int eventType;

    public KanjSvgParser(InputStream in) {
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

    public Kanji parse() {

        Kanji result = null;

        try {
            if (!reader.hasNext()) {
                return null;
            }

            String name = null;
            int strokeNumber = -1;
            eventType = reader.getEventType();
            boolean inKanji = false;
            boolean inStrokes = false;

            while (eventType != XMLStreamConstants.END_DOCUMENT) {
                switch (eventType) {
                case XMLStreamConstants.START_DOCUMENT:
                    break;
                case XMLStreamConstants.START_ELEMENT:
                    name = reader.getName().getLocalPart();
                    if (name.equalsIgnoreCase("kanji")) {
                        String id = reader.getAttributeValue(null, "id");
                        String[] parts = id.split("_");
                        int unicode = Integer.parseInt(parts[1], 16);
                        String s = Character.toString((char)unicode);
                        log.info("kanji: " + s);
                        String unicodeNumber = Integer.toHexString(unicode | 0x10000)
                                .substring(1);
                        result = new Kanji(s, unicodeNumber);
                    }
                    if (name.equalsIgnoreCase("g")) {
                        String id = reader.getAttributeValue(null, "id");
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
                        }
                        if (id.contains("-g")) {
                            inKanji = false;
                            inStrokes = true;
                        }
                        String k = reader.getAttributeValue("kvg", "element");
                    }

                    if (name.equals("path") && inStrokes) {
                        String path = reader.getAttributeValue(null, "d");
                        log.info("path: " + path);
                        String type = null;
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

            return result;
        } catch (XMLStreamException e) {
            throw new RuntimeException(e);
        }
    }
}
