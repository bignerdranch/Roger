![Roger icon](https://github.com/bignerdranch/Roger/raw/master/Android/Roger/res/drawable/roger_icon.png)

Roger
=====

Roger is an Android layout file viewer. Run the server on your MacBook; run the client
on one or more devices. Every time you save your layout, Roger will show it to you on 
your devices. You can then futz around with the view - rotate, change the layout params
on its container layout, fill its text views with enormous strings, or see what it looks
like used as a row in a ListView.

How To Use It
-------------

1. Install Mac Roger on your Mac.
2. Install Android Roger on your phone or tablet.
3. Connect all devices to the same wireless network.
4. Run Mac Roger.
5. Run Android Roger. Wait for it to connect. Tap Menu->Refresh if it 
   doesn't find your machine or if you ever need to reconnect.

Now, work as you had before Roger entered your life. Roger will show you what you 
are working on in real time every time you save a layout file.

How Does It Work
----------------

Roger monitors your Mac for file changes. If a saved file is an xml file, lives in a res/layout*
folder, and lives in a tree with an AndroidManifest.xml, Roger then builds a stripped down
version of your project apk and sends it to all of your devices. Android Roger then opens up 
the apk, fishes your layout out, inflates it and displays it inside a container view.

As a result, the views you see in Roger are identical to what you see when you inflate them at runtime
in your own application. And because not every resource is included, Roger is also substantially faster than building
and running your app for a large project.

Limitations
-----------

### Android versions?

We don't know. The goal is "As many as possible." This will probably be hard, because Roger
relies on intimate knowledge of internal Android classes. We've tested it so far on Gingerbread, 
Honeycomb, and Ice Cream Sandwich.

### Emulators?

Not at the moment. Sorry.

### Themes on pre-Honeycomb devices?

You might run into some problems there.

### Libraries?

Haven't tried it, but almost certainly no.

### Custom view subclasses?

No.

### Merge layouts?

Not right now.

### Any container layout besides FrameLayout?

See above. This and merge layouts are pointless without each other anyway.

### Does it do anything at all?

Yes, you might still be excited about it if you hadn't read this far. I hope you're 
happy with your curious self. Go back and read the first paragraph again. But stop after that.