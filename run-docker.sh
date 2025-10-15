#!/bin/bash

RETROARCH_LOGS_DIR="${1:-~/.config/retroarch/logs}"

docker run --rm --name retro2mqtt_app --network host \
  -v "${RETROARCH_LOGS_DIR}:/app/retroarch/logs:ro" \
  retro2mqtt
