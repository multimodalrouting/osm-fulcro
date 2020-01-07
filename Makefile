.DEFAULT_GOAL := test
MAKEFLAGS += --warn-undefined-variables --no-print-directory
SHELL := /bin/bash

DOCKER_ENV ?= DOCKER_BUILDKIT=1 DOCKER_CLI_EXPERIMENTAL=enabled
SUDO ?= $(shell if ! groups | grep -q docker; then echo sudo -E; fi)

.PHONY: test
test:
	npm install
	npx shadow-cljs compile ci-tests
	npx karma start --single-run
	clj -A:dev:clj-tests

.PHONY: android
android:
	npm install
	npm run cordova/android-all

.PHONY: docker
docker:
	$(SUDO) $(DOCKER_ENV) docker build . -t osm-fulcro

.PHONY: docker-android
docker-android:
	$(SUDO) $(DOCKER_ENV) docker build . -t osm-fulcro-android
	$(SUDO) $(DOCKER_ENV) docker run -v $$PWD/cordova/platforms/android/app/build/outputs/outputs/apk/debug:/host osm-fulcro-android cp /osm-fulcro/cordova/platforms/android/app/build/outputs/apk/debug/app-debug.apk /host
