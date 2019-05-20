# AI2_ANDROID_THINGSPEAK_CLIENT_EXTENSION

> Android client ThingSpeak extension for MIT Application Inventor 2   <img src="https://github.com/aluis-rcastro/AI2_ANDROID_THINGSPEAK_CLIENT_EXTENSION/blob/master/res/IconTS.png?raw=true" alt="" >

This extension is intended to easily make use of the Read and Write features of the ThingSpeak cloud service provided by MathWorks in such a manner that programmer is able to transparently make use of the service without having to deal with much details of the implementation in the code side. With the App - also attached - you can easily check the extension working on a friendly interface:
 
<img src="https://github.com/aluis-rcastro/AI2_ANDROID_THINGSPEAK_CLIENT_EXTENSION/blob/master/res/AppRunning.png" alt="" width="225" height="425">

For those who are not familiar with IoT stuffs, the key point to be aware is that the main goal achieved by using cloud servers is that we can design an architecture so that we don't need provide the server, which means that both sides of the system - the one being monitored, and the supervisory itself - now work as clients, as follow:

<img src="https://github.com/aluis-rcastro/AI2_ANDROID_THINGSPEAK_CLIENT_EXTENSION/blob/master/res/Architecture.png" alt="" width="425" height="225">

Essentially ThingSpeak is a Web server on which data is read and written via `GET` commands of the `HTTP` protocol. All replies come on the `JSON` format in raw text with no formatting, which means that no LF or CR characters are added, as can be seen on the left side of the following picture:

<img src="https://github.com/aluis-rcastro/AI2_ANDROID_THINGSPEAK_CLIENT_EXTENSION/blob/master/res/JSonFormatter.png" alt="" width="750" height="375">

The first step to do is subscribe to the [Thingspeak](https://pages.github.com/) website, create a channel, and take note of the following data automatically generated, namely `READKEY`, `WRITEKEY` and `CANNEL ID`, highlighted bellow:

<img src="https://github.com/aluis-rcastro/AI2_ANDROID_THINGSPEAK_CLIENT_EXTENSION/blob/master/res/ThingSpeak.png" alt="" width="225" height="325">

If you are not going to use multiple fields simultaneously, you can configure the channel to use only 1 of the 8 fields available, which would also shrink the size of the JSON structure. A single field is able to fit up to 255 characters, wich is quite enought to gather data from many peripheral devices.

<img src="https://github.com/aluis-rcastro/AI2_ANDROID_THINGSPEAK_CLIENT_EXTENSION/blob/master/res/Channel.png" alt="" width="625" height="300">

It can be seen on the browser that the JSon structure is significantly smaller:

<img src="https://github.com/aluis-rcastro/AI2_ANDROID_THINGSPEAK_CLIENT_EXTENSION/blob/master/res/Shrinked.png" alt="" width="1000" height="130">

It is important to note that all data sent and received are in the `ASCII` format at the URL body, which means that if one wish provide extra security, it should be considered any kind of cyphering or even cryptography in order to atleast detect whether data was maliciously corrupted or not. Another point to remark is that the parsing was performed without using any JSon library, which means that if on one hand the structural integrity of the JSON format is not checked out, on the other hand it means that tasks are performed lightweight, not wasting core processing, particularly useful if application require to update the screen in realtime.

> *Note: With the `Free` licensing option, upload to the server (write) cannot be made within an interval time smaller than **15s** otherwise client application will receive a negative response.*

Here's an overview of the "source code" or rather, "source blocs" of the above demo application.

That's all you need, nothing else:

<img src="https://github.com/aluis-rcastro/AI2_ANDROID_THINGSPEAK_CLIENT_EXTENSION/blob/master/res/AppProject.png" alt="" width="725" height="325">

Below, a snapshot of the components:

<img src="https://github.com/aluis-rcastro/AI2_ANDROID_THINGSPEAK_CLIENT_EXTENSION/blob/master/res/Components.png?raw=true" alt="" width="325" height="500">

Here, a snippet showing how much of the boring `Java` coding task can be avoided by customer, once it is embedded on the extension itself:

<img src="https://github.com/aluis-rcastro/AI2_ANDROID_THINGSPEAK_CLIENT_EXTENSION/blob/master/res/Snippet.png?raw=true" alt="" width="1200" height="450">

A brief description of each field on the given Demo program, you can refer to  [this](https://github.com/aluis-rcastro/AI2_ANDROID_THINGSPEAK_CLIENT_EXTENSION/blob/master/doc/usage.txt) document.

Feel free to make experiments and give feedback.

