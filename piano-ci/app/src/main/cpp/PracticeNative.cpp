#include <aaudio/AAudio.h>
#include <jni.h>

#include <algorithm>
#include <array>
#include <atomic>
#include <cmath>

namespace {
constexpr double kTwoPi = 6.283185307179586476925286766559;

struct PracticeVoice {
    double phase = 0.0;
    float envelope = 0.0f;
};

class PracticeNativeEngine {
public:
    PracticeNativeEngine() {
        for (auto& gate : gates_) gate.store(false);
        for (auto& velocity : velocities_) velocity.store(96);
    }

    bool start() {
        if (stream_ != nullptr) return true;
        AAudioStreamBuilder* builder = nullptr;
        if (AAudio_createStreamBuilder(&builder) != AAUDIO_OK || builder == nullptr) return false;
        AAudioStreamBuilder_setDirection(builder, AAUDIO_DIRECTION_OUTPUT);
        AAudioStreamBuilder_setFormat(builder, AAUDIO_FORMAT_PCM_FLOAT);
        AAudioStreamBuilder_setChannelCount(builder, 2);
        AAudioStreamBuilder_setPerformanceMode(builder, AAUDIO_PERFORMANCE_MODE_LOW_LATENCY);
        AAudioStreamBuilder_setSharingMode(builder, AAUDIO_SHARING_MODE_EXCLUSIVE);
        AAudioStreamBuilder_setDataCallback(builder, dataCallback, this);
        aaudio_result_t result = AAudioStreamBuilder_openStream(builder, &stream_);
        if (result != AAUDIO_OK || stream_ == nullptr) {
            AAudioStreamBuilder_setSharingMode(builder, AAUDIO_SHARING_MODE_SHARED);
            result = AAudioStreamBuilder_openStream(builder, &stream_);
        }
        AAudioStreamBuilder_delete(builder);
        if (result != AAUDIO_OK || stream_ == nullptr) {
            stream_ = nullptr;
            return false;
        }
        sampleRate_ = AAudioStream_getSampleRate(stream_);
        if (sampleRate_ <= 0) sampleRate_ = 48000;
        result = AAudioStream_requestStart(stream_);
        if (result != AAUDIO_OK) {
            AAudioStream_close(stream_);
            stream_ = nullptr;
            return false;
        }
        resetMetronome_.store(true);
        return true;
    }

    void stop() {
        if (stream_ == nullptr) return;
        allNotesOff();
        metronomeEnabled_.store(false);
        AAudioStream_requestStop(stream_);
        AAudioStream_close(stream_);
        stream_ = nullptr;
    }

    void noteOn(int midi, int velocity) {
        if (midi < 0 || midi > 127) return;
        velocities_[midi].store(std::clamp(velocity, 1, 127));
        gates_[midi].store(true);
    }

    void noteOff(int midi) {
        if (midi < 0 || midi > 127) return;
        gates_[midi].store(false);
    }

    void setMetronome(bool enabled, int bpm) {
        metronomeBpm_.store(std::clamp(bpm, 50, 120));
        const bool previous = metronomeEnabled_.exchange(enabled);
        if (previous != enabled) resetMetronome_.store(true);
    }

    void allNotesOff() {
        for (auto& gate : gates_) gate.store(false);
    }

private:
    static aaudio_data_callback_result_t dataCallback(
        AAudioStream*,
        void* userData,
        void* audioData,
        int32_t numFrames
    ) {
        return static_cast<PracticeNativeEngine*>(userData)->render(static_cast<float*>(audioData), numFrames);
    }

    float metronomeSample() {
        if (!metronomeEnabled_.load()) {
            clickEnvelope_ = 0.0f;
            framesUntilBeat_ = 0.0;
            beat_ = 0;
            return 0.0f;
        }
        if (resetMetronome_.exchange(false)) {
            framesUntilBeat_ = 0.0;
            beat_ = 0;
            clickEnvelope_ = 0.0f;
        }
        const int bpm = metronomeBpm_.load();
        const double framesPerBeat = static_cast<double>(sampleRate_) * 60.0 / static_cast<double>(bpm);
        if (framesUntilBeat_ <= 0.0) {
            const bool accent = (beat_ % 4) == 0;
            clickFrequency_ = accent ? 2100.0 : 1500.0;
            clickEnvelope_ = accent ? 0.34f : 0.23f;
            clickPhase_ = 0.0;
            beat_ = (beat_ + 1) % 4;
            framesUntilBeat_ += framesPerBeat;
        }
        float click = 0.0f;
        if (clickEnvelope_ > 0.0002f) {
            click = static_cast<float>(std::sin(clickPhase_)) * clickEnvelope_;
            clickPhase_ += kTwoPi * clickFrequency_ / static_cast<double>(sampleRate_);
            if (clickPhase_ >= kTwoPi) clickPhase_ -= kTwoPi;
            clickEnvelope_ *= 0.9967f;
        }
        framesUntilBeat_ -= 1.0;
        return click;
    }

