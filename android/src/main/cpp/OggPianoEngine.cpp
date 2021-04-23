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
    players = new std::vector<OggPlayer>;
}

void OggPianoEngine::start(bool isStereo) {
    AudioStreamBuilder builder;

    builder.setFormat(AudioFormat::Float);
    builder.setDirection(Direction::Output);
    builder.setChannelCount(isStereo ? ChannelCount::Stereo : ChannelCount::Mono);
    builder.setPerformanceMode(PerformanceMode::LowLatency);
    builder.setSharingMode(SharingMode::Shared);

    builder.setCallback(this);

    builder.openStream(&stream);

    stream->setBufferSizeInFrames(stream->getFramesPerBurst() * 2);

    stream->requestStart();

    deviceSampleRate = stream->getSampleRate();

    isStreamOpened = true;
    isStreamStereo = isStereo;
}

void OggPianoEngine::release() {
    stream->flush();
    stream->close();
    delete stream;
    delete players;
    isStreamOpened = false;
    deviceSampleRate = -1;
}

DataCallbackResult
OggPianoEngine::onAudioReady(AudioStream *audioStream, void *audioData, int32_t numFrames) {
    for(int i = 0; i < players->size(); i++) {
        players->at(i).renderAudio(static_cast<float*>(audioData), numFrames, i == 0, audioStream->getChannelCount() != 1);
    }

    return DataCallbackResult::Continue;
}

int OggPianoEngine::addPlayer(std::vector<float> data, bool isStereo, int sampleRate) const {
    if(deviceSampleRate != -1) {
        players->push_back(OggPlayer(std::move(data), isStereo, sampleRate, deviceSampleRate));
    } else {
        players->push_back(OggPlayer(std::move(data), isStereo, sampleRate, sampleRate));
    }

    return static_cast<int>(players->size()) - 1;
}

void OggPianoEngine::addQueue(int id, float pan, float pitch, int playerScale) const {
    if(id >= 0 && id < players->size()) {
        players->at(id).addQueue(pan, pitch, playerScale);
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

        delete stream;

        isStreamOpened = false;
        deviceSampleRate = -1;
    }
}

void OggPianoEngine::reopenStream() {
    if(isStreamOpened) {
        closeStream();
    }

    start(isStreamStereo);
}
