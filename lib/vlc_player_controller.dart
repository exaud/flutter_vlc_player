import 'dart:io';
import 'dart:typed_data';
import 'package:flutter/services.dart';

class VlcPlayerController {
  MethodChannel _channel;
  bool hasClients = false;

  initView(int id) {
    _channel = MethodChannel("flutter_video_plugin/getVideoView_$id");
    hasClients = true;
  }

  Future<String> setStreamUrl(String url, int defaultHeight, int defaultWidth) async {
    var result = await _channel.invokeMethod("playVideo", {
      'url': url,
    });
    return result['aspectRatio'];
  }

  void dispose() {
    if (Platform.isIOS){
      _channel.invokeMethod("dispose");
    }
  }

}
