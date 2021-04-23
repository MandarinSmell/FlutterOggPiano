@file:Suppress("DEPRECATION")

package com.mandarin.flutter_ogg_piano

import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.os.Build

class AudioReader(path: String) {
    private lateinit var codec: MediaCodec
    private val extractor = MediaExtractor()
    var isStereo = false
    var sampleRate = -1

    var canDo = true
    private var alreadyRead = false

    private var outputIndex = MediaCodec.INFO_TRY_AGAIN_LATER

    init {
        extractor.setDataSource(path)

        for(i in 0 until extractor.trackCount) {
            val format = extractor.getTrackFormat(i)
            val mime = format.getString(MediaFormat.KEY_MIME) ?: ""
            isStereo = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT) != 1
            sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)

            if(mime.startsWith("audio/")) {
                try {
                    extractor.selectTrack(i)
                    codec = MediaCodec.createDecoderByType(mime)
                    codec.configure(format, null, null, 0)
                } catch (e: Exception) {
                    e.printStackTrace()
                    canDo = false
                }

                break
            }
        }

        if(this::codec.isInitialized) {
            codec.start()
        }
    }

    fun getPCM(info: MediaCodec.BufferInfo) : ArrayList<Byte> {
        val result = ArrayList<Byte>()

        if(!this::codec.isInitialized)
            throw IllegalStateException("MediaCodec isn't initialized")

        if(alreadyRead)
            throw IllegalStateException("This reader already finished its task")

        var endOfFile = false

        while(true) {
            if(!endOfFile) {
                val index = codec.dequeueInputBuffer(10000)

                if(index >= 0) {
                    val inputBuffer = if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        codec.getInputBuffer(index) ?: throw IllegalStateException("Can't get inputBuffer : $index")
                    } else {
                        codec.inputBuffers[index] ?: throw IllegalStateException("Can't get inputBuffer : $index")
                    }

                    val size = extractor.readSampleData(inputBuffer, 0)

                    if(size < 0) {
                        codec.queueInputBuffer(index, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                        endOfFile = true
                    } else {
                        codec.queueInputBuffer(index, 0, size, extractor.sampleTime, 0)
                        extractor.advance()
                    }
                }
            }

            outputIndex = codec.dequeueOutputBuffer(info, 10000)

            if(outputIndex >= 0) {
                val outputBuffer = if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    codec.getOutputBuffer(outputIndex) ?: throw IllegalStateException("Can't get outputBuffer : $outputIndex")
                } else {
                    codec.outputBuffers[outputIndex] ?: throw IllegalStateException("Can't get outputBuffer : $outputIndex")
                }

                val b = ByteArray(info.size - info.offset)

                val pos = outputBuffer.position()

                outputBuffer.get(b)
                outputBuffer.position(pos)

                for(i in b.indices) {
                    result.add(b[i])
                }

                codec.releaseOutputBuffer(outputIndex, true)
            }


            if((info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                codec.stop()
                codec.release()
                extractor.release()

                break
            }
        }

        return result
    }
}