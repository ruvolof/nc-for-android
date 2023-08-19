# For more information about using CMake with Android Studio, read the
# documentation: https://d.android.com/studio/projects/add-native-code.html

cmake_minimum_required(VERSION 3.18.1)

project("androidnetcat")

include_directories(${CMAKE_CURRENT_SOURCE_DIR}/nmap-7.94/)

# This is a workaround to force Android Studio to package my executable.
# https://stackoverflow.com/questions/75032198/how-to-properly-bundle-an-already-compiled-executable-in-android-app-using-cmake

add_library(
        ncat
        SHARED
        IMPORTED)

set_target_properties(
        ncat
        PROPERTIES IMPORTED_LOCATION
        ${CMAKE_CURRENT_SOURCE_DIR}/libs/${ANDROID_ABI}/libncat.so)

add_executable(
        do_nothing.so
        do_nothing.cpp)

target_link_libraries(
        do_nothing.so
        ncat)