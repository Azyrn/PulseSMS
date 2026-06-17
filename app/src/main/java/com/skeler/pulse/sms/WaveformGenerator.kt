package com.skeler.pulse.sms

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.abs
import kotlin.math.min

object WaveformGenerator {

    private const val MAX_CACHE = 200
    private val cache = ConcurrentHashMap<String, List<Float>>()

    suspend fun generate(
        context: Context,
        uri: Uri,
        targetSamples: Int = 60,
    ): List<Float>? = withContext(Dispatchers.IO) {
        val key = uri.toString()
        cache[key]?.let { return@withContext it }

        if (cache.size >= MAX_CACHE) cache.clear()

        val pcm = decodeToPcm(context, uri) ?: return@withContext null
        val envelope = amplitudeEnvelope(pcm, targetSamples)
        cache[key] = envelope
        envelope
    }

    private fun decodeToPcm(context: Context, uri: Uri): ShortArray? {
        val extractor = MediaExtractor()
        try {
            extractor.setDataSource(context, uri, null)
        } catch (e: Exception) {
            return null
        }

        var audioTrackIndex = -1
        var audioFormat: MediaFormat? = null
        for (i in 0 until extractor.trackCount) {
            val format = extractor.getTrackFormat(i)
            if (format.getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true) {
                audioTrackIndex = i
                audioFormat = format
                break
            }
        }
        if (audioTrackIndex == -1) {
            extractor.release()
            return null
        }

        extractor.selectTrack(audioTrackIndex)
        val mime = audioFormat!!.getString(MediaFormat.KEY_MIME)!!
        val decoder = MediaCodec.createDecoderByType(mime)
        decoder.configure(audioFormat, null, null, 0)
        decoder.start()

        val allPcm = mutableListOf<Short>()
        val bufferInfo = MediaCodec.BufferInfo()
        var inputDone = false
        var outputDone = false
        val timeoutUs = 10000L

        while (!outputDone) {
            if (!inputDone) {
                val inputIndex = decoder.dequeueInputBuffer(timeoutUs)
                if (inputIndex >= 0) {
                    val inputBuffer = decoder.getInputBuffer(inputIndex) ?: continue
                    val sampleSize = extractor.readSampleData(inputBuffer, 0)
                    if (sampleSize < 0) {
                        decoder.queueInputBuffer(
                            inputIndex, 0, 0, 0,
                            MediaCodec.BUFFER_FLAG_END_OF_STREAM,
                        )
                        inputDone = true
                    } else {
                        decoder.queueInputBuffer(
                            inputIndex, 0, sampleSize,
                            extractor.sampleTime, 0,
                        )
                        extractor.advance()
                    }
                }
            }

            val outputIndex = decoder.dequeueOutputBuffer(bufferInfo, timeoutUs)
            when {
                outputIndex >= 0 -> {
                    if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                        outputDone = true
                    }
                    if (bufferInfo.size > 0) {
                        decoder.getOutputBuffer(outputIndex)?.let { buf ->
                            buf.position(bufferInfo.offset)
                            buf.limit(bufferInfo.offset + bufferInfo.size)
                            val shortBuf = buf.asShortBuffer()
                            val shorts = ShortArray(shortBuf.remaining())
                            shortBuf.get(shorts)
                            allPcm.addAll(shorts.toList())
                        }
                    }
                    decoder.releaseOutputBuffer(outputIndex, false)
                }
                outputIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> { /* continue */ }
            }
        }

        decoder.stop()
        decoder.release()
        extractor.release()

        return if (allPcm.isNotEmpty()) allPcm.toShortArray() else null
    }

    private fun amplitudeEnvelope(samples: ShortArray, targetSamples: Int): List<Float> {
        if (samples.isEmpty()) return emptyList()

        val result = mutableListOf<Float>()
        val chunkSize = maxOf(1, samples.size / targetSamples)

        var offset = 0
        while (offset < samples.size && result.size < targetSamples) {
            var maxAmp = 0f
            val end = min(offset + chunkSize, samples.size)
            for (j in offset until end) {
                val amp = abs(samples[j].toFloat()) / 32768f
                if (amp > maxAmp) maxAmp = amp
            }
            result.add(maxAmp)
            offset = end
        }

        while (result.size < targetSamples) result.add(0f)

        return result
    }
}
