import 'dart:async';
import 'dart:io';
import 'dart:typed_data';

import 'package:flutter/services.dart';
import 'package:flutter_ogg_piano/tempFileExporter.dart';

class FlutterOggPiano {
  static const MethodChannel _channel =
      const MethodChannel('flutter_ogg_piano');

  static Future<String?> get platformVersion async {
    final String? version = await _channel.invokeMethod('getPlatformVersion');
    return version;
  }

  /// Load sound file with specified [src] byte data and specified file [name] with custom [index] value.
  /// Don't overload file for same index or it won't load it. Or set [forceLoad] to true.
  /// This method will generate temp file in device, so you can decide if you will replace file with [replace] bool.
  Future<String?> load({required ByteData src, required String name, required int index, bool forceLoad = false, bool replace = false}) async {
    File? f = await (writeToFile(src, name: name, replace: replace));

    if(f == null)
      return null;

    final String? result = await _channel.invokeMethod("load", {"path": f.path, "index" : index, "forceLoad" : forceLoad});

    print("Result : $result");

    return result;
  }

  /// Play sound with specified [index] to choose instrument and [note] to pick note<br>
  /// 1 value difference for [note] is semitone<br>
  /// Negative [note] value for lower sound, positive for higher sound
  /// You can apply panning effect by adjusting [pan], this parameter must range from -1.0 to 1.0<br>
  /// If [pan] is -1.0, users will be able to hear sound only from left side, and if [pan] is 1.0, they will be able to hear sound only from right side<br>
  /// Be aware that [pan] will be effective only when stereo mode is enabled
  Future<void> play({required int index, required int note, double pan = 0.0}) async {
    await _channel.invokeMethod("play", {"index" : index, "note" : note, "pan" : pan});
  }

  /// Initialize sound system<br>
  /// You can enable mono mode by setting [isStereo] as false. [isStereo] is true as default
  Future<void> init({bool isStereo = true}) async {
    await _channel.invokeMethod("init", {"isStereo" : isStereo});
  }

  /// Release sound system. After releasing it, you can't do anything with this sound system.
  /// You can reuse it by calling init method again
  Future<void> release() async {
    await _channel.invokeMethod("release");
  }

  /// Check if sound system is released
  Future<bool?> isReleased() async {
    bool? isr = await _channel.invokeMethod("isReleased");

    return isr;
  }

  /// Plays multiple sounds at the same time, this method is for reducing delays in each sounds
  /// [data] must contain 3 length integer list. Each value in list represents {note, pan, number of notes}
  /// For example, if 1 pitched note will be played 10 times in single session with panned to right, data must be {1, 1, 10}
  /// Keys in [data] must contain id of each sound.
  Future<void> playInGroup(Map<int, List<Float64List>> data) async {
    await _channel.invokeMethod("playInGroup", {"data" : data});
  }
}
