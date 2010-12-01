package org.nick.kanjirecognizer.hkr;

interface CharacterRecognizer {
   
   void startRecognition(int width, int height);
   
   void addPoint(int strokeNum, int x, int y);
     
    String[] recognize(int numCandidates);
      
 }