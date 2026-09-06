#include <jni.h>

extern "C" JNIEXPORT jboolean JNICALL Java_com_pianostudio_alpha_LessonAudioEngine_nativeStart(JNIEnv*, jobject);
extern "C" JNIEXPORT void JNICALL Java_com_pianostudio_alpha_LessonAudioEngine_nativeStop(JNIEnv*, jobject);
extern "C" JNIEXPORT void JNICALL Java_com_pianostudio_alpha_LessonAudioEngine_nativeNoteOn(JNIEnv*, jobject, jint, jint);
extern "C" JNIEXPORT void JNICALL Java_com_pianostudio_alpha_LessonAudioEngine_nativeNoteOff(JNIEnv*, jobject, jint);
extern "C" JNIEXPORT void JNICALL Java_com_pianostudio_alpha_LessonAudioEngine_nativeAllNotesOff(JNIEnv*, jobject);

extern "C" JNIEXPORT jboolean JNICALL
Java_com_pianostudio_alpha_StudioLessonAudioEngine_nativeStart(JNIEnv* env, jobject self) {
    return Java_com_pianostudio_alpha_LessonAudioEngine_nativeStart(env, self);
}

extern "C" JNIEXPORT void JNICALL
Java_com_pianostudio_alpha_StudioLessonAudioEngine_nativeStop(JNIEnv* env, jobject self) {
    Java_com_pianostudio_alpha_LessonAudioEngine_nativeStop(env, self);
}

extern "C" JNIEXPORT void JNICALL
Java_com_pianostudio_alpha_StudioLessonAudioEngine_nativeNoteOn(JNIEnv* env, jobject self, jint midi, jint velocity) {
    Java_com_pianostudio_alpha_LessonAudioEngine_nativeNoteOn(env, self, midi, velocity);
}

extern "C" JNIEXPORT void JNICALL
Java_com_pianostudio_alpha_StudioLessonAudioEngine_nativeNoteOff(JNIEnv* env, jobject self, jint midi) {
    Java_com_pianostudio_alpha_LessonAudioEngine_nativeNoteOff(env, self, midi);
}

extern "C" JNIEXPORT void JNICALL
Java_com_pianostudio_alpha_StudioLessonAudioEngine_nativeAllNotesOff(JNIEnv* env, jobject self) {
    Java_com_pianostudio_alpha_LessonAudioEngine_nativeAllNotesOff(env, self);
}
