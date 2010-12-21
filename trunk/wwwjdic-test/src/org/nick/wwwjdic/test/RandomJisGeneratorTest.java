package org.nick.wwwjdic.test;

import org.junit.Test;
import org.nick.wwwjdic.widgets.RandomJisGenerator;

public class RandomJisGeneratorTest {

    @Test
    public void testGenerate() {
        RandomJisGenerator g = new RandomJisGenerator();
        String[] unicodeCps = new String[10];
        for (int i = 0; i < unicodeCps.length; i++) {
            unicodeCps[i] = g.generateAsUnicodeCp(true);
        }
        for (String cp : unicodeCps) {
            System.out.println(cp);
        }
    }
}
