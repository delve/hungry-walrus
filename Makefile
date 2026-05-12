APK := app/build/outputs/apk/debug/app-debug.apk
SHELL := /bin/bash

.PHONY: debug proj_instructions orchestrate orchestrate-reset

debug:
	@echo "Looking for connected physical device..."
	@DEVICE=$$(adb devices | grep -v 'List of devices' | grep -v 'emulator' | awk '{print $$1}' | head -n1); \
	if [ -z "$$DEVICE" ]; then \
		echo "Error: No physical device found. Check USB connection and debugging is enabled."; \
		exit 1; \
	fi; \
	echo "Found device: $$DEVICE"; \
	if [ ! -f "$(APK)" ]; then \
		echo "Error: APK not found at $(APK). Build the project first."; \
		exit 1; \
	fi; \
	echo "Installing $(APK)..."; \
	adb -s $$DEVICE install -r $(APK); \
	echo "Done."

proj_instructions:
	. scripts/proj_instructions.sh

# Hungry Walrus orchestration targets.
# Usage:
#   make orchestrate            # resume from state, or fresh from layer 1
#   make orchestrate LAYER=2    # clean start at layer 2
#   make orchestrate-reset      # delete orchestrator state
#
# Requires:
#   HUNGRY_WALRUS_DISCORD_WEBHOOK environment variable

ORCHESTRATOR := scripts/orchestrator.py
PYTHON ?= python3

orchestrate:
	@if [ -z "$$HUNGRY_WALRUS_DISCORD_WEBHOOK" ]; then \
		echo "ERROR: set HUNGRY_WALRUS_DISCORD_WEBHOOK"; exit 2; \
	fi
ifdef LAYER
	$(PYTHON) $(ORCHESTRATOR) --layer $(LAYER)
else
	$(PYTHON) $(ORCHESTRATOR)
endif

orchestrate-reset:
	rm -rf .orchestrator/state.json .orchestrator/log.txt
	@echo "Orchestrator state cleared."