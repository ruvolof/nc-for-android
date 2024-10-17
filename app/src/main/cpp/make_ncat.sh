#!/usr/bin/env bash

set -u

readonly SCRIPT_DIR=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" &> /dev/null \
                      && pwd )

readonly NMAP_VERSION='7.95'
readonly NMAP_SRC="nmap-${NMAP_VERSION}.tgz"
readonly NMAP_DOWNLOAD_URL="https://nmap.org/dist/${NMAP_SRC}"
readonly NMAP_BUILD_DIR="nmap-${NMAP_VERSION}"
readonly OPENSSL_VERSION='3.0.15'
readonly OPENSSL_SRC="openssl-${OPENSSL_VERSION}.tar.gz"
https://github.com/openssl/openssl/releases/download/openssl-3.0.15/openssl-3.0.15.tar.gz
readonly OPENSSL_DOWNLOAD_URL="https://github.com/openssl/openssl/releases/download/openssl-${OPENSSL_VERSION}/${OPENSSL_SRC}"
readonly OPENSSL_BUILD_DIR="${SCRIPT_DIR}/openssl-${OPENSSL_VERSION}"
readonly ANDROID_NDK_ROOT="$(ls -dr /home/${USER}/Android/Sdk/ndk/* | head -1)"
readonly HOST_ARCH='linux-x86_64'

declare -A ANDROID_TARGETS_ABI=(['aarch64-linux-android']='arm64-v8a' \
                                ['armv7a-linux-androideabi']='armeabi-v7a')
                                #['i686-linux-android']='x86' \
                                #['x86_64-linux-android']='x86_64')

# Exports variables needed to cross-compile for Android.
# Args:
#   $1 Target (from ANDROID_TARGETS)
function export_make_toolchain() {
  export TARGET="$1"
  export TOOLCHAIN="${ANDROID_NDK_ROOT}/toolchains/llvm/prebuilt/${HOST_ARCH}"
  export API=33
  export AR="${TOOLCHAIN}/bin/llvm-ar"
  export CC="${TOOLCHAIN}/bin/${TARGET}${API}-clang"
  export AS="${CC}"
  export CXX="${TOOLCHAIN}/bin/${TARGET}${API}-clang++"
  export LD="${TOOLCHAIN}/bin/ld"
  export RANLIB="${TOOLCHAIN}/bin/llvm-ranlib"
  export STRIP="${TOOLCHAIN}/bin/llvm-strip"
}

# Initializes the folder structure for libraries.
function create_lib_folders() {
  rm -rf libs
  for target in "${!ANDROID_TARGETS_ABI[@]}"; do
    mkdir -p "libs/${ANDROID_TARGETS_ABI[$target]}"
  done
}

# Extracts Nmap source. Removes it before, if it already exists.
function prepare_nmap_source() {
  if ! [[ -f "${NMAP_SRC}" ]]; then
    wget "${NMAP_DOWNLOAD_URL}" -O "${NMAP_SRC}"
  fi
  rm -rf "${NMAP_BUILD_DIR}"
  tar -xzf "${NMAP_SRC}"
}

# Extracts openssl source. Removes it before, if it already exists.
function prepare_openssl_source() {
  if ! [[ -f "${OPENSSL_SRC}" ]]; then
    wget "${OPENSSL_DOWNLOAD_URL}" -O "${OPENSSL_SRC}"
  fi
  rm -rf "${OPENSSL_BUILD_DIR}"
  tar -xzf "${OPENSSL_SRC}"
}

# Function to patch sockaddr_u.h.
# The patch file takes care of missing SUN_LEN macro.
function patch_source() {
  patch "${NMAP_BUILD_DIR}/ncat/sockaddr_u.h" < sockaddr_u.h.patch
}

# Cross-compiles openssl for a specified android target.
# Args:
#   $1 Target (from ANDROID_TARGETS)
function cross_compile_openssl() {
  target="$1"
  if [[ "${target}" == 'aarch64-linux-android' ]]; then
    ./Configure android-arm64
  elif [[ "${target}" == 'armv7a-linux-androideabi' ]]; then
    ./Configure android-arm
  elif [[ "${target}" == 'i686-linux-android' ]]; then
      ./Configure android-x86
  elif [[ "${target}" == 'x86_64-linux-android' ]]; then
      ./Configure android-x86_64
  fi
  make
}

# This function creates the folder OPENSSL_BUILD_DIR/lib, then copies
# needed openssl libraries in order to be properly included in nmap build.
function setup_openssl_dir_for_ncat_build() {
  local DEPS=('libcrypto.a'
              'libssl.a')
  mkdir lib
  for dep in "${DEPS[@]}"; do
      cp "${dep}" lib/
  done
}

# Cross-compiles nmap for a specified android target.
# Args:
#   $1 Target (from ANDROID_TARGETS)
function cross_compile_ncat() {
  export_make_toolchain "$1"
  ./configure --host "${TARGET}" \
              --without-nping \
              --without-zenmap \
              --without-ndiff \
              --with-openssl="${OPENSSL_BUILD_DIR}" \
              --with-libpcap=included \
              --with-liblua=included
  make build-ncat
  cp ncat/ncat "../libs/${ANDROID_TARGETS_ABI[$TARGET]}/libncat.so"
}

function main() {
  create_lib_folders
  for target in "${!ANDROID_TARGETS_ABI[@]}"; do
    export ANDROID_NDK_ROOT
    (
      prepare_openssl_source
      cd "${OPENSSL_BUILD_DIR}" || exit
      PATH="${ANDROID_NDK_ROOT}/toolchains/llvm/prebuilt/linux-x86_64/bin:${PATH}"
      cross_compile_openssl "${target}"
      setup_openssl_dir_for_ncat_build
    )
    (
      prepare_nmap_source
      patch_source
      cd "${NMAP_BUILD_DIR}" || exit
      cross_compile_ncat "${target}"
    )
  done
}

main "$@"
