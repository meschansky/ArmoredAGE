JAVA_HOME ?= /opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home
ANDROID_HOME ?= /opt/homebrew/share/android-commandlinetools
ANDROID_SDK_ROOT ?= $(ANDROID_HOME)
GRADLE ?= gradle

export JAVA_HOME
export ANDROID_HOME
export ANDROID_SDK_ROOT
export PATH := $(JAVA_HOME)/bin:$(ANDROID_HOME)/cmdline-tools/latest/bin:$(ANDROID_HOME)/platform-tools:$(PATH)

.PHONY: help sdk debug release lint test clean

help:
	@printf "Targets:\n"
	@printf "  make sdk      Install Android API 36 and Build Tools 36.0.0\n"
	@printf "  make debug    Build the debug APK\n"
	@printf "  make release  Build the release APK\n"
	@printf "  make lint     Run Android lint\n"
	@printf "  make test     Run unit tests\n"
	@printf "  make clean    Remove Gradle build outputs\n"

sdk:
	sdkmanager "platforms;android-36" "build-tools;36.0.0"

debug:
	$(GRADLE) :app:assembleDebug

release:
	$(GRADLE) :app:assembleRelease

lint:
	$(GRADLE) :app:lint

test:
	$(GRADLE) :app:testDebugUnitTest

clean:
	$(GRADLE) clean
