#!/usr/bin/env bash
# ==============================================================================
# build-apk.sh - Automated Android APK Builder for LOKER
# ==============================================================================
# Detects environment requirements, validates dependencies, compiles the APK,
# and outputs the final artifact to a specified destination directory.
#
# Usage:
#   ./build-apk.sh [OPTIONS] [OUTPUT_DIR]
#
# Options:
#   -o, --output <dir>    Specify destination directory for generated APK
#   -t, --type <build>    Build type: debug (default) or release
#   -h, --help            Show this help message
# ==============================================================================

set -euo pipefail

# Color formatting helpers
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
BOLD='\033[1m'
NC='\033[0m' # No Color

log_info() {
    echo -e "${BLUE}[INFO]${NC} $*"
}

log_step() {
    echo -e "${CYAN}${BOLD}[STEP]${NC} $*"
}

log_success() {
    echo -e "${GREEN}${BOLD}[SUCCESS]${NC} $*"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $*"
}

log_error() {
    echo -e "${RED}${BOLD}[ERROR]${NC} $*" >&2
}

show_help() {
    cat <<EOF
Usage: $(basename "$0") [OPTIONS] [OUTPUT_DIR]

Options:
  -o, --output <dir>    Target output directory for the built APK
                        (Default: dist)
  -t, --type <type>     Build type: 'debug' or 'release'
                        (Default: debug)
  -h, --help            Display this help dialog and exit

Examples:
  ./build-apk.sh
  ./build-apk.sh ./bin
  ./build-apk.sh --type debug --output ./release_output
EOF
}

# Resolve script directory and project root
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$SCRIPT_DIR"
OUTPUT_DIR=""
BUILD_TYPE="debug"

# Parse CLI arguments
while [[ $# -gt 0 ]]; do
    case "$1" in
        -h|--help)
            show_help
            exit 0
            ;;
        -o|--output)
            OUTPUT_DIR="$2"
            shift 2
            ;;
        -t|--type)
            BUILD_TYPE="$2"
            shift 2
            ;;
        *)
            if [[ -z "$OUTPUT_DIR" ]]; then
                OUTPUT_DIR="$1"
                shift
            else
                log_error "Unknown or unexpected argument: $1"
                show_help
                exit 1
            fi
            ;;
    esac
done

if [[ -z "$OUTPUT_DIR" ]]; then
    OUTPUT_DIR="${PROJECT_ROOT}/dist"
fi

cd "$PROJECT_ROOT"

echo -e "${BOLD}======================================================${NC}"
echo -e "${BOLD}       LOKER Android APK Build Automation System      ${NC}"
echo -e "${BOLD}======================================================${NC}"

# ------------------------------------------------------------------------------
# STEP 1: Tech Stack & Project Configuration Detection
# ------------------------------------------------------------------------------
log_step "1. Detecting Project Tech Stack and Specifications..."

BUILD_GRADLE="${PROJECT_ROOT}/app/build.gradle.kts"
VERSIONS_CATALOG="${PROJECT_ROOT}/gradle/libs.versions.toml"

if [[ ! -f "$BUILD_GRADLE" ]]; then
    log_error "Missing app build configuration: $BUILD_GRADLE"
    exit 1
fi

# Detect Target/Compile SDK
COMPILE_SDK=$(grep -oE 'release\([0-9]+\)' "$BUILD_GRADLE" | head -n1 | tr -cd '0-9' || true)
if [[ -z "$COMPILE_SDK" ]]; then
    COMPILE_SDK=$(grep -oE 'compileSdk\s*=\s*[0-9]+' "$BUILD_GRADLE" | head -n1 | tr -cd '0-9' || true)
fi
[[ -z "$COMPILE_SDK" ]] && COMPILE_SDK="36"

MIN_SDK=$(grep -oE 'minSdk\s*=\s*[0-9]+' "$BUILD_GRADLE" | head -n1 | tr -cd '0-9' || true)
[[ -z "$MIN_SDK" ]] && MIN_SDK="24"

# Detect AGP and Kotlin versions if catalog exists
AGP_VERSION="Unknown"
KOTLIN_VERSION="Unknown"
if [[ -f "$VERSIONS_CATALOG" ]]; then
    AGP_VERSION=$(grep -oE 'agp\s*=\s*"[^"]+"' "$VERSIONS_CATALOG" | cut -d'"' -f2 || true)
    KOTLIN_VERSION=$(grep -oE 'kotlin\s*=\s*"[^"]+"' "$VERSIONS_CATALOG" | cut -d'"' -f2 || true)
fi

