JAVA_SOURCES := $(shell find src -name '*.java')
BUILD_DIR := build
MAIN_CLASS := controller.Main
SMOKE_TIMEOUT_SEC := 5
SPRITE_SCRIPT := scripts/prepare-car-sprites.sh
BUNDLED_CAR_SPRITES := src/sprites/car1.png src/sprites/car2.png src/sprites/car3.png src/sprites/car4.png
CONFIG_FILES := $(wildcard src/data/config/*.properties)

.PHONY: all compile run smoke-test clean help prepare-sprites

all: compile

help:
	@echo "Super Sprint Supelec — make targets:"
	@echo "  make compile     Compile Java sources into $(BUILD_DIR)/"
	@echo "  make run         Compile (if needed) and launch the game"
	@echo "  make smoke-test  Headless launch test for CI"
	@echo "  make clean       Remove build artifacts"
	@echo "  make help        Show this message"

$(BUILD_DIR)/sprites/.sprites-stamp: $(SPRITE_SCRIPT) $(BUNDLED_CAR_SPRITES)
	@bash $(SPRITE_SCRIPT) $(BUILD_DIR)

prepare-sprites: $(BUILD_DIR)/sprites/.sprites-stamp

$(BUILD_DIR)/config/.stamp: $(CONFIG_FILES)
	@mkdir -p $(BUILD_DIR)/config
	@cp src/data/config/*.properties $(BUILD_DIR)/config/
	@touch $(BUILD_DIR)/config/.stamp

$(BUILD_DIR)/.stamp: $(JAVA_SOURCES) $(CONFIG_FILES) $(BUILD_DIR)/sprites/.sprites-stamp $(BUILD_DIR)/config/.stamp
	@mkdir -p $(BUILD_DIR)
	@find src -name '*.java' > $(BUILD_DIR)/sources.txt
	javac -d $(BUILD_DIR) -sourcepath src @$(BUILD_DIR)/sources.txt
	@touch $(BUILD_DIR)/.stamp

compile: $(BUILD_DIR)/.stamp

run: compile
	java -cp $(BUILD_DIR) $(MAIN_CLASS)

smoke-test: compile
	@command -v xvfb-run >/dev/null 2>&1 || { echo "xvfb-run is required for smoke-test"; exit 1; }
	@xvfb-run -a timeout $(SMOKE_TIMEOUT_SEC)s java -cp $(BUILD_DIR) $(MAIN_CLASS) || test $$? -eq 124
	@echo "Smoke test passed (process started successfully)"

clean:
	rm -rf $(BUILD_DIR)
