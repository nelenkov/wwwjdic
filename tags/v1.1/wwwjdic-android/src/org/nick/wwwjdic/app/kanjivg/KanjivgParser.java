package org.nick.wwwjdic.app.kanjivg;

import java.io.InputStream;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

public class KanjivgParser {

    private InputStream in;
    private XMLStreamReader reader;
    private int eventType;

    public KanjivgParser(InputStream in) {
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

    public void fastForward(int idx) {
        eventType = reader.getEventType();
        boolean done = false;

        int currentIdx = 0;
        String name;

        try {
            while (eventType != XMLStreamConstants.END_DOCUMENT && !done) {
                switch (eventType) {
                case XMLStreamConstants.START_DOCUMENT:
                    break;
                case XMLStreamConstants.START_ELEMENT:
                    name = reader.getName().getLocalPart();
                    if (name.equalsIgnoreCase("kanji")) {
                        if (currentIdx == idx) {
                            return;
                        }
                        String midashi = reader.getAttributeValue(null,
                                "midashi");
                        System.out.println(currentIdx + ": " + midashi);

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
        try {
            if (!reader.hasNext()) {
                return null;
            }

            Kanji result = null;
            String name = null;
            int strokeNumber = -1;
            eventType = reader.getEventType();
            boolean done = false;

            while (eventType != XMLStreamConstants.END_DOCUMENT && !done) {
                switch (eventType) {
                case XMLStreamConstants.START_DOCUMENT:
                    break;
                case XMLStreamConstants.START_ELEMENT:
                    name = reader.getName().getLocalPart();
                    if (name.equalsIgnoreCase("kanji")) {
                        String midashi = reader.getAttributeValue(null,
                                "midashi");
                        String unicodeNumber = reader.getAttributeValue(null,
                                "id");
                        result = new Kanji(midashi, unicodeNumber);
                        strokeNumber = 1;
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