log_info "Detected Android Gradle Plugin (AGP): ${AGP_VERSION:-N/A}"
log_info "Detected Kotlin version:              ${KOTLIN_VERSION:-N/A}"
log_info "Detected Compile SDK:                 ${COMPILE_SDK}"
log_info "Detected Min SDK:                     ${MIN_SDK}"
log_info "Target Build Variant:                 ${BUILD_TYPE}"
log_info "Target Output Directory:              ${OUTPUT_DIR}"

# ------------------------------------------------------------------------------
# STEP 2: Tooling & Environment Verification (Java, Gradle, Android SDK)
# ------------------------------------------------------------------------------
log_step "2. Checking Required Build Tools and Dependencies..."

# 2.1 Java / JDK check
if command -v java >/dev/null 2>&1; then
    JAVA_CMD="java"
    JAVA_VERSION_RAW=$("$JAVA_CMD" -version 2>&1 | awk -F '"' '/version/ {print $2}' | cut -d'.' -f1)
    # OpenJDK 21 or 17 returns major version as first token
    if [[ "$JAVA_VERSION_RAW" == "1" ]]; then
        JAVA_VERSION_RAW=$("$JAVA_CMD" -version 2>&1 | awk -F '"' '/version/ {print $2}' | cut -d'.' -f2)
    fi
    log_info "Java runtime detected: $(which java) (Major version: ${JAVA_VERSION_RAW:-Unknown})"
    if [[ -n "$JAVA_VERSION_RAW" && "$JAVA_VERSION_RAW" -lt 17 ]]; then
        log_warn "JDK version ($JAVA_VERSION_RAW) is lower than recommended JDK 17+ for AGP 8+/9+."
    fi
else
    log_error "Java runtime (JDK 17+) is not found on PATH."
    echo -e "${YELLOW}Please install OpenJDK 17 or 21:${NC}"
    echo "  - Ubuntu/Debian: sudo apt-get update && sudo apt-get install -y openjdk-17-jdk"
    echo "  - Arch Linux: sudo pacman -S jdk17-openjdk"
    echo "  - macOS: brew install openjdk@17"
    exit 1
fi

# 2.2 Gradle check (Use installed gradle command or gradle wrapper)
GRADLE_CMD=""
if command -v gradle >/dev/null 2>&1; then
    GRADLE_CMD="gradle"
elif [[ -x "./gradlew" ]]; then
    GRADLE_CMD="./gradlew"
elif [[ -f "./gradlew" ]]; then
    chmod +x ./gradlew
    GRADLE_CMD="./gradlew"
else
    log_error "Gradle build tool not found."
    echo -e "${YELLOW}Please install Gradle or configure the Gradle wrapper:${NC}"
    echo "  - Ubuntu/Debian: sudo apt-get install gradle"
    echo "  - Arch Linux: sudo pacman -S gradle"
    echo "  - macOS: brew install gradle"
    exit 1
fi
log_info "Gradle executable detected: $GRADLE_CMD"

# 2.3 Android SDK check
ANDROID_SDK_FOUND=false
if [[ -n "${ANDROID_SDK_ROOT:-}" && -d "$ANDROID_SDK_ROOT" ]]; then
    export ANDROID_HOME="$ANDROID_SDK_ROOT"
    ANDROID_SDK_FOUND=true
    log_info "Using ANDROID_SDK_ROOT: $ANDROID_SDK_ROOT"
elif [[ -n "${ANDROID_HOME:-}" && -d "$ANDROID_HOME" ]]; then
    export ANDROID_SDK_ROOT="$ANDROID_HOME"
    ANDROID_SDK_FOUND=true
    log_info "Using ANDROID_HOME: $ANDROID_HOME"
else
    # Check default fallback standard locations
    COMMON_SDK_PATHS=(
        "/opt/android/sdk"
        "$HOME/Android/Sdk"
        "$HOME/Library/Android/sdk"
        "/usr/lib/android-sdk"
    )
    for path in "${COMMON_SDK_PATHS[@]}"; do
        if [[ -d "$path" ]]; then
            export ANDROID_HOME="$path"
            export ANDROID_SDK_ROOT="$path"
            ANDROID_SDK_FOUND=true
            log_info "Found Android SDK at: $path"
            break
        fi
    done
fi

if [[ "$ANDROID_SDK_FOUND" = false ]]; then
    log_error "Android SDK not found. ANDROID_HOME or ANDROID_SDK_ROOT must be set."
    echo -e "${YELLOW}Please install Android Commandline Tools and SDK:${NC}"
    echo "  1. Download commandlinetools from https://developer.android.com/studio#command-line-tools-only"
    echo "  2. Extract and export ANDROID_HOME=/path/to/android-sdk"
    echo "  3. Accept licenses: yes | sdkmanager --licenses"
    exit 1
fi

