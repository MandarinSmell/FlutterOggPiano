cmake_minimum_required(VERSION 3.10.2)

add_library(
        oggPiano
        SHARED
        OggPianoEngine.cpp
        OggPlayer.cpp
        OggPiano.cpp
)

set(OBOE_DIR oboe)

add_subdirectory(${OBOE_DIR} oboe)

include_directories(${OBOE_DIR}/include)

target_link_libraries(oggPiano android log oboe)