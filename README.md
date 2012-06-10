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
* Support for SDK 8-15 (Android v2.2 - v4.0)

Getting Started
---------------

0. Open mac/Roger/Roger.xcodeproj in Xcode and run.
1. In Roger, open preferences, and select your Android SDK folder.
2. Connect any number of devices or emulators. Select "yes" to install and start
    the Android Roger client.

Now, work as you had before Roger entered your life. As you save edits to XML
files, Roger will show you what you are working on close to real time.

Rationale
---------

We wrote this tool to scratch our own itch. We all edit XML by hand, deploying
to devices on a variety of versions of Android. Seeing the results of our work
was tiresome at best, and downright infuriating when the problem was version or
device specific. Roger solves these problems nicely, without getting in the way
of our existing workflow.

How Does It Work
----------------

Roger monitors your Mac for file changes. If a saved file is a layout or drawable XML
file living in an Android project, Roger then does a dependency analysis to build a stripped down
version of your project apk and sends it to all of your devices. Android Roger then opens up 
the apk, fishes your layout out, inflates it and displays it inside a container view.

As a result, the views you see in Roger are identical to what you see when you inflate them at runtime
in your own application. And because not every resource is included, Roger can also be much faster than building
and running your entire app.

Arbitrary Questions
-------------------

### Android versions?

Roger currently supports everything from v2.2 on up. No guarantee about
specific devices, apart from the devices we have personal experience with
(Nexus S w/ICS, Xoom w/3.2, emulators 2.2+). Roger does inappropriate things 
with reflection to do its things, so device-specific problems are not out of
the realm of possibility.

### Emulators?

Yes. Emulators do not support WiFi.

### So Roger supports both ADB and WiFi. Which should I use?

Roger will attempt to use both wherever it can. WiFi is usually faster, but if ADB 
loads quicker it will try to cancel the WiFi download.

### Themes?

There is currently no support for application or activity specific themes.
Layouts will load with the same theme as Roger (which is the default theme for 
targetSdkVersion=15).

### Libraries?

No.

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

### Roger seems to put my inflated layout inside a FrameLayout. Will Roger
### use a different layout than this?

Nope. Use the same workaround as above for this.
