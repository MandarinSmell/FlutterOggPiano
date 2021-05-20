//
// Created by user on 4/1/2021.
//

#ifndef ANDROID_OGGPLAYER_H
#define ANDROID_OGGPLAYER_H

#include <android/asset_manager.h>
#include <vector>

const static float F_PI = (float) M_PI;
const static float F_PI_2 = F_PI / 2;
const static float B = 4/F_PI;
const static float C = -4/(F_PI*F_PI);

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
    float playScale;

    bool queueEnded = false;

    PlayerQueue(float pan, float pitch, float playScale, OggPlayer* player) {
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

    float fastSin(float degree) {
        return B * degree + C * degree * degree;
    }

    float fastCos(float degree) {
        return B * (degree + F_PI_2) + C * (degree + F_PI_2) * (degree + F_PI_2);
    }
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

    void addQueue(float pan, float pitch, float playerScale) {
        queues.push_back(PlayerQueue(pan, defaultPitch * pitch, playerScale, this));
    };

    static void resetAudioData(float* audioData, int32_t numFrames, bool isStreamStereo);

    std::vector<float> data;
};


#endif //ANDROID_OGGPLAYER_H