    aaudio_data_callback_result_t render(float* output, int32_t frames) {
        if (output == nullptr || frames <= 0) return AAUDIO_CALLBACK_RESULT_CONTINUE;
        const float attack = 1.0f / std::max(1.0f, static_cast<float>(sampleRate_) * 0.004f);
        const float release = 1.0f / std::max(1.0f, static_cast<float>(sampleRate_) * 0.18f);

        for (int frame = 0; frame < frames; ++frame) {
            float mix = 0.0f;
            int active = 0;
            for (int midi = 36; midi <= 84; ++midi) {
                auto& voice = voices_[midi];
                if (gates_[midi].load()) {
                    voice.envelope = std::min(1.0f, voice.envelope + attack);
                } else {
                    voice.envelope = std::max(0.0f, voice.envelope - release);
                }
                if (voice.envelope <= 0.0001f) continue;
                const double frequency = 440.0 * std::pow(2.0, (static_cast<double>(midi) - 69.0) / 12.0);
                const double increment = kTwoPi * frequency / static_cast<double>(sampleRate_);
                const float velocityGain = static_cast<float>(velocities_[midi].load()) / 127.0f;
                const float tone = static_cast<float>(
                    std::sin(voice.phase) +
                    0.28 * std::sin(2.0 * voice.phase) +
                    0.10 * std::sin(3.0 * voice.phase)
                );
                mix += tone * voice.envelope * velocityGain;
                ++active;
                voice.phase += increment;
                if (voice.phase >= kTwoPi) voice.phase -= kTwoPi;
            }
            if (active > 0) {
                mix = std::tanh(mix * (0.48f / std::sqrt(static_cast<float>(active))));
            }
            mix += metronomeSample();
            mix = std::tanh(mix);
            output[frame * 2] = mix;
            output[frame * 2 + 1] = mix;
        }
        return AAUDIO_CALLBACK_RESULT_CONTINUE;
    }

    AAudioStream* stream_ = nullptr;
    int32_t sampleRate_ = 48000;
    std::array<std::atomic<bool>, 128> gates_{};
    std::array<std::atomic<int>, 128> velocities_{};
    std::array<PracticeVoice, 128> voices_{};
    std::atomic<bool> metronomeEnabled_{false};
    std::atomic<int> metronomeBpm_{80};
    std::atomic<bool> resetMetronome_{true};
    double framesUntilBeat_ = 0.0;
    double clickPhase_ = 0.0;
    double clickFrequency_ = 2100.0;
    float clickEnvelope_ = 0.0f;
    int beat_ = 0;
};

PracticeNativeEngine gPracticeEngine;
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_pianostudio_alpha_PracticeAudioEngine_nativeStart(JNIEnv*, jobject) {
    return gPracticeEngine.start() ? JNI_TRUE : JNI_FALSE;
}

extern "C" JNIEXPORT void JNICALL
Java_com_pianostudio_alpha_PracticeAudioEngine_nativeStop(JNIEnv*, jobject) {
    gPracticeEngine.stop();
}

extern "C" JNIEXPORT void JNICALL
Java_com_pianostudio_alpha_PracticeAudioEngine_nativeNoteOn(JNIEnv*, jobject, jint midi, jint velocity) {
    gPracticeEngine.noteOn(midi, velocity);
}

extern "C" JNIEXPORT void JNICALL
Java_com_pianostudio_alpha_PracticeAudioEngine_nativeNoteOff(JNIEnv*, jobject, jint midi) {
    gPracticeEngine.noteOff(midi);
}

extern "C" JNIEXPORT void JNICALL
Java_com_pianostudio_alpha_PracticeAudioEngine_nativeSetMetronome(JNIEnv*, jobject, jboolean enabled, jint bpm) {
    gPracticeEngine.setMetronome(enabled == JNI_TRUE, bpm);
}

extern "C" JNIEXPORT void JNICALL
Java_com_pianostudio_alpha_PracticeAudioEngine_nativeAllNotesOff(JNIEnv*, jobject) {
    gPracticeEngine.allNotesOff();
}
