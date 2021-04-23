//
// Created by user on 4/1/2021.
//

#ifndef ANDROID_OGGPLAYER_H
#define ANDROID_OGGPLAYER_H

#include <android/asset_manager.h>
#include <vector>

const static float MIX_RATIO = 0.8;

class OggPlayer;

class PlayerQueue {
private:
    OggPlayer* player;

    void renderStereo(float* audioData, int32_t numFrames);
    void renderMono(float* audioData, int32_t numFrames);
public:
    int offset = 0;
    float pan;
    float pitch;
    int playScale;

    bool queueEnded = false;

    PlayerQueue(float pan, float pitch, int playScale, OggPlayer* player) {
        this->pan = pan;
        this->playScale = playScale;
        this->player = player;
        this->pitch = pitch;

        if(this->pan < -1.0)
            this->pan = -1.0;
        else if(this->pan > 1.0)
            this->pan = 1.0;
    }

    void renderAudio(float* audioData, int32_t numFrames, bool isStreamStereo);
};

class OggPlayer {
private:
    std::vector<PlayerQueue> queues = std::vector<PlayerQueue>();
public:

    int offset = 0;
    bool isStereo;
    float defaultPitch = 1.0;

    OggPlayer(std::vector<float> data, bool isStereo, int fileSampleRate, int deviceSampleRate) {
        this->data = data;
        this->isStereo = isStereo;
        defaultPitch = (float) (fileSampleRate) / (float) (deviceSampleRate);
    }

    void renderAudio(float* audioData, int32_t numFrames, bool reset, bool isStreamStereo);
    static void smoothAudio(float* audioData, int32_t numFrames, bool isStreamStereo);

    void addQueue(float pan, float pitch, int playerScale) {
        queues.push_back(PlayerQueue(pan, defaultPitch * pitch, playerScale, this));
    };

    static void resetAudioData(float* audioData, int32_t numFrames, bool isStreamStereo);

    std::vector<float> data;
};


#endif //ANDROID_OGGPLAYER_H
