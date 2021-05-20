//
// Created by user on 4/1/2021.
//

#include "OggPianoEngine.h"
#include <oboe/Oboe.h>
#include <utility>
#include <vector>
#include <android/log.h>
#include "OggPlayer.h"

void OggPianoEngine::initialize() {
    std::vector<OggPlayer>().swap(players);
}

void OggPianoEngine::start(bool isStereo, MODE mode) {
    AudioStreamBuilder builder;

    builder.setFormat(AudioFormat::Float);
    builder.setDirection(Direction::Output);
    builder.setChannelCount(isStereo ? ChannelCount::Stereo : ChannelCount::Mono);
    builder.setPerformanceMode(mode == LOW_LATENCY ? PerformanceMode::LowLatency : PerformanceMode::PowerSaving);
    builder.setSharingMode(SharingMode::Shared);

    builder.setCallback(this);

    builder.openStream(stream);

    stream->setBufferSizeInFrames(stream->getFramesPerBurst() * 2);

    stream->requestStart();

    deviceSampleRate = stream->getSampleRate();

    isStreamOpened = true;
    isStreamStereo = isStereo;
    selectedMode = mode;
}

void OggPianoEngine::release() {
    stream->flush();
    stream->close();
    stream.reset();
    isStreamOpened = false;
    deviceSampleRate = -1;
}

DataCallbackResult
OggPianoEngine::onAudioReady(AudioStream *audioStream, void *audioData, int32_t numFrames) {
    for(int i = 0; i < players.size(); i++) {
        players.at(i).renderAudio(static_cast<float*>(audioData), numFrames, i == 0, audioStream->getChannelCount() != 1);
    }

    return DataCallbackResult::Continue;
}

int OggPianoEngine::addPlayer(std::vector<float> data, int index, bool forceLoad, bool isStereo, int sampleRate) {
    if(index >= data.size()) {
        if(deviceSampleRate != -1) {
            players.emplace_back(std::move(data), isStereo, sampleRate, deviceSampleRate);
        } else {
            players.emplace_back(std::move(data), isStereo, sampleRate, sampleRate);
        }
    } else {
        if(forceLoad) {
            players.at(index).release();

            if(deviceSampleRate != -1) {
                players.at(index) = OggPlayer(std::move(data), isStereo, sampleRate, deviceSampleRate);
            } else {
                players.at(index) = OggPlayer(std::move(data), isStereo, sampleRate, sampleRate);
            }
        }
    }

    return (int) players.size() - 1;
}

void OggPianoEngine::addQueue(int id, float pan, float pitch, float playerScale) {
    if(id >= 0 && id < players.size()) {
        players.at(id).addQueue(pan, pitch, playerScale);
    }
}

void OggPianoEngine::onErrorAfterClose(AudioStream *audioStream, Result result) {
    if(result == oboe::Result::ErrorDisconnected) {
        reopenStream();
    }
}

void OggPianoEngine::closeStream() {
    if(isStreamOpened) {
        stream->stop();
        stream->flush();

        stream.reset();

        isStreamOpened = false;
        deviceSampleRate = -1;
    }
}

void OggPianoEngine::reopenStream() {
    if(isStreamOpened) {
        closeStream();
    }

    start(isStreamStereo, selectedMode);
}