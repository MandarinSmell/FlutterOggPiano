//
// Created by user on 4/1/2021.
//

#ifndef ANDROID_OGGPIANOENGINE_H
#define ANDROID_OGGPIANOENGINE_H

#include <oboe/Oboe.h>
#include <vector>

#include "OggPlayer.h"

using namespace oboe;

class OggPianoEngine : public AudioStreamCallback {
public:
    void initialize();
    void start(bool isStereo);
    void closeStream();
    void reopenStream();
    void release();

    bool isStreamOpened = false;
    bool isStreamStereo;

    int deviceSampleRate = 0;

    DataCallbackResult
    onAudioReady(AudioStream *audioStream, void *audioData, int32_t numFrames) override;
    void onErrorAfterClose(AudioStream *audioStream, Result result) override ;

    AudioStream* stream;
    std::vector<OggPlayer>* players;

    int addPlayer(std::vector<float> data, bool isStereo, int sampleRate) const;

    void addQueue(int id, float pan, float pitch, int playerScale) const;
};


#endif //ANDROID_OGGPIANOENGINE_H
