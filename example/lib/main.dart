import 'dart:math';
import 'dart:typed_data';

import 'package:flutter/material.dart';
import 'dart:async';

import 'package:flutter/services.dart';
import 'package:flutter/widgets.dart';
import 'package:flutter_ogg_piano/flutter_ogg_piano.dart';

void main() {
  runApp(MyApp());
}

class MyApp extends StatefulWidget {
  @override
  _MyAppState createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  String _platformVersion = 'Unknown';
  FlutterOggPiano fop = FlutterOggPiano();

  List<String> files = ["piano.ogg", "piano2.ogg"];

  bool initialized = false;

  int _count = 1;
  int _pitch = 0;

  @override
  void initState() {
    super.initState();
    initPlatformState();

    if(!initialized) {
      loadPianoSounds();
    }
  }

  // Platform messages are asynchronous, so we initialize in an async method.
  Future<void> initPlatformState() async {
    String platformVersion;
    // Platform messages may fail, so we use a try/catch PlatformException.
    try {
      platformVersion = (await FlutterOggPiano.platformVersion) ?? _platformVersion;
    } on PlatformException {
      platformVersion = 'Failed to get platform version.';
    }

    // If the widget was removed from the tree while the asynchronous platform
    // message was in flight, we want to discard the reply rather than calling
    // setState to update our non-existent appearance.
    if (!mounted) return;

    setState(() {
      _platformVersion = platformVersion;
    });
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: Scaffold(
        appBar: AppBar(
          title: const Text('Plugin example app'),
        ),
        body: Center(
          child: Column(
            mainAxisAlignment: MainAxisAlignment.spaceEvenly,
            children: [
              Row(
                mainAxisAlignment: MainAxisAlignment.spaceEvenly,
                children: [
                  ElevatedButton(onPressed: () {
                      if(initialized) {
                        fop.play(index: 0, note: 0);
                      }
                    }, child: Text("Play Sound 1")
                  ),
                  ElevatedButton(onPressed: () {
                      if(initialized) {
                        fop.play(index: 1, note: 0);
                      }
                  }, child: Text("Play Sound 2"),)
                ],
              ),
              Row(
                mainAxisAlignment: MainAxisAlignment.spaceAround,
                children: [
                  Column(
                    mainAxisAlignment: MainAxisAlignment.spaceEvenly,
                    children: [
                      Center(child: Text("Pitch"),),
                      Row(
                        children: [
                          Ink(
                            decoration: ShapeDecoration(
                              color: Colors.lightBlue,
                              shape: CircleBorder(),
                            ),
                            child: IconButton(
                              splashRadius: 24,
                              splashColor: Colors.lightBlueAccent,
                              onPressed: () {
                                setState(() {
                                  _pitch++;
                                });
                              },
                              icon: Icon(Icons.arrow_upward, color: Colors.white70,),
                            ),
                          ),
                          Padding(padding: EdgeInsets.fromLTRB(6, 0, 6, 0)),
                          Container(
                            width: 72,
                            height: 36,
                            decoration: BoxDecoration(
                                borderRadius: BorderRadius.circular(8),
                                color: Colors.grey[100],
                                boxShadow: [
                                  BoxShadow(
                                      offset: Offset(4,4),
                                      blurRadius: 4,
                                      color: Colors.grey[600]!
                                  )
                                ]
                            ),
                            child: Center(
                              child: Text(_pitch.toString()),
                            ),
                          ),
                          Padding(padding: EdgeInsets.fromLTRB(6, 0, 6, 0)),
                          Ink(
                            decoration: ShapeDecoration(
                              color: Colors.lightBlue,
                              shape: CircleBorder(),
                            ),
                            child: IconButton(
                              splashRadius: 24,
                              splashColor: Colors.lightBlueAccent,
                              onPressed: () {
                                setState(() {
                                  _pitch--;
                                });
                              },
                              icon: Icon(Icons.arrow_downward, color: Colors.white70,),
                            ),
                          )
                        ],
                      )
                    ],
                  ),
                  Column(
                    mainAxisAlignment: MainAxisAlignment.spaceEvenly,
                    children: [
                      ElevatedButton(onPressed: () {
                        if(initialized) {
                          fop.play(index: 0, note: _pitch);
                        }
                      }, child: Text("Play Sound 1")),
                      ElevatedButton(onPressed: () {
                        fop.play(index: 1, note: _pitch);
                      }, child: Text("Play Sound 2")),
                    ],
                  )
                ],
              ),
              Row(
                mainAxisAlignment: MainAxisAlignment.spaceEvenly,
                children: [
                  Ink(
                    decoration: ShapeDecoration(
                      color: Colors.lightBlue,
                      shape: CircleBorder(),
                    ),
                    child: IconButton(
                      splashRadius: 24,
                      splashColor: Colors.lightBlueAccent,
                      onPressed: () {
                        setState(() {
                          _count++;
                        });
                      },
                      icon: Icon(Icons.arrow_upward, color: Colors.white70,),
                    ),
                  ),
                  Container(
                    width: 72,
                    height: 36,
                    decoration: BoxDecoration(
                      borderRadius: BorderRadius.circular(8),
                      color: Colors.grey[100],
                      boxShadow: [
                        BoxShadow(
                            offset: Offset(4,4),
                          blurRadius: 4,
                          color: Colors.grey[600]!
                        )
                      ]
                    ),
                    child: Center(
                      child: Text(_count.toString()),
                    ),
                  ),
                  Ink(
                    decoration: ShapeDecoration(
                      color: Colors.lightBlue,
                      shape: CircleBorder(),
                    ),
                    child: IconButton(
                      splashRadius: 24,
                      splashColor: Colors.lightBlueAccent,
                      onPressed: () {
                        setState(() {
                          _count = max(--_count, 1);
                        });
                      },
                      icon: Icon(Icons.arrow_downward, color: Colors.white70,),
                    ),
                  ),
                  Column(
                    mainAxisAlignment: MainAxisAlignment.spaceEvenly,
                    children: [
                      ElevatedButton(onPressed: () {
                        if(initialized) {
                          Map<int, List<Float64List>> maps = Map();

                          List<Float64List> sounds = [];

                          Float64List data = Float64List(3);

                          data[0] = 0;
                          data[1] = 0;
                          data[2] = _count.toDouble();

                          sounds.add(data);

                          maps[0] = sounds;

                          fop.playInGroup(maps);
                        }
                      }, child: Text("Play Sound 1 $_count time${_count == 1 ? "" : "s"}")),
                      ElevatedButton(onPressed: () {
                        if(initialized) {
                          Map<int, List<Float64List>> maps = Map();

                          List<Float64List> sounds = [];

                          Float64List data = Float64List(3);

                          data[0] = 0;
                          data[1] = 0;
                          data[2] = _count.toDouble();

                          sounds.add(data);

                          maps[1] = sounds;

                          fop.playInGroup(maps);
                        }
                      }, child: Text("Play Sound 2 $_count time${_count == 1 ? "" : "s"}")),
                      ElevatedButton(onPressed: () {
                        if(initialized) {
                          Map<int, List<Float64List>> maps = Map();

                          List<Float64List> sounds = [];

                          Float64List data = Float64List(3);

                          data[0] = 0;
                          data[1] = 0;
                          data[2] = _count.toDouble();

                          sounds.add(data);

                          maps[1] = sounds;
                          maps[0] = sounds;

                          fop.playInGroup(maps);
                        }
                      }, child: Text("Play Both $_count time${_count == 1 ? "" : "s"}"))
                    ],
                  )
                ],
              ),
              Row(
                mainAxisAlignment: MainAxisAlignment.spaceEvenly,
                children: [
                  ElevatedButton(onPressed: () {
                    if(initialized) {
                      fop.play(index: 0, note: 0, pan: -1.0);
                    }
                  }, child: Text("Play Sound 1 in left")),
                  ElevatedButton(onPressed: () {
                    if(initialized) {
                      fop.play(index: 0, note: 0, pan: 1.0);
                    }
                  }, child: Text("Play Sound 1 in right"))
                ],
              )
            ],
          ),
        ),
      ),
    );
  }

  Future<void> loadPianoSounds() async {
    fop.init();

    for(int i = 0; i < files.length; i++) {
      String name = "assets/"+files[i];

      ByteData data = await rootBundle.load(name);

      await fop.load(src: data, name: files[i], index: i, forceLoad: true);
    }

    initialized = true;
  }
}
