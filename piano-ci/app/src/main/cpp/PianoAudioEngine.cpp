#include <aaudio/AAudio.h>
#include <android/log.h>
#include <jni.h>

#include <algorithm>
#include <array>
#include <atomic>
#include <cmath>
#include <cstdint>

namespace {
constexpr const char* kTag = "PianoStudioAudio";
constexpr double kTwoPi = 6.283185307179586476925286766559;
constexpr int kMidiNotes = 128;

struct VoiceState {
    double phase = 0.0;
    float envelope = 0.0f;
    bool wasSounding = false;
};

class PianoEngine {
public:
    PianoEngine() {
        for (auto& gate : gates_) gate.store(false);
        for (auto& velocity : velocities_) velocity.store(96);
        for (auto& hold : sustainHolds_) hold.store(false);
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
        AAudioStreamBuilder_setErrorCallback(builder, errorCallback, this);
        aaudio_result_t result = AAudioStreamBuilder_openStream(builder, &stream_);
        if (result != AAUDIO_OK || stream_ == nullptr) {
            AAudioStreamBuilder_setSharingMode(builder, AAUDIO_SHARING_MODE_SHARED);
            result = AAudioStreamBuilder_openStream(builder, &stream_);
        }
        AAudioStreamBuilder_delete(builder);
        if (result != AAUDIO_OK || stream_ == nullptr) {
            __android_log_print(ANDROID_LOG_ERROR, kTag, "Unable to open AAudio stream: %s", AAudio_convertResultToText(result));
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
        resetMetronome_.store(true, std::memory_order_relaxed);
        return true;
    }

    void stop() {
        if (stream_ == nullptr) return;
        allNotesOff();
        metronomeEnabled_.store(false, std::memory_order_relaxed);
        AAudioStream_requestStop(stream_);
        AAudioStream_close(stream_);
        stream_ = nullptr;
    }

    void noteOn(int midi, int velocity) {
        if (midi < 0 || midi >= kMidiNotes) return;
        velocities_[midi].store(std::clamp(velocity, 1, 127), std::memory_order_relaxed);
        sustainHolds_[midi].store(false, std::memory_order_relaxed);
        gates_[midi].store(true, std::memory_order_release);
    }

    void noteOff(int midi) {
        if (midi < 0 || midi >= kMidiNotes) return;
        gates_[midi].store(false, std::memory_order_release);
        if (sustain_.load(std::memory_order_relaxed)) {
            sustainHolds_[midi].store(true, std::memory_order_relaxed);
        }
    }

    void setSustain(bool enabled) {
        sustain_.store(enabled, std::memory_order_relaxed);
        if (!enabled) {
            for (int midi = 0; midi < kMidiNotes; ++midi) {
                if (!gates_[midi].load(std::memory_order_relaxed)) {
                    sustainHolds_[midi].store(false, std::memory_order_relaxed);
                }
            }
        }
    }

    void setMetronome(bool enabled, int bpm) {
        metronomeBpm_.store(std::clamp(bpm, 40, 220), std::memory_order_relaxed);
        const bool previous = metronomeEnabled_.exchange(enabled, std::memory_order_relaxed);
        if (enabled != previous) resetMetronome_.store(true, std::memory_order_release);
    }

    void allNotesOff() {
        sustain_.store(false, std::memory_order_relaxed);
        for (int midi = 0; midi < kMidiNotes; ++midi) {
            gates_[midi].store(false, std::memory_order_relaxed);
            sustainHolds_[midi].store(false, std::memory_order_relaxed);
        }
    }

private:
    static aaudio_data_callback_result_t dataCallback(
        AAudioStream*,
        void* userData,
        void* audioData,
        int32_t numFrames
    ) {
        return static_cast<PianoEngine*>(userData)->render(static_cast<float*>(audioData), numFrames);
    }

    static void errorCallback(AAudioStream*, void*, aaudio_result_t error) {
        __android_log_print(ANDROID_LOG_ERROR, kTag, "AAudio stream error: %s", AAudio_convertResultToText(error));
    }

    float renderMetronomeSample() {
        if (!metronomeEnabled_.load(std::memory_order_relaxed)) {
            metronomeEnvelope_ = 0.0f;
            metronomeFramesUntilBeat_ = 0.0;
            metronomeBeat_ = 0;
            return 0.0f;
        }

        if (resetMetronome_.exchange(false, std::memory_order_acq_rel)) {
            metronomeFramesUntilBeat_ = 0.0;
            metronomeBeat_ = 0;
            metronomeEnvelope_ = 0.0f;
        }

        const int bpm = std::clamp(metronomeBpm_.load(std::memory_order_relaxed), 40, 220);
        const double framesPerBeat = static_cast<double>(sampleRate_) * 60.0 / static_cast<double>(bpm);
        if (metronomeFramesUntilBeat_ <= 0.0) {
            const bool accent = (metronomeBeat_ % 4) == 0;
            metronomeClickFrequency_ = accent ? 2200.0 : 1550.0;
            metronomeEnvelope_ = accent ? 0.34f : 0.25f;
            metronomePhase_ = 0.0;
            metronomeBeat_ = (metronomeBeat_ + 1) % 4;
            metronomeFramesUntilBeat_ += framesPerBeat;
        }

        float click = 0.0f;
        if (metronomeEnvelope_ > 0.0002f) {
            click = static_cast<float>(std::sin(metronomePhase_)) * metronomeEnvelope_;
            metronomePhase_ += kTwoPi * metronomeClickFrequency_ / static_cast<double>(sampleRate_);
            if (metronomePhase_ >= kTwoPi) metronomePhase_ -= kTwoPi;
            metronomeEnvelope_ *= 0.9968f;
        }
        metronomeFramesUntilBeat_ -= 1.0;
        return click;
    }

    aaudio_data_callback_result_t render(float* output, int32_t frames) {
        if (output == nullptr || frames <= 0) return AAUDIO_CALLBACK_RESULT_CONTINUE;
        const float attackStep = 1.0f / std::max(1.0f, static_cast<float>(sampleRate_) * 0.004f);
        const float releaseStep = 1.0f / std::max(1.0f, static_cast<float>(sampleRate_) * 0.28f);

        for (int frame = 0; frame < frames; ++frame) {
            float mix = 0.0f;
            int active = 0;
            for (int midi = 21; midi <= 108; ++midi) {
                const bool keyDown = gates_[midi].load(std::memory_order_acquire);
                const bool held = sustainHolds_[midi].load(std::memory_order_relaxed);
                const bool sounding = keyDown || held;
                auto& voice = voices_[midi];
                if (sounding && !voice.wasSounding) {
                    voice.phase = 0.0;
                    voice.envelope = std::max(voice.envelope, 0.015f);
                }
                if (sounding) {
                    voice.envelope = std::min(1.0f, voice.envelope + attackStep);
                } else if (voice.envelope > 0.0f) {
                    voice.envelope = std::max(0.0f, voice.envelope - releaseStep);
                }
                voice.wasSounding = sounding;
                if (voice.envelope <= 0.0001f) continue;

                const double frequency = 440.0 * std::pow(2.0, (static_cast<double>(midi) - 69.0) / 12.0);
                const double increment = kTwoPi * frequency / static_cast<double>(sampleRate_);
                const float velocityGain = static_cast<float>(velocities_[midi].load(std::memory_order_relaxed)) / 127.0f;
                const double phase = voice.phase;
                const float tone = static_cast<float>(
                    std::sin(phase) +
                    0.31 * std::sin(2.0 * phase + 0.035) +
                    0.13 * std::sin(3.0 * phase + 0.09)
                );
                mix += tone * voice.envelope * velocityGain;
                ++active;
                voice.phase += increment;
                if (voice.phase >= kTwoPi) voice.phase -= kTwoPi;
            }

            if (active > 0) {
                mix = std::tanh(mix * (0.52f / std::sqrt(static_cast<float>(active))));
            }
            mix += renderMetronomeSample();
            mix = std::tanh(mix);
            output[frame * 2] = mix;
            output[frame * 2 + 1] = mix;
        }
        return AAUDIO_CALLBACK_RESULT_CONTINUE;
    }

    AAudioStream* stream_ = nullptr;
    int32_t sampleRate_ = 48000;
    std::array<std::atomic<bool>, kMidiNotes> gates_{};
    std::array<std::atomic<int>, kMidiNotes> velocities_{};
    std::array<std::atomic<bool>, kMidiNotes> sustainHolds_{};
    std::array<VoiceState, kMidiNotes> voices_{};
    std::atomic<bool> sustain_{false};

    std::atomic<bool> metronomeEnabled_{false};
    std::atomic<int> metronomeBpm_{96};
    std::atomic<bool> resetMetronome_{true};
    double metronomeFramesUntilBeat_ = 0.0;
    double metronomePhase_ = 0.0;
    double metronomeClickFrequency_ = 2200.0;
    float metronomeEnvelope_ = 0.0f;
    int metronomeBeat_ = 0;
};

PianoEngine gEngine;
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_pianostudio_alpha_PianoAudioEngine_nativeStart(JNIEnv*, jobject) {
    return gEngine.start() ? JNI_TRUE : JNI_FALSE;
}

extern "C" JNIEXPORT void JNICALL
Java_com_pianostudio_alpha_PianoAudioEngine_nativeStop(JNIEnv*, jobject) {
    gEngine.stop();
}

extern "C" JNIEXPORT void JNICALL
Java_com_pianostudio_alpha_PianoAudioEngine_nativeNoteOn(JNIEnv*, jobject, jint midi, jint velocity) {
    gEngine.noteOn(midi, velocity);
}

extern "C" JNIEXPORT void JNICALL
Java_com_pianostudio_alpha_PianoAudioEngine_nativeNoteOff(JNIEnv*, jobject, jint midi) {
    gEngine.noteOff(midi);
}

extern "C" JNIEXPORT void JNICALL
Java_com_pianostudio_alpha_PianoAudioEngine_nativeSetSustain(JNIEnv*, jobject, jboolean enabled) {
    gEngine.setSustain(enabled == JNI_TRUE);
}

extern "C" JNIEXPORT void JNICALL
Java_com_pianostudio_alpha_PianoAudioEngine_nativeSetMetronome(JNIEnv*, jobject, jboolean enabled, jint bpm) {
    gEngine.setMetronome(enabled == JNI_TRUE, bpm);
}

extern "C" JNIEXPORT void JNICALL
Java_com_pianostudio_alpha_PianoAudioEngine_nativeAllNotesOff(JNIEnv*, jobject) {
    gEngine.allNotesOff();
}

// The lesson surface intentionally shares the same single low-latency engine.
extern "C" JNIEXPORT jboolean JNICALL
Java_com_pianostudio_alpha_LessonAudioEngine_nativeStart(JNIEnv*, jobject) {
    return gEngine.start() ? JNI_TRUE : JNI_FALSE;
}

extern "C" JNIEXPORT void JNICALL
Java_com_pianostudio_alpha_LessonAudioEngine_nativeStop(JNIEnv*, jobject) {
    gEngine.stop();
}

extern "C" JNIEXPORT void JNICALL
Java_com_pianostudio_alpha_LessonAudioEngine_nativeNoteOn(JNIEnv*, jobject, jint midi, jint velocity) {
    gEngine.noteOn(midi, velocity);
}

extern "C" JNIEXPORT void JNICALL
Java_com_pianostudio_alpha_LessonAudioEngine_nativeNoteOff(JNIEnv*, jobject, jint midi) {
    gEngine.noteOff(midi);
}

extern "C" JNIEXPORT void JNICALL
Java_com_pianostudio_alpha_LessonAudioEngine_nativeAllNotesOff(JNIEnv*, jobject) {
    gEngine.allNotesOff();
}
