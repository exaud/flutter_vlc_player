import 'dart:typed_data';

import 'package:flutter/material.dart';
import 'package:flutter_vlc_player/vlc_player.dart';
import 'package:flutter_vlc_player/vlc_player_controller.dart';

void main() => runApp(MyApp());

class MyApp extends StatefulWidget {
  @override
  _MyAppState createState() => new _MyAppState();
}

class _MyAppState extends State<MyApp> {
  Uint8List image;
  GlobalKey imageKey;
  VlcPlayer videoView;
  VlcPlayerController _videoViewController;

  @override
  void initState() {
    imageKey = new GlobalKey();
    _videoViewController = new VlcPlayerController();
    super.initState();
  }

  @override
  Widget build(BuildContext context) {
    return new MaterialApp(
      home: new Scaffold(
        appBar: new AppBar(
          title: const Text('Plugin example app'),
        ),
        body: Column(
          children: <Widget>[
            new VlcPlayer(
              defaultWidth: 640,
              defaultHeight: 360,
              url: "rtsp://192.168.1.3:8086",
              controller: _videoViewController,
              placeholder: Container(
                height: 250.0,
                child: Row(
                  mainAxisAlignment: MainAxisAlignment.center,
                  children: <Widget>[CircularProgressIndicator()],
                ),
              ),
            ),
            Expanded(
              child: image == null
                  ? Container()
                  : Container(
                decoration: BoxDecoration(image: DecorationImage(image: MemoryImage(image))),
              ),
            ),
          ],
        ),
      ),
    );
  }
}
