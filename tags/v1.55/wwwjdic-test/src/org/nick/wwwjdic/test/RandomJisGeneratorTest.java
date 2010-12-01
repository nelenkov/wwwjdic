package org.nick.wwwjdic.test;

import org.junit.Test;
import org.nick.wwwjdic.widgets.RandomJisGenerator;

public class RandomJisGeneratorTest {

    @Test
    public void testGenerate() {
        RandomJisGenerator g = new RandomJisGenerator();
        String[] jisCodes = new String[10];
        for (int i = 0; i < jisCodes.length; i++) {
            jisCodes[i] = g.generate();
        }
        for (String jis : jisCodes) {
            System.out.println(jis);
        }
    }
}
