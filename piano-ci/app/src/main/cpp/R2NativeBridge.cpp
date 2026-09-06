#include <jni.h>

// Forward declarations for the already-tested native entry points.
extern "C" JNIEXPORT jboolean JNICALL Java_com_pianostudio_alpha_PianoAudioEngine_nativeStart(JNIEnv*, jobject);
extern "C" JNIEXPORT void JNICALL Java_com_pianostudio_alpha_PianoAudioEngine_nativeStop(JNIEnv*, jobject);
extern "C" JNIEXPORT void JNICALL Java_com_pianostudio_alpha_PianoAudioEngine_nativeNoteOn(JNIEnv*, jobject, jint, jint);
extern "C" JNIEXPORT void JNICALL Java_com_pianostudio_alpha_PianoAudioEngine_nativeNoteOff(JNIEnv*, jobject, jint);
extern "C" JNIEXPORT void JNICALL Java_com_pianostudio_alpha_PianoAudioEngine_nativeSetSustain(JNIEnv*, jobject, jboolean);
extern "C" JNIEXPORT void JNICALL Java_com_pianostudio_alpha_PianoAudioEngine_nativeSetMetronome(JNIEnv*, jobject, jboolean, jint);
extern "C" JNIEXPORT void JNICALL Java_com_pianostudio_alpha_PianoAudioEngine_nativeAllNotesOff(JNIEnv*, jobject);

extern "C" JNIEXPORT jboolean JNICALL Java_com_pianostudio_alpha_PracticeAudioEngine_nativeStart(JNIEnv*, jobject);
extern "C" JNIEXPORT void JNICALL Java_com_pianostudio_alpha_PracticeAudioEngine_nativeStop(JNIEnv*, jobject);
extern "C" JNIEXPORT void JNICALL Java_com_pianostudio_alpha_PracticeAudioEngine_nativeNoteOn(JNIEnv*, jobject, jint, jint);
extern "C" JNIEXPORT void JNICALL Java_com_pianostudio_alpha_PracticeAudioEngine_nativeNoteOff(JNIEnv*, jobject, jint);
extern "C" JNIEXPORT void JNICALL Java_com_pianostudio_alpha_PracticeAudioEngine_nativeSetMetronome(JNIEnv*, jobject, jboolean, jint);
extern "C" JNIEXPORT void JNICALL Java_com_pianostudio_alpha_PracticeAudioEngine_nativeAllNotesOff(JNIEnv*, jobject);

extern "C" JNIEXPORT jboolean JNICALL Java_com_pianostudio_alpha_LessonAudioEngine_nativeStart(JNIEnv*, jobject);
extern "C" JNIEXPORT void JNICALL Java_com_pianostudio_alpha_LessonAudioEngine_nativeStop(JNIEnv*, jobject);
extern "C" JNIEXPORT void JNICALL Java_com_pianostudio_alpha_LessonAudioEngine_nativeNoteOn(JNIEnv*, jobject, jint, jint);
extern "C" JNIEXPORT void JNICALL Java_com_pianostudio_alpha_LessonAudioEngine_nativeNoteOff(JNIEnv*, jobject, jint);
extern "C" JNIEXPORT void JNICALL Java_com_pianostudio_alpha_LessonAudioEngine_nativeAllNotesOff(JNIEnv*, jobject);

// R2 Free Piano -> shared PianoAudioEngine.
extern "C" JNIEXPORT jboolean JNICALL Java_com_pianostudio_alpha_R2AudioEngine_nativeStart(JNIEnv* env, jobject thiz) {
    return Java_com_pianostudio_alpha_PianoAudioEngine_nativeStart(env, thiz);
}
extern "C" JNIEXPORT void JNICALL Java_com_pianostudio_alpha_R2AudioEngine_nativeStop(JNIEnv* env, jobject thiz) {
    Java_com_pianostudio_alpha_PianoAudioEngine_nativeStop(env, thiz);
}
extern "C" JNIEXPORT void JNICALL Java_com_pianostudio_alpha_R2AudioEngine_nativeNoteOn(JNIEnv* env, jobject thiz, jint midi, jint velocity) {
    Java_com_pianostudio_alpha_PianoAudioEngine_nativeNoteOn(env, thiz, midi, velocity);
}
extern "C" JNIEXPORT void JNICALL Java_com_pianostudio_alpha_R2AudioEngine_nativeNoteOff(JNIEnv* env, jobject thiz, jint midi) {
    Java_com_pianostudio_alpha_PianoAudioEngine_nativeNoteOff(env, thiz, midi);
}
extern "C" JNIEXPORT void JNICALL Java_com_pianostudio_alpha_R2AudioEngine_nativeSetSustain(JNIEnv* env, jobject thiz, jboolean enabled) {
    Java_com_pianostudio_alpha_PianoAudioEngine_nativeSetSustain(env, thiz, enabled);
}
extern "C" JNIEXPORT void JNICALL Java_com_pianostudio_alpha_R2AudioEngine_nativeSetMetronome(JNIEnv* env, jobject thiz, jboolean enabled, jint bpm) {
    Java_com_pianostudio_alpha_PianoAudioEngine_nativeSetMetronome(env, thiz, enabled, bpm);
}
extern "C" JNIEXPORT void JNICALL Java_com_pianostudio_alpha_R2AudioEngine_nativeAllNotesOff(JNIEnv* env, jobject thiz) {
    Java_com_pianostudio_alpha_PianoAudioEngine_nativeAllNotesOff(env, thiz);
}

