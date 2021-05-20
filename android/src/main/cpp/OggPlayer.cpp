//
// Created by user on 4/1/2021.
//

#include "OggPlayer.h"

void OggPlayer::renderAudio(float *audioData, int32_t numFrames, bool reset, bool isStreamStereo) {
    if(reset) {
        resetAudioData(audioData, numFrames, isStreamStereo);
    }

    for(auto & queue : queues) {
        if(!queue.queueEnded) {
            queue.renderAudio(audioData, numFrames, isStreamStereo);
        }
    }

    smoothAudio(audioData, numFrames, isStreamStereo);

    queues.erase(std::remove_if(queues.begin(), queues.end(),
            [](const PlayerQueue& p) {return p.queueEnded;}), queues.end());
}

void OggPlayer::smoothAudio(float *audioData, int32_t numFrames, bool isStreamStereo) {
    for(int i = 0; i < numFrames; i++) {
        if(isStreamStereo) {
            if(audioData[i * 2] > 1.0)
                audioData[i * 2] = 1.0;
            else if(audioData[i * 2] < -1.0)
                audioData[i * 2] = -1.0;

            if(audioData[i * 2 + 1] > 1.0)
                audioData[i * 2 + 1] = 1.0;
            else if(audioData[i * 2 + 1] < -1.0)
                audioData[i * 2 + 1] = -1.0;
        } else {
            if(audioData[i] > 1.0)
                audioData[i] = 1.0;
            else if(audioData[i] < -1.0)
                audioData[i] = -1.0;
        }
    }
}

void OggPlayer::resetAudioData(float *audioData, int32_t numFrames, bool isStreamStereo) {
    for(int i = 0; i < numFrames; i++) {
        if(isStreamStereo) {
            audioData[i * 2] = 0;
            audioData[i * 2 + 1] = 0;
        } else {
            audioData[i] = 0;
        }
    }
}

void PlayerQueue::renderAudio(float * audioData, int32_t numFrames, bool isStreamStereo) {
    if(isStreamStereo) {
        renderStereo(audioData, numFrames);
    } else {
        renderMono(audioData, numFrames);
    }
}

void PlayerQueue::renderStereo(float *audioData, int32_t numFrames) {
    for(int i = 0; i < numFrames; i++) {
        if(player->isStereo) {
            if((int) ((float) (offset + i) * pitch) * 2 + 1 < player->data.size()) {
                float left = player->data.at((int)((float) (offset + i) * pitch) * 2);
                float right = player->data.at((int)((float) (offset + i) * pitch)  * 2 + 1);

                if(pan == 0) {
                    audioData[i * 2] += left * (float) playScale;
                    audioData[i * 2 + 1] += right * (float) playScale;
                } else if(pan < 0) {
                    audioData[i * 2] += (left + right * fastSin(-pan * F_PI / 2)) * (float) playScale;
                    audioData[i * 2 + 1] += right * fastCos(-pan * F_PI / 2) * (float) playScale;
                } else {
                    audioData[i * 2] += left * fastCos(pan * F_PI / 2) * (float) playScale;
                    audioData[i * 2 + 1] += (right + left * fastSin(pan * F_PI / 2)) * (float) playScale;
                }
            } else {
                break;
            }
        } else {
            if((int) ((float) (offset + i) * pitch) < player->data.size()) {
                float sample = player->data.at((int) ((float) (offset + i) * pitch));

                if(pan == 0) {
                    audioData[i * 2] += sample * (float) playScale;
                    audioData[i * 2] += sample * (float) playScale;
                } else if(pan < 0) {
                    audioData[i * 2] += sample * (1 + fastSin(-pan * F_PI / 2)) * (float) playScale;
                    audioData[i * 2 + 1] += sample * fastCos(-pan * F_PI / 2) * (float) playScale;
                } else {
                    audioData[i * 2] += sample * fastCos(pan * F_PI / 2) * (float) playScale;
                    audioData[i * 2 + 1] += sample * (1 + fastSin(pan * F_PI / 2)) * (float) playScale;
                }
            } else {
                break;
            }
        }
    }

    offset += numFrames;

    if((float) offset * pitch >= player->data.size()) {
        offset = 0;
        queueEnded = true;
    }
}

void PlayerQueue::renderMono(float *audioData, int32_t numFrames) {
    for(int i = 0; i < numFrames; i++) {
        if(player->isStereo) {
            if((int) ((float) (offset + i) * pitch) * 2 + 1 < player->data.size()) {
                audioData[i] += (player->data.at((int) ((float) (offset + i) * pitch) * 2) + player->data.at((int) ((float) (offset + i) * pitch) * 2 + 1)) / 2 * (float) playScale;
            } else {
                break;
            }
        } else {
            if((int) ((float) (offset + i) * pitch) < player->data.size()) {
                audioData[i] += player->data.at((int) ((float) (offset + i) * pitch)) * (float) playScale;
            } else {
                break;
            }
        }

        if(audioData[i] > 1.0)
            audioData[i] = 1.0;
        else if(audioData[i] < -1.0)
            audioData[i] = -1.0;
    }

    offset += numFrames;

    if((float) offset * pitch >= player->data.size()) {
        queueEnded = true;
        offset = 0;
    }
}