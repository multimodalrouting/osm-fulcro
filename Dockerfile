FROM openjdk:8-stretch as android-sdk

ENV ANDROID_SDK_URL="https://dl.google.com/android/repository/tools_r25.2.5-linux.zip" \
    ANDROID_SDK_ROOT="/opt/android"

WORKDIR $ANDROID_SDK_ROOT

RUN wget -q -O tools.zip $ANDROID_SDK_URL \
  && unzip -q tools.zip \
  && rm tools.zip

SHELL ["/bin/bash", "-o", "pipefail", "-c"]

ENV ANDROID_APIS=android-28 \
    ANDROID_BUILD_TOOLS_VERSION=28.0.3 \
    PATH=$PATH:$ANDROID_SDK_ROOT/tools:$ANDROID_SDK_ROOT/platform-tools:$ANDROID_SDK_ROOT/build-tools/$ANDROID_BUILD_TOOLS_VERSION

RUN chmod a+x -R $ANDROID_SDK_ROOT \
  && echo y | android update sdk -a -u -t platform-tools,${ANDROID_APIS},build-tools-${ANDROID_BUILD_TOOLS_VERSION}

#------------#

FROM debian:stretch as gradle

RUN apt-get update -q \
  && apt-get install -qy --no-install-recommends ca-certificates unzip wget \
  && rm -rf /var/lib/apt/lists/*

ENV GRADLE_VERSION=4.10.3 \
    GRADLE_HOME=/opt/gradle

WORKDIR /opt

RUN wget -q https://services.gradle.org/distributions/gradle-$GRADLE_VERSION-all.zip \
  && unzip -q gradle-$GRADLE_VERSION-all.zip \
  && mv gradle-$GRADLE_VERSION $GRADLE_HOME \
  && rm gradle-$GRADLE_VERSION-all.zip

#------------#

FROM clojure:openjdk-8-tools-deps-stretch as base

ENV NODE_VERSION=12 \
    NODE_ENV=development

SHELL ["/bin/bash", "-o", "pipefail", "-c"]

RUN wget -qO- https://deb.nodesource.com/setup_$NODE_VERSION.x | bash - \
  && apt-get install -qy --no-install-recommends nodejs \ 
  && rm -rf /var/lib/apt/lists/*

WORKDIR /osm-fulcro/

COPY [ "package.json", "/osm-fulcro/" ]

RUN npm install

COPY [ "deps.edn", "shadow-cljs.edn", "/osm-fulcro/" ]

RUN npx shadow-cljs npm-deps

COPY [ "src/", "/osm-fulcro/src/" ]

RUN npm run cordova/build-main-dev

COPY [ "cordova/", "/osm-fulcro/cordova/" ]

RUN npm run cordova/build-assets

COPY [ "resources/", "/osm-fulcro/resources/" ]

RUN npm run cordova/prepare-dev

COPY [ ".", "/osm-fulcro/" ]

COPY --from=android-sdk [ "/opt/android", "/opt/android" ]
COPY --from=gradle [ "/opt/gradle", "/opt/gradle" ]

ENV ANDROID_SDK_ROOT=/opt/android \
    GRADLE_HOME=/opt/gradle

ENV PATH=$PATH:$ANDROID_SDK_ROOT/tools:$ANDROID_SDK_ROOT/platform-tools:$ANDROID_SDK_ROOT/build-tools/$ANDROID_BUILD_TOOLS_VERSION:$GRADLE_HOME/bin

RUN mkdir $ANDROID_SDK_ROOT/licenses \
 && printf "8933bad161af4178b1185d1a37fbf41ea5269c55\n\
d56f5187479451eabf01fb78af6dfcb131a6481e\n\
24333f8a63b6825ea9c5514f83c2829b004d1fee\n" > "$ANDROID_SDK_ROOT/licenses/android-sdk-license" \
  && echo "84831b9409646a918e30573bab4c9c91346d8abd" > "$ANDROID_SDK_ROOT/licenses/android-sdk-preview-license"

WORKDIR /osm-fulcro/

RUN npm run cordova/prepare-android \
  && npm run cordova/build-android

VOLUME /osm-fulcro/cordova/platforms/android/app/build/outputs/

CMD [ "bash" ]
