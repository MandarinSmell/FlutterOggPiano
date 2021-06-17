# flutter_ogg_piano

Put sound and adjust pitching with Android internal codes in Flutter

## Getting Started

### Implementation

To implement this plugin in Flutter, add this line in dependencies section in pubspec.yaml

```yaml
flutter_ogg_piano : ^1.1.3
```

### Example

Before you using this FlutterOggPiano class, you always have to call init method first.
There is maximum number of sounds which can be played for Android, default is 128 and you can change this value
Be aware that this plugin can't hold sound longer than 5 seconds duration.

Since FlutterOggPiano 1.1.0, this plugin uses [Oboe Library](https://github.com/google/oboe) in Android,
you can set performance mode : LOW_LATENCY, POWER_SAVING.

LOW_LATENCY mode can perform the least latency when rendering audio.
However, be aware that playing too much sounds at the same time will make rendering process not able to be done properly, leading to bad sound experience.
POWER_SAVING on the other hand, requires some higher performance than LOW_LATENCY mode, but it can handle more amount of sounds simultaneously.
This plugin uses LOW_LATENCY mode as default for better audio rendering time, but you can change mode by calling init() method.

You can also decide whether audio will be rendered with stereo mode or not. In init() method, if *isStereo* argument is false, it will render all audio as mono.
This will affect device's speaker as well. If you turn on mono mode, even though speaker has 2 channels, it will play any sounds as if speaker has only one channel.

If you can be aware of memory usage, you can allow users to load any sounds unlike [SoundPool](https://developer.android.com/reference/android/media/SoundPool) in android.

```dart
FlutterOggPiano fop = FlutterOggPiano();

//In somewhere of codes...
fop.init();
//Decide performance mode
fop.init(mode: MODE.POWER_SAVING);
//If you want to play sound with mono
fop.ini(isStereo: false);
```

This example shows how you use FlutterOggPiano with sounds file saved in assets folder.
In pubspec.yaml file...

```yaml
assets:
  - assets/123.ogg
  - assets/456.ogg
```

When you load file, app will generate temporary sound file into device and won't be removed for usage later.
To replace this file, you have to set replace parameter to true, or it will load old data.
You can load sound file like this,

```dart
//In somewhere of codes
rootBundle.load("assets/123.ogg").then((ogg) {
  //If you want to overload already existing sounds...
  fop.load(src: ogg, name: "123.ogg", index: 1, forceLoad: true);
  //If you want to load it normally...
  fop.load(src: ogg, name: "123.ogg", index: 1);
  //If you want to replace generated temp file...
  fop.load(src: ogg, name: "123.ogg", index: 1, forceLoad: true, replace: true);
});

rootBundle.load("assets/456.ogg").then((ogg) {
  fop.load(src: ogg, index: 0);
});

//Keep going for more sounds
```

When you make it play sound, you have to put note value.
1 value difference is 1 semitone difference. 
Negative value for lower pitch and positive value for higher pitch.
Zero for same pitch with source sound.

```dart
//Somewhere of codes...
fop.play(index: 1, note: -1) // 1 lower semitone with 123.ogg sound
fop.play(index: 0, note: 3) //3 higher semitones with 456.ogg sound
```

Since version 1.0.5, playing supports separated left/right volume
If left/right isn't specified, default value is 1.0

```dart
//Somewhere of codes...
fop.play(index: 1, note: -1, left: 0.5, right: 0.75)
```

Since version 1.1.0, we don't use left/right volume value. Instead, we use pan value only.
pan value can be decimal, but it must be ranged from -1.0 to 1.0.
-1.0 means panned to left, and 1.0 means panned to right. 0.0 means panned to center.
If pan value isn't specified, value is 0.0 as default

```dart
//Somewhere of codes...
fop.play(index: 1, note: 3, pan: 1.0);
```

Since version 1.0.6, you can now send multiple sound data which will be played
But this has some restriction, you have to pass Map<int, double[]>
Each key will be ID, and double[] contains [pitch, left_volume, right_volume]

```dart
Map<int, List<Float64List>> map = Map();

List<Float64List> sounds = [];

for(int i = 0; i < _number; i++) {
  Float64List list = Float64List(3);

  list[0] = _pitch;
  list[1] = _left;
  list[2] = _right;
  
  sounds.add(list);
}

map[id] = sounds;

fop.playInGroup(map);
```

Since version 1.1.0, playInGroup() method's parameter got changed a little bit.
Due to removal of left/right volume value, double[] must contain pan value.
And since Oboe can perform small latency for playing sounds, developers can play same sound multiple times in one session.
So double[] will require 3 data still, but now, it will be [pitch, pan, scale]

Be aware that if scale value is too large, users will get awful broken sound due to clipping, so adjust this value properly.
pan value has to be ranged from -1.0 to 1.0 still.

```dart
Map<int, List<Float64List>> map = Map();

List<Float64List> sounds = [];

for(int i = 0; i < _number; i++) {
  Float64List list = Float64List(3);

  list[0] = _pitch;
  list[1] = _pan;
  list[2] = _scale;
  
  sounds.add(list);
}

map[id] = sounds;

fop.playInGroup(map);
```

Don't forget to release it after using this class

```dart
fop.release();
```

It will automatically prevent playing/loading sound with released state,
but you can simply check if it's released by calling this

```dart
fop.isReleased().then((r) {
  //Do something
})
```
