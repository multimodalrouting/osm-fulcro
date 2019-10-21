FROM beevelop/cordova:latest

ENV ANDROID_HOME=/opt/android

RUN /opt/android/tools/bin/sdkmanager --update

RUN mkdir "$ANDROID_HOME/licenses"

RUN echo '8933bad161af4178b1185d1a37fbf41ea5269c55\n\
d56f5187479451eabf01fb78af6dfcb131a6481e\n\
24333f8a63b6825ea9c5514f83c2829b004d1fee\n'\
 > "$ANDROID_HOME/licenses/android-sdk-license"

RUN echo "84831b9409646a918e30573bab4c9c91346d8abd" > "$ANDROID_HOME/licenses/android-sdk-preview-license"


ENV CLOJURE_VERSION=1.10.0.442 \
    LANG=en_US.UTF-8 \
    LC_ALL=C.UTF-8

RUN set -ex; \
    wget "https://download.clojure.org/install/linux-install-$CLOJURE_VERSION.sh"; \
    chmod +x "linux-install-$CLOJURE_VERSION.sh"; \
    "./linux-install-$CLOJURE_VERSION.sh"; \
    rm -rf "linux-install-$CLOJURE_VERSION.sh"


WORKDIR /osm-fulcro

VOLUME /osm-fulcro/app/cordova/platforms/android/app/build/outputs/

CMD git clone -b cordova https://github.com/bastiion/osm-fulcro app && cd app ;  npm install && npm run cordova/android-all
