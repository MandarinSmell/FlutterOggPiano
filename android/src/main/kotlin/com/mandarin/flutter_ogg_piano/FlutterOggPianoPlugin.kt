package com.mandarin.flutter_ogg_piano

import android.media.AudioManager
import android.media.SoundPool
import android.os.Build
import android.util.Log
import androidx.annotation.NonNull

import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import io.flutter.plugin.common.PluginRegistry.Registrar
import java.lang.StringBuilder
import kotlin.math.pow

/** FlutterOggPianoPlugin */
class FlutterOggPianoPlugin : FlutterPlugin, MethodCallHandler {
    private lateinit var channel: MethodChannel
    private lateinit var pool: SoundPool

    private val ids = HashMap<Int, Int>()
    private var released = false

    override fun onAttachedToEngine(@NonNull flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
        channel = MethodChannel(flutterPluginBinding.binaryMessenger, "flutter_ogg_piano")
        channel.setMethodCallHandler(this);
    }

    companion object {
        @JvmStatic
        fun registerWith(registrar: Registrar) {
            val channel = MethodChannel(registrar.messenger(), "flutter_ogg_piano")
            channel.setMethodCallHandler(FlutterOggPianoPlugin())
        }
    }

    override fun onMethodCall(@NonNull call: MethodCall, @NonNull result: Result) {
        when (call.method) {
            "getPlatformVersion" -> {
                result.success("Android ${Build.VERSION.RELEASE}")
            }
            "init" -> {
                val maximum = call.argument<Int>("maxSound") ?: 128

                pool = if (Build.VERSION.SDK_INT >= 21) {
                    SoundPool.Builder().setMaxStreams(maximum).build()
                } else {
                    SoundPool(100, AudioManager.STREAM_MUSIC, 0)
                }

                released = false

                result.success("Succeeded to initialize SoundPool")
            }
            "load" -> {
                if(released) {
                    print("Warning : SoundPool is already released! Re-initializing is required")
                    result.error("FOP_LOAD_ERROR", "SoundPool is already released! Re-initializing is required", "")

                    return
                }

                val path = call.argument<String>("path") ?: return
                val index = call.argument<Int>("index") ?: return
                val force = call.argument<Boolean>("forceLoad") ?: return

                if (ids[index] != null) {
                    if(!force) {
                        print("Warning : ID already exists for this index. Try to allow forceLoad?")
                        result.error("FOP_LOAD_ERROR", "ID already exists for this index. Try to allow forceLoad?", "")
                        return
                    }
                }

                val id = pool.load(path, 0)

                ids[index] = id

                result.success("Succeeded to initialize sound, index is $index and id is $id")
            }
            "play" -> {
                if(released) {
                    print("Warning : SoundPool is already released! Re-initializing is required")
                    result.error("FOP_PLAY_ERROR", "SoundPool is already released! Re-initializing is required", "")
                    return
                }

                val index = call.argument<Int>("index")

                if(index == null) {
                    result.error("FOP_PLAY_ERROR","Index isn't specified","")
                    return
                }

                val note = call.argument<Int>("note")

                if(note == null) {
                    result.error("FOP_PLAY_ERROR","Note isn't specified","")
                    return
                }

                var left = call.argument<Double>("left")

                if(left == null) {
                    result.error("FOP_PLAY_ERROR","Left volume isn't specified", "")
                    return
                } else if(left < 0 || left > 1.0) {
                    Log.w("FOP_PLAY_WARNING", "Left volume is out of range! Readjusting left volume")

                    if(left < 0)
                        left = 0.0

                    if(left > 1.0)
                        left = 1.0
                }

                var right = call.argument<Double>("right")

                if(right == null) {
                    result.error("FOP_PLAY_ERROR", "Right volume isn't specified", "")
                    return
                } else if(right < 0 || right > 1.0) {
                    Log.w("FOP_PLAY_WARNING", "right volume is out of range! Readjusting right volume")

                    if(right < 0)
                        right = 0.0

                    if(right > 1.0)
                        right = 1.0
                }

                val rate = 1.0f * 2.0.pow((1.0 * note.toFloat()) / 12.0).toFloat()

                val id = ids[index]

                if (id == null) {
                    println("Warning : No such $index index found")
                    result.error("FOP_PLAY_ERROR", "No such $index index found", "")
                    return
                }

                pool.play(id, left.toFloat(), right.toFloat(), 0, 0, rate)

                result.success("Succeeded to play index $index with pitch $note")
            }
            "playInGroup" -> {
                val data = call.argument<Map<Int, List<DoubleArray>>>("data")

                if(data == null) {
                    result.error("FOP_PLAY_GROUP_ERROR", "Data isn't specified", "")
                    return
                }

                for(n in data.values) {
                    for(s in n) {
                        if(s.size != 3) {
                            result.error("FOP_PLAY_GROUP_ERROR", "Data has invalid format. Data : ${printMap(data)}", "")
                            return
                        }
                    }
                }

                for(n in data.keys) {
                    val sound = data[n] ?: continue

                    val id = ids[n]

                    if(id == null) {
                        Log.w("FOP_PLAY_GROUP_WARNING", "ID index $n isn't found, continuing playing sounds...")
                        continue
                    }

                    for(note in sound) {
                        val rate = 1.0f * 2.0.pow((1.0 * note[0].toFloat()) / 12.0).toFloat()

                        var right = note[2]

                        if(right < 0 || right > 1.0) {
                            Log.w("FOP_PLAY_WARNING", "right volume is out of range! Readjusting right volume")

                            if(right < 0)
                                right = 0.0

                            if(right > 1.0)
                                right = 1.0
                        }

                        var left= note[1]

                        if(left < 0 || left > 1.0) {
                            Log.w("FOP_PLAY_GROUP_WARNING", "Left volume is out of range! Readjusting left volume")

                            if(left < 0)
                                left = 0.0

                            if(left > 1.0)
                                left = 1.0
                        }

                        pool.play(id, left.toFloat(), right.toFloat(), 0, 0, rate)
                    }
                }

                result.success("Succeeded to play multiple sounds")
            }
            "release" -> {
                pool.release()
                result.success("Succeeded to release soundpool")
            }
            "isReleased" -> {
                result.success(released)
            }
            else -> {
                result.notImplemented()
            }
        }
    }

    override fun onDetachedFromEngine(@NonNull binding: FlutterPlugin.FlutterPluginBinding) {
        channel.setMethodCallHandler(null)
    }

    private fun printMap(map: Map<Int, List<DoubleArray>>) : String {
        val builder = StringBuilder("{")

        for(key in map.keys) {
            builder.append(" $key : ")

            val array = map[key]

            if(array == null){
                builder.append("null,")
            } else {
                builder.append("[")

                for(j in array.indices) {
                    val arr = array[j]

                    builder.append("(")

                    for(i in arr.indices) {
                        builder.append(arr[i])

                        if(i < arr.size - 1) {
                            builder.append(", ")
                        }
                    }

                    builder.append(")")

                    if(j < array.size - 1) {
                        builder.append(", ")
                    }
                }

                builder.append("],")
            }
        }

        builder.append("}")

        return builder.toString()
    }
}
