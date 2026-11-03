cmake_minimum_required(VERSION 3.22.1)

project("scadroid" CXX)

set(CMAKE_CXX_STANDARD 20)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

add_library(${PROJECT_NAME} SHARED
    jni_bridge.cpp
)

find_library(log-lib log)
find_library(android-lib android)
find_library(gles3-lib GLESv3)
find_library(egl-lib EGL)

target_link_libraries(${PROJECT_NAME}
    ${log-lib}
    ${android-lib}
    ${gles3-lib}
    ${egl-lib}
)
