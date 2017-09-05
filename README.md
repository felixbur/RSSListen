# RSSListen

RSSListen is an Android app I wrote privately to exercise my Android skills and to have an instrument to listen to (and test) different Text to speech engines under real world conditions. News articles are a great source to test pronunciation capabilities of text to speech engines.

It lets you listen to RSS Feeds via the Android TTS interface (meaning a TTS engine must be installed on he Android device, it's not part of the app).

You can search for keywords in the articles via the Android speech recognition interface.

The list of feeds that is used to gather articles can be edited on the device's external memory (SD-card I guess), it's a text file named "rssspeaker/rssUrls.txt".

The RSS feeds are loaded into the android database in the background each time the app is started, so if you don't use the app new articles will not be loaded. The loading process can also be started from the menu. 
