# Thin convenience wrappers around the Gradle build.
# Prefer ./gradlew directly; these targets exist for muscle memory.

.PHONY: all compile run test smoke-test demo-race record-demo package package-appimage clean help

GRADLEW := ./gradlew
APP_VERSION ?= 0.0.0-dev

all: compile

help:
	@echo "Super Sprint Supelec — Make wrappers around Gradle:"
	@echo "  make compile           ./gradlew classes"
	@echo "  make run               ./gradlew run"
	@echo "  make test              ./gradlew test"
	@echo "  make smoke-test        ./gradlew smokeTest"
	@echo "  make demo-race         ./gradlew demoRace -PTRACK=... -PCARS=..."
	@echo "  make record-demo       ./gradlew recordDemo (needs DISPLAY/ffmpeg)"
	@echo "  make package           ./gradlew packageRelease"
	@echo "  make package-appimage  ./gradlew packageRelease -PappImage=true"
	@echo "  make clean             ./gradlew clean"
	@echo "  make help              Show this message"
	@echo ""
	@echo "Primary interface: ./gradlew <task>  (see README / docs/BUILD.md)"

compile:
	$(GRADLEW) classes

run:
	$(GRADLEW) run

test:
	$(GRADLEW) test

smoke-test:
	$(GRADLEW) smokeTest

demo-race:
	@if [ -z "$(TRACK)" ] || [ -z "$(CARS)" ]; then \
		echo "Usage: make demo-race TRACK=<trackId> CARS=<carIds> [LAPS=3]"; \
		exit 1; \
	fi
	$(GRADLEW) demoRace -PTRACK=$(TRACK) -PCARS="$(CARS)" -PLAPS=$(or $(LAPS),3)

record-demo:
	@if [ -z "$(TRACK)" ] || [ -z "$(CARS)" ]; then \
		echo "Usage: make record-demo TRACK=<trackId> CARS=<carIds> [LAPS=3] [DEMO_MP4=...]"; \
		exit 1; \
	fi
	$(GRADLEW) recordDemo \
		-PTRACK=$(TRACK) \
		-PCARS="$(CARS)" \
		-PLAPS=$(or $(LAPS),3) \
		$(if $(DEMO_MP4),-PDEMO_MP4=$(DEMO_MP4),)

package:
	$(GRADLEW) packageRelease -PappVersion=$(APP_VERSION)

package-appimage:
	$(GRADLEW) packageRelease -PappVersion=$(APP_VERSION) -PappImage=true

clean:
	$(GRADLEW) clean
	rm -rf artifacts/release
