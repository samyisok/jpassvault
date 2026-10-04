#!/usr/bin/env bash
# Build a portable application image for Linux or macOS (runs with no Java
# installed on the target machine): (1) build the shaded jar if missing,
# (2) assemble the jpackage input, (3) run jpackage --type app-image,
# (4) create the archive. Windows uses packaging/package.ps1.
# Avoid GNU-only constructs: runs under bash 3.2 on macOS (design D4).
set -euo pipefail

cd "$(dirname "${BASH_SOURCE[0]}")/.."

APP_NAME=jpassvaultclient
MAIN_CLASS=com.samyisok.jpassvaultclient.JpassvaultclientApplication
# Explicit jlink module set for a classpath JavaFX app (design D3); JavaFX
# natives ship inside the shaded jar and load from the classpath.
MODULES=java.base,java.desktop,java.logging,java.naming,java.net.http,java.prefs,java.scripting,java.xml,jdk.crypto.ec,jdk.unsupported,jdk.zipfs

# Platform layout of the jpackage image (design D3/D5).
case "$(uname -s)" in
  Linux)  platform=linux; image_dir="$APP_NAME";      runtime_rel="lib/runtime" ;;
  Darwin) platform=macos; image_dir="$APP_NAME.app";  runtime_rel="Contents/runtime" ;;
  *)      echo "error: unsupported OS: $(uname -s)" >&2; exit 1 ;;
esac

# jpackage replaces jlink's default options when --jlink-options is given, so
# restate them. --ignore-modified-runtime downgrades jlink's integrity error on
# distro-patched JDKs (Fedora rewrites conf/security/java.security for crypto
# policies); probe first, keep it only where jlink supports it.
JLINK_OPTS="--strip-native-commands --strip-debug --no-man-pages --no-header-files"
if jlink --ignore-modified-runtime --version >/dev/null 2>&1; then
  JLINK_OPTS="$JLINK_OPTS --ignore-modified-runtime"
fi

# Version source of truth (design D3): JPASSVAULT_VERSION when set (CI passes
# the release tag), otherwise the pom's <version>.
pom_version=$(awk -F'[<>]' '/<version>/ { print $3; exit }' pom.xml)
version="${JPASSVAULT_VERSION:-$pom_version}"

# Exact pom-versioned path selects the shaded jar; the leftover
# original-jpassvaultclient-*.jar from maven-shade carries the `original-`
# prefix and can never match this path.
jar="target/${APP_NAME}-${pom_version}.jar"
if [ ! -f "$jar" ]; then
  ./mvnw -B package
fi
if [ ! -f "$jar" ]; then
  echo "error: shaded jar not found: $jar" >&2
  exit 1
fi

echo "Packaging ${APP_NAME} ${version} (${platform}) from ${jar}"
rm -rf target/jpackage-input target/app-image
mkdir -p target/jpackage-input
cp "$jar" "target/jpackage-input/${APP_NAME}.jar"

jpackage \
  --type app-image \
  --name "$APP_NAME" \
  --app-version "$version" \
  --input target/jpackage-input \
  --main-jar "${APP_NAME}.jar" \
  --main-class "$MAIN_CLASS" \
  --add-modules "$MODULES" \
  --jlink-options "$JLINK_OPTS" \
  --dest target/app-image

# Distro-patched JDKs rewrite conf/security/java.security to include
# crypto-policy files that jlink does not copy into the image; the bundled app
# then dies at startup with "Unable to include 'redhat//crypto-policies.properties'".
# Fedora documents java.security.upstream as the stock file for linked images —
# swap it in where the packaging JDK ships it (Temurin: no-op).
jpackage_bin=$(command -v jpackage)
while [ -h "$jpackage_bin" ]; do
  link=$(readlink "$jpackage_bin")
  case "$link" in
    /*) jpackage_bin=$link ;;
    *)  jpackage_bin="$(dirname "$jpackage_bin")/$link" ;;
  esac
done
jdk_security="$(dirname "$(dirname "$jpackage_bin")")/conf/security"
image_security="target/app-image/${image_dir}/${runtime_rel}/conf/security"
if [ -f "$jdk_security/java.security.upstream" ] && [ -f "$image_security/java.security" ]; then
  echo "Replacing distro-patched java.security with java.security.upstream"
  cp "$jdk_security/java.security.upstream" "$image_security/java.security"
fi

mkdir -p dist
archive="dist/${APP_NAME}-${version}-${platform}.tar.gz"
tar -czf "$archive" -C target/app-image "$image_dir"
echo "Created $archive"
