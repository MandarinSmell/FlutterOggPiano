@file:Suppress("DEPRECATION")

package com.mandarin.flutter_ogg_piano

import android.media.MediaCodec
import android.os.Build
import android.util.Log
import androidx.annotation.NonNull
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import io.flutter.plugin.common.PluginRegistry.Registrar
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.pow

/** FlutterOggPianoPlugin */
class FlutterOggPianoPlugin : FlutterPlugin, MethodCallHandler {

    private lateinit var channel: MethodChannel

    private val ids = HashMap<Int, Int>()
    private var released = false

    init {
        System.loadLibrary("oggPiano")
    }

    private external fun addPlayer(data: FloatArray, index: Int, forceLoad: Boolean, isStereo: Boolean, sampleRate: Int) : Int
    private external fun initializeEngine(isStereo: Boolean, mode: Int)
    private external fun addQueue(id: Int, pan: Float, pitch: Float, playScale: Float)
    private external fun release()

    override fun onAttachedToEngine(@NonNull flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
        channel = MethodChannel(flutterPluginBinding.binaryMessenger, "flutter_ogg_piano")
        channel.setMethodCallHandler(this)
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
                released = false

                val isStereo = call.argument<Boolean>("isStereo") ?: true
                val mode = call.argument<Int>("mode") ?: 0

                Log.i("FOP", "Initializing engine with ${if(mode == 0) "LOW_LATENCY" else "POWER_SAVING"} mode")

                initializeEngine(isStereo, mode)

                result.success("Succeeded to initialize OggEngine")
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

                val reader = AudioReader(path)

                if(reader.canDo) {
                    val resArray = reader.getPCM(MediaCodec.BufferInfo()).toByteArray()

                    val id = addPlayer(toFloatArray(resArray), index, force, reader.isStereo, reader.sampleRate)

                    ids[index] = id

                    result.success("Succeeded to initialize sound, index is $index and id is $id")
                } else {
                    result.error("FOP_SOUND_INIT_FAILURE", "Failed to initialize sound", null)
                }
            }
            "play" -> {
                if(released) {
                    print("Warning : OggEngine is already released! Re-initializing is required")
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

                var pan = call.argument<Double>("pan")

                if(pan == null) {
                    result.error("FOP_PLAY_ERROR","pan isn't specified", "")
                    return
                } else if(pan < -1.0 || pan > 1.0) {
                    Log.w("FOP_PLAY_WARNING", "pan is out of range! Readjusting pan")

                    if(pan < -1.0)
                        pan = -1.0

                    if(pan > 1.0)
                        pan = 1.0
                }

                val rate = 1.0f * 2.0.pow((1.0 * note.toFloat()) / 12.0).toFloat()

                val id = ids[index]

                if (id == null) {
                    println("Warning : No such $index index found")
                    result.error("FOP_PLAY_ERROR", "No such $index index found", "")
                    return
                }

                addQueue(id, pan.toFloat(), rate, 1F)

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

                        var pan = note[1]

                        if(pan < -1.0 || pan > 1.0) {
                            Log.w("FOP_PLAY_GROUP_WARNING", "pan is out of range! Readjusting pan")

                            if(pan < -1.0)
                                pan = -1.0

                            if(pan > 1.0)
                                pan = 1.0
                        }

                        addQueue(id, pan.toFloat(), rate, note[2].toFloat())
                    }
                }

                result.success("Succeeded to play multiple sounds")
            }
            "release" -> {
                release()
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

    private fun toFloatArray(bytes: ByteArray) : FloatArray {
        val shorts = ShortArray(bytes.size / 2)

        ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(shorts)

        val result = FloatArray(shorts.size)

        for(i in shorts.indices) {
            result[i] = shorts[i] * 2.0f / 65535
        }

        return result
    }
}
