# ncat - Netcat for Android
This is a wrapper around the `ncat` binary cross-compiled for Android.

## History
Version 1 of the application was a custom implementation in Kotlin of commands to open UDP and TCP
sockets. However, after developing (Nmap for Android)[https://github.com/ruvolof/anmap-wrapper], it
became clear that the same approach would work better.

## How to cross-compile Ncat

Be aware that the script to compile Ncat is based on my development environment. 
You might need to fix some paths for it to be working on your system.

```
cd app/src/main/cpp
./make_ncat.sh
```

The script will do the following:

1) Download latest stable Nmap source (7.95 at the time of writing).
2) Configure and compile Ncat for `armeabi-v7a` and `arm64-v8a`.