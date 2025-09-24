# syntax=docker/dockerfile:1
# Use a pre-built React Native Android image

FROM reactnativecommunity/react-native-android:latest AS base

# Set environment variables
ENV ANDROID_SDK_ROOT=/opt/android-sdk \
    ANDROID_HOME=/opt/android-sdk \
    JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 \
    GRADLE_USER_HOME=/opt/gradle

# Set workdir and copy minimal files first to leverage Docker cache
WORKDIR /app
COPY package.json package-lock.json* yarn.lock* ./

# Install JS deps
RUN npm ci --no-audit --no-fund || npm install --no-audit --no-fund

# Copy the rest of the project
COPY . .

# Pre-download Gradle wrapper and dependencies (improves first build time)
RUN chmod +x android/gradlew && cd android && ./gradlew --no-daemon tasks || true

# Expose Metro bundler port
EXPOSE 8081

# Default command: start Metro bundler
CMD ["npm","run","start"]
