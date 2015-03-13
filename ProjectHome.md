# WWWJDIC for Android #

Android frontend for Jim Breen's [WWWJDIC](http://www.csse.monash.edu.au/~jwb/cgi-bin/wwwjdic.cgi?1C)


# News #
  * 2014/6/12
    * v2.3.8 released. Use Kanji Recognizer for handwriting recognition.
  * 2014/3/21
    * v2.3.7 released. Scale stroke annotations.
  * 2014/3/18
    * v2.3.6 released. Bug fixes.
  * 2014/2/28
    * v2.3.5 released. Added new Germany mirror.
  * 2013/12/24
    * v2.3.4 released.  Added backup file selection dialog (4.4+).

# Handwriting Recognition #

If you are looking for handwriting recognition for Japanese/Chinese, try:
  * [Kanji Recognizer](https://sites.google.com/site/kanjirecognizer/)
  * [Hanzi Recognizer](https://market.android.com/details?id=org.nick.hanzirecognizer)

All versions after 1.3 integrate with Kanji Recognizer.

# Changelog #
  * v2.3.7
    * Scale stroke annotations
    * Handwriting recognition fixes for some devices
  * v2.3.6
    * Bug fixes
  * v2.3.5
    * Added new Germany mirror.
  * v2.3.4
    * Android 4.4 support
    * Added backup file selection dialog (4.4+)
  * v2.3.3
    * Use Japanese font on non-Japanese locales (4.2+)
    * Added action to share Anki deck
    * Bug fixes
  * v2.3.2
    * White text for transparent widget
    * Widget layout adjustments
    * Search UI improvements

The full changelog can be found [here](Changelog.md).

# Features #
  * Japanese text-to-speech for dictionary entry/kanji pronunciation, as well as example sentences
  * Text-to-speech for kanji and dictionary entries translation (English/German/French/Spanish)
  * [Multi-radical](Screenshots#Multi-radical_kanji_search.md) kanji search.
  * Kanji of the day [widget](Screenshots#Kanji_of_the_Day_Widget.md), can limit scope by JLPT level
  * Japanese sentence translation.
  * [example search](Screenshots#Example_Search/Breakdown.md) with approximately 150,000 examples (Japanese/English pairs)
  * example breakdown/translation
  * [history/favorites](Screenshots#History_and_Favorites.md)
    * CSV import/export
    * favorites export to Google Docs
    * favorites Anki Export
    * filtering by type (dictionary/kanji/examples)
  * [animated stroke order diagrams](Screenshots#Stroke_Order_Diagrams.md) for over 6000 kanji
  * [handwritten kanji recognition](Screenshots#Handwritten_Kanji_Recognition.md)
    * high-accuracy vector-based handwritten recogntion
    * OCR-based handwritten kanji recognition, does not require correct stroke order
  * [OCR](Screenshots#OCR.md)
  * multiple [dictionary lookup](Screenshots#Dictionary_Lookup.md) (General/Japanese Names/Computing/Life Sciences and more)
  * multiple languages supported (German/French/Russian/Swedish/Hungarian/Spanish/Dutch/Slovenian)
  * [romaji](http://en.wikipedia.org/wiki/Romaji) input
  * [kanji lookup](Screenshots#Kanji_Lookup.md) by reading, English meaning, radical number, stroke count and code (Unicode/JIS)
  * [radical table](Screenshots#Kanji_Lookup.md) showing radical variations

# Screenshots #

[Here](Screenshots.md)

# Requirements #

Should run on any device with 2.1 or higher. Older versions support 1.6 (see downloads).

Uses the WWWJDIC online service, requires Internet connection.

Dictionary search supports romaji lookup, but you need a Japanese
IME to use all features. Try [OpenWnn/Flick](http://www.cyrket.com/p/android/com.pm9.flickwnn/) if you don't have one.

# Download #

[![](https://developer.android.com/images/brand/en_generic_rgb_wo_45.png)](https://play.google.com/store/apps/details?id=org.nick.wwwjdic)


[Download](https://play.google.com/store/apps/details?id=org.nick.wwwjdic) from Google Play (Referecne category) or use the QR code below:

[Donate version](https://play.google.com/store/apps/details?id=org.nick.wwwjdic.donate) also available.

![http://wwwjdic.googlecode.com/svn/trunk/site/images/qrcode.png](http://wwwjdic.googlecode.com/svn/trunk/site/images/qrcode.png)

# Tip jar #

If you really like the app you can drop your bitcoins here:
```
 1DXhWFS9SL78GGyX7Luao9EuP5SxtDiPG1
```

# Contact #

Find us on <a href='https://plus.google.com/105457662805333954065'>Google+</a> or use email address in the Play Store app page (link above).

# Acknowledgements #

  * Thanks to Jim Breen for all his nihongo work and for adding raw output to WWWJDIC.
  * Thanks to the [WeOCR Project](http://weocr.ocrgrid.org/) for providing the OCR service.
  * Uses the kanji recognizer at [kanji.sljfaq.org](http://kanji.sljfaq.org/kanji16/draw-canvas.html)
  * Uses [KanjiVG](http://kanjivg.tagaini.net/) data for stroke order diagrams