JAVA_SOURCES := $(shell find src -name '*.java')
TEST_SOURCES := $(shell find tests -name '*.java')
BUILD_DIR := build
MAIN_CLASS := controller.Main
SMOKE_TIMEOUT_SEC := 5
SPRITE_SCRIPT := scripts/prepare-car-sprites.sh
KENNEY_SCRIPT := scripts/prepare-kenney-sprites.sh
TRACK_PREVIEW_SCRIPT := scripts/generate-track-previews.sh
CAR_SHEET := src/sprites/cars.png
KENNEY_ZIP := third_party/kenney-top-down-tanks-redux/kenney_topdownTanksRedux.zip
KENNEY_LICENSE := third_party/kenney-top-down-tanks-redux/License.txt
CONFIG_FILES := $(wildcard src/data/config/*.properties)
JUNIT_VERSION := 1.10.2
JUNIT_JAR := $(BUILD_DIR)/lib/junit-platform-console-standalone-$(JUNIT_VERSION).jar
JUNIT_URL := https://repo1.maven.org/maven2/org/junit/platform/junit-platform-console-standalone/$(JUNIT_VERSION)/junit-platform-console-standalone-$(JUNIT_VERSION).jar

.PHONY: all compile run test smoke-test demo-race record-demo clean help prepare-sprites prepare-kenney-sprites

all: compile

help:
	@echo "Super Sprint Supelec — make targets:"
	@echo "  make compile     Compile Java sources into $(BUILD_DIR)/"
	@echo "  make run         Compile (if needed) and launch the game"
	@echo "  make test        Run the JUnit test suite in tests/"
	@echo "  make smoke-test  Headless launch test for CI"
	@echo "  make demo-race   All-AI Dune Horseshoe exhibition race"
	@echo "                   Optional: LAPS=3 DEMO_CARS=identical:0"
	@echo "  make record-demo Record demo race MP4 (needs DISPLAY/ffmpeg)"
	@echo "                   Optional: DEMO_CARS=0,0,0,0 DEMO_MP4=..."
	@echo "  make clean       Remove build artifacts"
	@echo "  make help        Show this message"

$(BUILD_DIR)/sprites/.sprites-stamp: $(SPRITE_SCRIPT) $(CAR_SHEET)
	@bash $(SPRITE_SCRIPT) $(BUILD_DIR)

prepare-sprites: $(BUILD_DIR)/sprites/.sprites-stamp

$(BUILD_DIR)/sprites/.kenney-stamp: $(KENNEY_SCRIPT) $(KENNEY_ZIP) $(KENNEY_LICENSE)
	@bash $(KENNEY_SCRIPT) $(BUILD_DIR)

prepare-kenney-sprites: $(BUILD_DIR)/sprites/.kenney-stamp

$(BUILD_DIR)/sprites/.track-previews-stamp: $(TRACK_PREVIEW_SCRIPT) $(BUILD_DIR)/.stamp
	@bash $(TRACK_PREVIEW_SCRIPT) $(BUILD_DIR)
	@touch $(BUILD_DIR)/sprites/.track-previews-stamp

prepare-track-previews: $(BUILD_DIR)/sprites/.track-previews-stamp

$(BUILD_DIR)/config/.stamp: $(CONFIG_FILES)
	@mkdir -p $(BUILD_DIR)/config
	@cp src/data/config/*.properties $(BUILD_DIR)/config/
	@touch $(BUILD_DIR)/config/.stamp

$(BUILD_DIR)/.stamp: $(JAVA_SOURCES) $(CONFIG_FILES) $(BUILD_DIR)/sprites/.sprites-stamp $(BUILD_DIR)/sprites/.kenney-stamp $(BUILD_DIR)/config/.stamp
	@mkdir -p $(BUILD_DIR)
	@find src -name '*.java' > $(BUILD_DIR)/sources.txt
	javac -d $(BUILD_DIR) -sourcepath src @$(BUILD_DIR)/sources.txt
	@bash $(TRACK_PREVIEW_SCRIPT) $(BUILD_DIR)
	@touch $(BUILD_DIR)/.stamp

compile: $(BUILD_DIR)/.stamp

run: compile
	java -cp $(BUILD_DIR) $(MAIN_CLASS)

$(JUNIT_JAR):
	@mkdir -p $(BUILD_DIR)/lib
	curl -fsSL -o $(JUNIT_JAR) $(JUNIT_URL)

$(BUILD_DIR)/tests/.stamp: $(BUILD_DIR)/.stamp $(TEST_SOURCES) $(JUNIT_JAR)
	@mkdir -p $(BUILD_DIR)/tests
	javac -d $(BUILD_DIR)/tests -cp $(BUILD_DIR):$(JUNIT_JAR) $(TEST_SOURCES)
	@touch $(BUILD_DIR)/tests/.stamp

test: $(BUILD_DIR)/tests/.stamp
	java -jar $(JUNIT_JAR) execute \
		--class-path $(BUILD_DIR):$(BUILD_DIR)/tests \
		--scan-class-path \
		--fail-if-no-tests \
		--disable-banner

smoke-test: compile
	@command -v xvfb-run >/dev/null 2>&1 || { echo "xvfb-run is required for smoke-test"; exit 1; }
	@xvfb-run -a timeout $(SMOKE_TIMEOUT_SEC)s java -cp $(BUILD_DIR) $(MAIN_CLASS) || test $$? -eq 124
	@echo "Smoke test passed (process started successfully)"

# All-AI Dune Horseshoe exhibition (no recording).
# Optional: LAPS=1 DEMO_CARS=identical:0 make demo-race
# DEMO_CARS accepts 0,1,2,3 | identical | identical:N
demo-race: compile
	@if [ -n "$(DEMO_CARS)" ]; then \
		java -cp $(BUILD_DIR) view.DemoRaceCapture $(or $(LAPS),3) "$(DEMO_CARS)"; \
	else \
		java -cp $(BUILD_DIR) view.DemoRaceCapture $(or $(LAPS),3); \
	fi

# Record the exhibition race to artifacts/demo/ (requires DISPLAY + ffmpeg + xdotool)
# Optional: DEMO_CARS=identical:0 LAPS=3 make record-demo
record-demo: compile
	@bash scripts/record-demo-race.sh $(or $(DEMO_MP4),artifacts/demo/dune-horseshoe-ai-demo.mp4) $(or $(LAPS),3)

clean:
	rm -rf $(BUILD_DIR) bin