// R2 Practice -> existing practice engine.
extern "C" JNIEXPORT jboolean JNICALL Java_com_pianostudio_alpha_R2PracticeAudio_nativeStart(JNIEnv* env, jobject thiz) {
    return Java_com_pianostudio_alpha_PracticeAudioEngine_nativeStart(env, thiz);
}
extern "C" JNIEXPORT void JNICALL Java_com_pianostudio_alpha_R2PracticeAudio_nativeStop(JNIEnv* env, jobject thiz) {
    Java_com_pianostudio_alpha_PracticeAudioEngine_nativeStop(env, thiz);
}
extern "C" JNIEXPORT void JNICALL Java_com_pianostudio_alpha_R2PracticeAudio_nativeNoteOn(JNIEnv* env, jobject thiz, jint midi, jint velocity) {
    Java_com_pianostudio_alpha_PracticeAudioEngine_nativeNoteOn(env, thiz, midi, velocity);
}
extern "C" JNIEXPORT void JNICALL Java_com_pianostudio_alpha_R2PracticeAudio_nativeNoteOff(JNIEnv* env, jobject thiz, jint midi) {
    Java_com_pianostudio_alpha_PracticeAudioEngine_nativeNoteOff(env, thiz, midi);
}
extern "C" JNIEXPORT void JNICALL Java_com_pianostudio_alpha_R2PracticeAudio_nativeSetMetronome(JNIEnv* env, jobject thiz, jboolean enabled, jint bpm) {
    Java_com_pianostudio_alpha_PracticeAudioEngine_nativeSetMetronome(env, thiz, enabled, bpm);
}
extern "C" JNIEXPORT void JNICALL Java_com_pianostudio_alpha_R2PracticeAudio_nativeAllNotesOff(JNIEnv* env, jobject thiz) {
    Java_com_pianostudio_alpha_PracticeAudioEngine_nativeAllNotesOff(env, thiz);
}

// R2 Lesson -> shared lesson/piano engine.
extern "C" JNIEXPORT jboolean JNICALL Java_com_pianostudio_alpha_R2LessonAudio_nativeStart(JNIEnv* env, jobject thiz) {
    return Java_com_pianostudio_alpha_LessonAudioEngine_nativeStart(env, thiz);
}
extern "C" JNIEXPORT void JNICALL Java_com_pianostudio_alpha_R2LessonAudio_nativeStop(JNIEnv* env, jobject thiz) {
    Java_com_pianostudio_alpha_LessonAudioEngine_nativeStop(env, thiz);
}
extern "C" JNIEXPORT void JNICALL Java_com_pianostudio_alpha_R2LessonAudio_nativeNoteOn(JNIEnv* env, jobject thiz, jint midi, jint velocity) {
    Java_com_pianostudio_alpha_LessonAudioEngine_nativeNoteOn(env, thiz, midi, velocity);
}
extern "C" JNIEXPORT void JNICALL Java_com_pianostudio_alpha_R2LessonAudio_nativeNoteOff(JNIEnv* env, jobject thiz, jint midi) {
    Java_com_pianostudio_alpha_LessonAudioEngine_nativeNoteOff(env, thiz, midi);
}
extern "C" JNIEXPORT void JNICALL Java_com_pianostudio_alpha_R2LessonAudio_nativeAllNotesOff(JNIEnv* env, jobject thiz) {
    Java_com_pianostudio_alpha_LessonAudioEngine_nativeAllNotesOff(env, thiz);
}
