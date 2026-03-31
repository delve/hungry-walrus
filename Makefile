APK := app/build/outputs/apk/debug/app-debug.apk

.PHONY: debug

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
