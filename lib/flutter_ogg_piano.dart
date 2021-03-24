import 'dart:async';
import 'dart:io';
import 'dart:typed_data';

import 'package:flutter/cupertino.dart';
import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';

import 'package:flutter/services.dart';
import 'package:flutter_ogg_piano/tempFileExporter.dart';

class FlutterOggPiano {
  static const MethodChannel _channel =
      const MethodChannel('flutter_ogg_piano');

  static Future<String> get platformVersion async {
    final String version = await _channel.invokeMethod('getPlatformVersion');
    return version;
  }

  /// Load sound file with specified [src] byte data and specified file [name] with custom [index] value.
  /// Don't overload file for same index or it won't load it. Or set [forceLoad] to true.
  /// This method will generate temp file in device, so you can decide if you will replace file with [replace] bool.
  Future<String> load({@required ByteData src, @required String name, @required int index, bool forceLoad = false, bool replace = false}) async {
    File f = await writeToFile(src, name: name, replace: replace);

    final String result = await _channel.invokeMethod("load", {"path": f.path, "index" : index, "forceLoad" : forceLoad});

    print("Result : $result");

    return result;
  }

  /// Play sound with specified [index] to choose instrument and [note] to pick note<br>
  /// 1 value difference for [note] is semitone<br>
  /// Negative [note] value for lower sound, positive for higher sound
  Future<void> play({@required int index, @required int note, double left = 1.0, double right = 1.0}) async {
    await _channel.invokeMethod("play", {"index" : index, "note" : note, "left" : left, "right" : right});
  }

  /// Initialize sound system with [max] number of sounds which can be played.
  /// Default maximum number of sounds is 128.
  Future<void> init({int max = 128}) async {
    await _channel.invokeMethod("init", {"max" : max});
  }

  /// Release sound system. After releasing it, you can't do anything with this sound system.
  /// You can reuse it by calling init method again
  Future<void> release() async {
    await _channel.invokeMethod("release");
  }

  /// Check if sound system is released
  Future<bool> isReleased() async {
    bool isr = await _channel.invokeMethod("isReleased");

    return isr;
  }

  /// Plays multiple sounds at the same time, this method is for reducing delays in each sounds
  /// [data] must contain 3 length integer list. Each value in list represents {note, left_volume, right_volume}
  /// Keys in [data] must contain id of each sound.
  Future<void> playInGroup(Map<int, List<Float64List>> data) async {
    await _channel.invokeMethod("playInGroup", {"data" : data});
  }
}
