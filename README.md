# flutter_ogg_piano

Put sound and adjust pitching with Android internal codes in Flutter

## Getting Started

### Implementation

To implement this plugin in Flutter, add this line in dependencies section in pubspec.yaml

```yaml
flutter_ogg_piano : ^1.0.4
```

### Example

Before you using this FlutterOggPiano class, you always have to call init method first.
There is maximum number of sounds which can be played for Android, default is 128 and you can change this value
Be aware that this plugin can't hold sound longer than 5 seconds duration.

```dart
FlutterOggPiano fop = FlutterOggPiano();

//In somewhere of codes...
fop.init();
//With maximum number of sounds
fop.init(max: 64);
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
