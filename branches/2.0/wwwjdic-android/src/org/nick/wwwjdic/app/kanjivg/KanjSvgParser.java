package org.nick.wwwjdic.app.kanjivg;

import java.io.InputStream;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

public class KanjSvgParser {

    private static final String NS_KVG = "http://kanjivg.tagaini.net";

    private InputStream in;
    private XMLStreamReader reader;
    private int eventType;

    public KanjSvgParser(InputStream in) {
        try {
            this.in = in;
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
            boolean done = false;
            boolean inKanji = false;
            boolean inStrokes = false;

            while (eventType != XMLStreamConstants.END_DOCUMENT && !done) {
                switch (eventType) {
                case XMLStreamConstants.START_DOCUMENT:
                    break;
                case XMLStreamConstants.START_ELEMENT:
                    name = reader.getName().getLocalPart();
                    if (name.equalsIgnoreCase("g")) {
                        String id = reader.getAttributeValue(null, "id");
                        if ("Kanji".equals(id)) {
                            inKanji = true;
                            inStrokes = false;
                        }
                        if ("StrokePaths".equals(id)) {
                            inKanji = false;
                            inStrokes = true;
                        }
                    }

                    if (name.equals("text") && inKanji) {
                        String midashi = reader.getElementText();
                        char c = midashi.charAt(0);
                        String unicodeNumber = Integer.toHexString(c | 0x10000)
                                .substring(1);
                        result = new Kanji(midashi, unicodeNumber);
                        strokeNumber = 1;
                        inKanji = false;
                    }

                    if (name.equals("path") && inStrokes) {
                        String path = reader.getAttributeValue(null, "d");
                        String type = null;
                        Stroke stroke = new Stroke(strokeNumber, type, path);
                        result.getStrokes().add(stroke);
                        strokeNumber++;
                    }
                    break;
                case XMLStreamConstants.END_ELEMENT:
                    //                    name = reader.getName().getLocalPart();
                    //                    if (name.equalsIgnoreCase("kanji")) {
                    //                        eventType = reader.next();
                    //                        return result;
                    //                    }
                }
                eventType = reader.next();
            }

            return result;
        } catch (XMLStreamException e) {
            throw new RuntimeException(e);
        }
    }
}
