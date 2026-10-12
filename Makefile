# Tryvoha Local: build, install, launch.
.DEFAULT_GOAL := all

ADB ?= adb
SERIAL ?=
ADB_FLAGS := $(if $(strip $(SERIAL)),-s $(strip $(SERIAL)),)
APK := app/build/outputs/apk/debug/app-debug.apk
PACKAGE := ua.schaslyve.tryvoha
ACTIVITY := .MainActivity

.PHONY: all build install launch run devices check-device test clean help

# The default target runs sequentially, including with `make -j`.
all:
	@$(MAKE) --no-print-directory build
	@$(MAKE) --no-print-directory install
	@$(MAKE) --no-print-directory launch

build:
	./build-apk.sh

check-device:
	@command -v $(firstword $(ADB)) >/dev/null 2>&1 || { \
		echo 'ERROR: adb not found on PATH. Install android-tools.' >&2; exit 1; }
	@$(ADB) $(ADB_FLAGS) get-state 2>/dev/null | grep -qx device || { \
		echo 'ERROR: no authorized device (or multiple devices).' >&2; \
		echo 'Check: make devices; select one: make SERIAL=<serial>' >&2; exit 1; }

install: check-device
	@test -s "$(APK)" || { echo 'ERROR: APK missing. Run make build.' >&2; exit 1; }
	$(ADB) $(ADB_FLAGS) install -r "$(APK)"

launch: check-device
	$(ADB) $(ADB_FLAGS) shell am start -n "$(PACKAGE)/$(ACTIVITY)"

run: launch

devices:
	$(ADB) devices -l

test:
	./test-matcher.sh

clean:
	./gradlew clean

help:
	@printf '%s\n' \
	  'make                  Build, install and launch on Android' \
	  'make build            Build debug APK (also copies it to ~/Downloads)' \
	  'make install          Install the existing APK (no rebuild)' \
	  'make launch           Launch the installed app (no rebuild)' \
	  'make run              Alias for make launch' \
	  'make devices          List attached Android devices' \
	  'make SERIAL=<serial>   Build, install and launch on a selected device' \
	  'make test             Run pure Java logic tests' \
	  'make clean            Clean Gradle build outputs'