# 2.4 Keystore check for Debug builds
if [[ "$BUILD_TYPE" == "debug" ]]; then
    DEBUG_KEYSTORE="${PROJECT_ROOT}/debug.keystore"
    DEBUG_KEYSTORE_B64="${PROJECT_ROOT}/debug.keystore.base64"
    if [[ ! -f "$DEBUG_KEYSTORE" ]]; then
        if [[ -f "$DEBUG_KEYSTORE_B64" ]]; then
            log_info "Restoring debug.keystore from base64 representation..."
            base64 -d "$DEBUG_KEYSTORE_B64" > "$DEBUG_KEYSTORE" 2>/dev/null || \
            base64 -D "$DEBUG_KEYSTORE_B64" > "$DEBUG_KEYSTORE" 2>/dev/null || true
        fi
        if [[ ! -f "$DEBUG_KEYSTORE" ]] && command -v keytool >/dev/null 2>&1; then
            log_info "Generating temporary debug.keystore with keytool..."
            keytool -genkey -v -keystore "$DEBUG_KEYSTORE" -storepass android -alias androiddebugkey -keypass android -keyalg RSA -keysize 2048 -validity 10000 -dname "CN=Android Debug,O=Android,C=US" >/dev/null 2>&1 || true
        fi
    fi
fi

# ------------------------------------------------------------------------------
# STEP 3: Executing Gradle APK Build
# ------------------------------------------------------------------------------
log_step "3. Compiling APK using Gradle..."

GRADLE_TASK=""
EXPECTED_OUTPUT_SUBDIR=""
if [[ "$BUILD_TYPE" == "debug" ]]; then
    GRADLE_TASK=":app:assembleDebug"
    EXPECTED_OUTPUT_SUBDIR="debug"
elif [[ "$BUILD_TYPE" == "release" ]]; then
    GRADLE_TASK=":app:assembleRelease"
    EXPECTED_OUTPUT_SUBDIR="release"
else
    log_error "Unsupported build type '$BUILD_TYPE'. Must be 'debug' or 'release'."
    exit 1
fi

log_info "Executing task: $GRADLE_CMD $GRADLE_TASK"

# Run Gradle build
if ! $GRADLE_CMD "$GRADLE_TASK" --no-daemon; then
    log_error "Gradle build failed for task: $GRADLE_TASK"
    exit 1
fi

# ------------------------------------------------------------------------------
# STEP 4: Locate Built APK and Copy to Destination
# ------------------------------------------------------------------------------
log_step "4. Locating artifact and saving to output directory..."

APK_SOURCE_DIR="${PROJECT_ROOT}/app/build/outputs/apk/${EXPECTED_OUTPUT_SUBDIR}"
BUILT_APK=""

if [[ -d "$APK_SOURCE_DIR" ]]; then
    BUILT_APK=$(find "$APK_SOURCE_DIR" -maxdepth 2 -name "*.apk" -type f | head -n1 || true)
fi

# Fallback search anywhere in build/outputs/apk if not found in expected subdir
if [[ -z "$BUILT_APK" || ! -f "$BUILT_APK" ]]; then
    BUILT_APK=$(find "${PROJECT_ROOT}/app/build/outputs/apk" -name "*.apk" -type f | head -n1 || true)
fi

if [[ -z "$BUILT_APK" || ! -f "$BUILT_APK" ]]; then
    log_error "Build succeeded but could not locate the generated APK in ${PROJECT_ROOT}/app/build/outputs/apk"
    exit 1
fi

log_info "Found built APK: $BUILT_APK"

# Ensure output directory exists
mkdir -p "$OUTPUT_DIR"

APK_FILENAME=$(basename "$BUILT_APK")
FINAL_DESTINATION="${OUTPUT_DIR}/${APK_FILENAME}"

cp -f "$BUILT_APK" "$FINAL_DESTINATION"

# Verify copied artifact
if [[ ! -f "$FINAL_DESTINATION" ]]; then
    log_error "Failed to copy APK to destination: $FINAL_DESTINATION"
    exit 1
fi

FILE_SIZE_BYTES=$(wc -c < "$FINAL_DESTINATION" | tr -d ' ')
FILE_SIZE_MB=$(awk "BEGIN {printf \"%.2f\", $FILE_SIZE_BYTES/1048576}")

# ------------------------------------------------------------------------------
# STEP 5: Verification & Summary
# ------------------------------------------------------------------------------
echo ""
echo -e "${BOLD}======================================================${NC}"
log_success "Build completed successfully!"
echo -e "${BOLD}======================================================${NC}"
echo -e "${BOLD}Artifact Name:${NC}     ${APK_FILENAME}"
echo -e "${BOLD}Artifact Size:${NC}     ${FILE_SIZE_MB} MB (${FILE_SIZE_BYTES} bytes)"
echo -e "${BOLD}Final APK Path:${NC}    ${FINAL_DESTINATION}"
echo -e "${BOLD}======================================================${NC}"
