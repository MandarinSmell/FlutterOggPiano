//
// Created by user on 4/1/2021.
//

#ifndef ANDROID_OGGPIANOENGINE_H
#define ANDROID_OGGPIANOENGINE_H

#include <oboe/Oboe.h>
#include <vector>

#include "OggPlayer.h"

enum MODE {
    LOW_LATENCY,
    POWER_SAVING
};

using namespace oboe;

class OggPianoEngine : public AudioStreamCallback {
public:
    void initialize();
    void start(bool isStereo, MODE mode);
    void closeStream();
    void reopenStream();
    void release();

    bool isStreamOpened = false;
    bool isStreamStereo;

    int deviceSampleRate = 0;

    MODE selectedMode = LOW_LATENCY;

    DataCallbackResult
    onAudioReady(AudioStream *audioStream, void *audioData, int32_t numFrames) override;
    void onErrorAfterClose(AudioStream *audioStream, Result result) override ;

    std::shared_ptr<AudioStream> stream;
    std::vector<OggPlayer> players = std::vector<OggPlayer>();

    int addPlayer(std::vector<float> data, int index, bool forceLoad, bool isStereo, int sampleRate);

    void addQueue(int id, float pan, float pitch, float playerScale);
};


#endif //ANDROID_OGGPIANOENGINE_H
