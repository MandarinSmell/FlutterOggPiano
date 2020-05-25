package com.mandarin.flutter_ogg_piano

import android.media.AudioManager
import android.media.SoundPool
import android.os.Build
import androidx.annotation.NonNull;

import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import io.flutter.plugin.common.PluginRegistry.Registrar
import kotlin.math.pow

/** FlutterOggPianoPlugin */
public class FlutterOggPianoPlugin : FlutterPlugin, MethodCallHandler {
    private lateinit var channel: MethodChannel
    private lateinit var pool: SoundPool

    private val ids = HashMap<Int, Int>()
    private var released = false

    override fun onAttachedToEngine(@NonNull flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
        channel = MethodChannel(flutterPluginBinding.getFlutterEngine().getDartExecutor(), "flutter_ogg_piano")
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
            }
            "load" -> {
                if(released) {
                    print("Warning : SoundPool is already released! Re-initializing is required")
                    return
                }

                val path = call.argument<String>("path") ?: return
                val index = call.argument<Int>("index") ?: return
                val force = call.argument<Boolean>("forceLoad") ?: return

                if (ids[index] != null) {
                    if(!force) {
                        print("Warning : ID already exists for this index. Try to allow forceLoad?")
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
                    return
                }

                val index = call.argument<Int>("index") ?: return
                val note = call.argument<Int>("note") ?: return

                val rate = 1.0f * 2.0.pow((1.0 * note.toFloat()) / 12.0).toFloat()

                val id = ids[index]

                if (id == null) {
                    println("Warning : No such $index index found")
                    return
                }

                pool.play(id, 1f, 1f, 0, 0, rate)
            }
            "release" -> {
                pool.release()
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
}
