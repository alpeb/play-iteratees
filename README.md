Tutorial for Play Framework Iteratees
=====================================

[![Build Status](https://api.travis-ci.org/alpeb/play-iteratees.png?branch=master)](https://travis-ci.org/alpeb/play-iteratees)

This is an [Activator](https://typesafe.com/activator) template showing how to use Play framework Iteratees to build a custom body parser in Scala, using as an example an MP3 file metadata parser.


How to Run
==========
First make sure you already have Typesafe's Activator installed. We recommend you install the mini-package so only the required dependencies are loaded when you launch the project.

Then, cd into the project and run
```
activator ui
```

and follow the tutorial.

What this is (Tutorial Excerpt)
================================
Throughout this tutorial we will explore how to use [Iteratees](http://www.playframework.com/documentation/2.3.x/api/scala/index.html#play.api.libs.iteratee.Iteratee), a nifty abstraction provided in the Play framework to handle streams reactively.

According to the [manual](http://www.playframework.com/documentation/2.3.x/Iteratees):
> Progressive Stream Processing and manipulation is an important task in modern Web Programming, starting from chunked upload/download to Live Data Streams consumption, creation, composition and publishing through different technologies including Comet and WebSockets.

Inside the Play framework, Iteratees are used among other things to build body parsers, which consume the raw incoming data and transform it appropriately so that it is delivered to an [Action](http://www.playframework.com/documentation/2.3.x/api/scala/index.html#play.api.mvc.Action), that receives it aptly wrapped in a [Request](http://www.playf  ramework.com/documentation/2.3.x/api/scala/index.html#play.api.mvc.Request). The Request's body will be typed according to the transformed data and can contain for example json, xml, multipart form data or a URL-encoded form.

You can build your own custom body parsers to pre-process incoming data before delivering it to an Action, and that's exactly what we're gonna do here.

License
=======
This software is issued under the [Apache 2](https://github.com/alpeb/play-iteratees/blob/master/LICENSE) license.
