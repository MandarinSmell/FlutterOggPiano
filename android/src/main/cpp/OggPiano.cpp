//
// Created by user on 4/2/2021.
//

#include <jni.h>
#include <vector>
#include <algorithm>
#include <string>

#include "OggPianoEngine.h"

using namespace std;

OggPianoEngine engine;

extern "C" void Java_com_mandarin_flutter_1ogg_1piano_FlutterOggPianoPlugin_initializeEngine(JNIEnv *env, jobject instance, jboolean isStereo, jint mode) {
    engine.initialize();
    engine.start(isStereo, mode == 0 ? LOW_LATENCY : POWER_SAVING);
}

extern "C" jint Java_com_mandarin_flutter_1ogg_1piano_FlutterOggPianoPlugin_addPlayer(JNIEnv *env, jobject instance, jfloatArray data, jboolean isStereo, jint sampleRate) {
    int size = env -> GetArrayLength(data);

    auto* parse = (jfloat*) (env -> GetFloatArrayElements(data, nullptr));

    auto* vectorData = new vector<float>;

    for(int i = 0; i < size; i++) {
        vectorData -> push_back(parse[i]);
    }

    return engine.addPlayer(*vectorData, isStereo, sampleRate);
}

extern "C" void Java_com_mandarin_flutter_1ogg_1piano_FlutterOggPianoPlugin_addQueue(JNIEnv *env, jobject instance, jint id, jfloat pan, jfloat pitch, jfloat playerScale) {
    engine.addQueue(id, pan, pitch, playerScale);
}

extern "C" void Java_com_mandarin_flutter_1ogg_1piano_FlutterOggPianoPlugin_release(JNIEnv *env, jobject instance) {
    engine.release();
}