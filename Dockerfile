# syntax=docker/dockerfile:1
# Multi-stage to keep image lean

FROM node:18-bullseye AS base

# Install Java (OpenJDK 17) and required tools
RUN apt-get update && DEBIAN_FRONTEND=noninteractive apt-get install -y --no-install-recommends \
    openjdk-17-jdk \
    wget unzip git curl ca-certificates \
    && rm -rf /var/lib/apt/lists/*

ENV ANDROID_SDK_ROOT=/opt/android-sdk \
    ANDROID_HOME=/opt/android-sdk \
    JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 \
    GRADLE_USER_HOME=/opt/gradle

# Install Android SDK commandline tools
RUN mkdir -p $ANDROID_SDK_ROOT/cmdline-tools \
    && cd /tmp \
    && wget -q https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip -O cmdline-tools.zip \
    && mkdir -p $ANDROID_SDK_ROOT/cmdline-tools/tools \
    && unzip -q cmdline-tools.zip -d $ANDROID_SDK_ROOT/cmdline-tools \
    && mv $ANDROID_SDK_ROOT/cmdline-tools/cmdline-tools $ANDROID_SDK_ROOT/cmdline-tools/tools \
    && rm -f cmdline-tools.zip

ENV PATH=$PATH:$ANDROID_SDK_ROOT/cmdline-tools/tools/bin:$ANDROID_SDK_ROOT/platform-tools:$ANDROID_SDK_ROOT/emulator

# Accept licenses and install SDK packages commonly needed for RN Android builds
RUN yes | sdkmanager --licenses \
    && sdkmanager \
      "platform-tools" \
      "platforms;android-34" \
      "build-tools;34.0.0" \
      "cmake;3.22.1" \
      "ndk;25.2.9519653"

# Set workdir and copy minimal files first to leverage Docker cache
WORKDIR /app
COPY package.json package-lock.json* yarn.lock* ./

# Install JS deps
RUN npm ci --no-audit --no-fund || npm install --no-audit --no-fund

# Copy the rest of the project
COPY . .

# Pre-download Gradle wrapper and dependencies (improves first build time)
RUN cd android && ./gradlew --no-daemon tasks || true

# Expose Metro bundler port
EXPOSE 8081

# Default command: start Metro bundler
CMD ["npm","run","start"]
