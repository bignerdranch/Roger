![Roger icon](https://github.com/bignerdranch/Roger/raw/master/Android/Roger/res/drawable/roger_icon.png)

Roger
=====

Roger is an Android visual resource viewer that dramatically shortens your
edit-compile-test loop. Run the server on your MacBook; run the client on one
or more devices. Every time you save a layout file or xml drawable, Roger will
show it to you on your devices. You can then futz around with it - rotate your
device, change the layout params on the container layout, fill its text views
with long strings, or see what it looks like as a row in a ListView.

Roger has a lot of rough edges right now, so it should be considered experimental.
We are using it in our daily development, though, so to the degree that we are 
irritated by it, it is under active development.

Features
--------

* Hands off resource visualization
* Multiple devices (haven't found an upper limit so far)
* ADB or WiFi connection
* View layouts and XML drawables
* Modify container layout params on the fly
* Support for SDK 9-15 (v2.3 - v4.0)


Getting Started
---------------

1. Install Android Roger on your devices or emulators, and install Mac Roger
on your Mac.
2. Run Mac Roger, open preferences, and select your Android SDK folder.

Then, for wifi:

3. Connect your devices to the same wifi network as your Mac.
4. Run Android Roger for it to connect. Tap Menu->Refresh if it 
   doesn't find your machine or if you ever need to reconnect.

For ADB:

3. Connect your devices or emulators to ADB and run Roger.

Now, work as you had before Roger entered your life. As you save edits to XML
files, Roger will show you what you are working on close to real time.

How Does It Work
----------------

Roger monitors your Mac for file changes. If a saved file is a layout or drawable XML
file living in an Android project, Roger then does a dependency analysis to build a stripped down
version of your project apk and sends it to all of your devices. Android Roger then opens up 
the apk, fishes your layout out, inflates it and displays it inside a container view.

As a result, the views you see in Roger are identical to what you see when you inflate them at runtime
in your own application. And because not every resource is included, Roger can also be much faster than building
and running your entire app.

Roger loads the apks it builds by reflecting on classes and doing inappropriate things with the runtime.
This is probably why it is not the most stable tool in the world at the moment.


Arbitrary Questions
-------------------

### Android versions?

Right now, the build target for the client is v2.1. Layouts do not actually
load on v2.1, though. Works against v2.2 onwards on the emulator. I can make no
guarantee about specific devices, apart from the devices I have personal experience
with (Nexus S w/ICS, Xoom w/3.2, emulators 2.2+).

### Emulators?

~~Not at the moment. Sorry.~~ Woop, this works now. Not over WiFi or other networks, though. Emulators
are supported exclusively over ADB.

### So Roger supports both ADB and WiFi. Which should I use?

ADB is more reliable right now, but WiFi is faster. I usually end up being lazy and letting the WiFi
connection lapse.

### Themes on pre-Honeycomb devices?

You might run into some problems there.

### Libraries?

Haven't tried it, but almost certainly no.

### Custom view subclasses?

No. Wondering if this is possible. Obviously not in the general case.

### Merge layouts?

Not right now. You can work around this by using a layout file like
this:

    <?xml version="1.0" encoding="utf-8"?>
    <FrameLayout xmlns:android="http://schemas.android.com/apk/res/android"
        android:layout_width="fill_parent"
        android:layout_height="fill_parent" >

        <include layout="@layout/merge_layout_i_want_to_test" />

    </FrameLayout>

### Any container layout besides FrameLayout?

Nope. Use the same workaround as above for this.

### How stable is it?

Ehh. It makes Android native code explode every now and then, causing
Roger to disappear without warning. Just restart the client if that 
happens.

### What will happen if git creates a bunch of new layout files?

Roger will display one of the new files that was created, but it should
handle it okay.

