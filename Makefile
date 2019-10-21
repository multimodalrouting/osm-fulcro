test:
	npm install
	npx shadow-cljs compile ci-tests
	npx karma start --single-run
	clj -A:dev:clj-tests

android:
	npm install
	npm run cordova/android-all

.PHONY: test
