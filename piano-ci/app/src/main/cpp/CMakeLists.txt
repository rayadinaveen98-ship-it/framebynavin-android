cmake_minimum_required(VERSION 3.22.1)
project(pianostudio_audio LANGUAGES CXX)
add_library(
    pianostudio_audio
    SHARED
    PianoAudioEngine.cpp
    PracticeNative.cpp
)
find_library(aaudio-lib aaudio)
find_library(log-lib log)
target_link_libraries(pianostudio_audio PRIVATE ${aaudio-lib} ${log-lib})
