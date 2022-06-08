#!/bin/bash

set -ex

unzip -o apksigner-32.0.0.jar com/android/apksig/internal/apk/v1/V1SchemeSigner.class
java -jar ../../patch/build/libs/dx-patch.jar com/android/apksig/internal/apk/v1/V1SchemeSigner.class com/android/apksig/internal/apk/v1/V1SchemeSigner.class

zip -u apksigner-32.0.0.jar com/android/apksig/internal/apk/v1/V1SchemeSigner.class

zip -d apksigner-32.0.0.jar 'org/conscrypt/*' 'com/android/apksigner/*'

rm -rf com
