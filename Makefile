# ==============================================================================
# Makefile - Android APK Automation for LOKER
# ==============================================================================

SHELL := /bin/bash
OUTPUT_DIR ?= dist
BUILD_TYPE ?= debug

.PHONY: all build debug release clean help check

# Default target
all: build

# Build target (uses BUILD_TYPE, defaults to debug)
build:
	@chmod +x ./build-apk.sh
	@./build-apk.sh --type $(BUILD_TYPE) --output $(OUTPUT_DIR)

# Debug build shortcut
debug:
	@chmod +x ./build-apk.sh
	@./build-apk.sh --type debug --output $(OUTPUT_DIR)

# Release build shortcut
release:
	@chmod +x ./build-apk.sh
	@./build-apk.sh --type release --output $(OUTPUT_DIR)

# Environment check only
check:
	@chmod +x ./build-apk.sh
	@bash -c "which java >/dev/null && echo '✓ Java installed' || echo '✗ Java missing'"
	@bash -c "which gradle >/dev/null && echo '✓ Gradle installed' || echo '✗ Gradle missing'"
	@bash -c "[ -d \"$${ANDROID_SDK_ROOT:-$${ANDROID_HOME:-/opt/android/sdk}}\" ] && echo '✓ Android SDK found' || echo '✗ Android SDK missing'"

# Clean artifacts
clean:
	@gradle clean || true
	@rm -rf $(OUTPUT_DIR)
	@echo "Cleaned build artifacts and $(OUTPUT_DIR)/"

# Help target
help:
	@echo "LOKER Build Automation Makefile"
	@echo "-------------------------------"
	@echo "Targets:"
	@echo "  make build [OUTPUT_DIR=path] [BUILD_TYPE=debug|release]  - Build APK to specified directory"
	@echo "  make debug [OUTPUT_DIR=path]                             - Build debug APK (default: dist)"
	@echo "  make release [OUTPUT_DIR=path]                           - Build release APK"
	@echo "  make check                                               - Verify toolchain requirements"
	@echo "  make clean                                               - Clean Gradle build outputs & dist"
	@echo "  make help                                                - Show this help message"
