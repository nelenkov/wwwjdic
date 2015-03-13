# Frequently Asked Questions (FAQ) #

## Q: Are all those permissions really needed? What are they for? ##

A:  Each permission is needed to implement one or more features. Even if you don't use all features, all permissions need to be set at install time, that's how Android works. See below for details.

  * android.permission.INTERNET
    * needed to access the WWWJDIC site, OCR and kanji recognizer services as well as Google Docs
  * android.permission.CAMERA
    * needed to take pictures for OCR
  * android.permission.WRITE\_EXTERNAL\_STORAGE
    * need to write stuff (temporary files, CSV/Anki exports, etc.) to the SD card
  * android.permission.GET\_ACCOUNTS (removed as of v2.0)
    * to list available Google accounts
  * android.permission.USE\_CREDENTIALS (removed as of v2.0)
    * to use an authentication token for Google Docs
  * android.permission.MANAGE\_ACCOUNTS (removed as of v2.0)
    * to mark the authentication token as expired
  * android.permission.ACCESS\_COARSE\_LOCATION
    * needed to automatically select mirror site based on current location

## Q: Why is the 'Google Docs' export menu disabled? ##

A: Authentication to Google Docs requires the AccountManager API, which is only available in post-Eclair devices (Android 2.0 and above). Without it you will need enter your Google account and password, which we don't want :)

Update: this feature was removed from version 2.0. It was not widely used and the needed Google account permission were confusing a lot of people.

## Q: The exported favorites CSV file doesn't work with Excel!? ##

A: CSV files exported with version 1.51 and above should open correctly with Excel 2007 when double-clicked. For earlier versions of Excel, you need to use the text import wizard with the following parameters:
  * File type: delimited
  * File origin (encoding): UTF-8
  * Delimiter: comma
  * Text qualifier: "
  * Column data format: text (for all columns)

Or, simply use OpenOffice, it lets you specify parameters at import time (and guesses most of them correctly, except for encoding).

## Q: Will there be an offline version? ##

A: Most probably not. Or it will be a completely different application. After all, this one has 'WWW' in its name :) If you need an offline kanji reference, try [Kanji Recognizer](https://play.google.com/store/apps/details?id=org.nick.kanjirecognizer).

## Q: Why can't I find the widget? ##

A: If you moved the app to the SD card, the widget cannot be used. Unfortunately that's how Android works. You are not alone, other people are complaining too:  see [Issue 8555](http://code.google.com/p/android/issues/detail?id=8555) :)

## Q: How to enable Japanese pronunciation (text-to-speech)? ##

A: Install and enable the [N2 TTS](https://market.android.com/details?id=jp.kddilabs.n2tts) Japanese text-to-speech engine (free). After installing, open Settings->Voice input & output->Text-to-speech settings and check 'KDDLABS N2 TTS'. You don't have to set it as the default engine (if you do, readout for other languages may be disabled).

Using two separate engines at the same time (one for Japanese and one for English, etc.) requires Android 2.2 (Froyo), so this feature is not available on 2.1 (Eclair).

Other supported Japanese TTS engines
  * [SVOX Japanese](https://play.google.com/store/apps/details?id=com.svox.classic.langpack.jpn_jpn_fem)
  * [AquesTalk TTS](https://play.google.com/store/apps/details?id=com.a_quest.aquestalka)
  * [DTalker Japanese TTS](https://play.google.com/store/apps/details?id=jp.co.createsystem)

## Q: Why do kanji look weird? ##

Most Android devices sold outside of Japan will use a font with Chinese glyphs to display Japanese characters. The only supported way to use a Japanese font for all applications is to set the language of the device to Japanese from Settings. If your device doesn't support Japanese and you have root access, you can change the default font setting as described [here](http://sindu.blogspot.jp/2012/12/fixing-japanese-font-rendering-in.html). Another option is to use a custom ROM like [CyanogenMod](http://www.cyanogenmod.org/) which supports Japanese.

WWWJDIC for Android 2.3.3 and later automatically uses a Japanese font (if available) on Android 4.2+. However, widget fonts cannot be changed by the app and the only way to get the 'Kanji of the Day' widget to use Japanese glyhps is to set the system language to Japanese (Android 4.2+).

If you are using a device sold in Japan with vendor firmware (ROM), it is most probably using a Japanese font by default.