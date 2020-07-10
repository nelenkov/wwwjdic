KanjiVG update
=

* Get latest KanjiVG XML file from https://github.com/KanjiVG/kanjivg
* Split to fit in blob field (gzipped):
```
xml_split -s 3MB kanjivg.xml
```
* fix start/end of XML file to use `<kanjivg>`
* gzip all files
```
for i in kanjivg-*.xml; do gzip $i;done
```
* Go to https://wwwjdic-android-hrd.appspot.com/update-strokes.xhtml and upload
* Use cron to kick `/update`
* Remove cron when done
```
./gradlew appengineDeployCron
```
