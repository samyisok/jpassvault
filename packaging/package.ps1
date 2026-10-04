# Build a portable Windows application image (runs with no JRE installed on the
# target machine): (1) build the shaded jar if missing, (2) assemble the
# jpackage input, (3) run jpackage --type app-image, (4) create the archive.
# Mirrors packaging/package.sh step for step (design D2/D3).
$ErrorActionPreference = 'Stop'
Set-Location (Join-Path $PSScriptRoot '..')

$AppName     = 'jpassvaultclient'
$MainClass   = 'com.samyisok.jpassvaultclient.JpassvaultclientApplication'
# Explicit jlink module set for a classpath JavaFX app (design D3); JavaFX
# natives ship inside the shaded jar and load from the classpath.
$Modules = 'java.base,java.desktop,java.logging,java.naming,java.net.http,java.prefs,java.scripting,java.xml,jdk.crypto.ec,jdk.unsupported,jdk.zipfs'

# jpackage replaces jlink's default options when --jlink-options is given, so
# restate them. --ignore-modified-runtime downgrades jlink's integrity error on
# distro-patched JDKs; probe first, keep it only where jlink supports it.
$JlinkOpts = '--strip-native-commands --strip-debug --no-man-pages --no-header-files'
$JavaHome = $env:JAVA_HOME
if (-not $JavaHome) {
  $jpackageCmd = Get-Command jpackage -ErrorAction SilentlyContinue
  if ($jpackageCmd) { $JavaHome = Split-Path (Split-Path $jpackageCmd.Source -Parent) -Parent }
}
if (-not $JavaHome) { throw 'JAVA_HOME is not set and jpackage is not on PATH' }
$jlink = Join-Path $JavaHome 'bin/jlink.exe'
if (-not (Test-Path $jlink)) { $jlink = 'jlink' }
& $jlink --ignore-modified-runtime --version 2>$null | Out-Null
if ($LASTEXITCODE -eq 0) { $JlinkOpts += ' --ignore-modified-runtime' }

# Version source of truth (design D3): JPASSVAULT_VERSION when set (CI passes
# the release tag), otherwise the pom's <version>.
$pomVersion = ([regex] '<version>([^<]+)</version>').Match((Get-Content pom.xml -Raw)).Groups[1].Value
$version = if ($env:JPASSVAULT_VERSION) { $env:JPASSVAULT_VERSION } else { $pomVersion }

# Exact pom-versioned path selects the shaded jar; the leftover
# original-jpassvaultclient-*.jar from maven-shade carries the `original-`
# prefix and can never match this path.
$jar = "target/$AppName-$pomVersion.jar"
if (-not (Test-Path $jar)) {
  & ./mvnw.cmd -B package
  if ($LASTEXITCODE -ne 0) { throw "mvnw package failed: $LASTEXITCODE" }
}
if (-not (Test-Path $jar)) { throw "shaded jar not found: $jar" }

Write-Host "Packaging $AppName $version from $jar"
Remove-Item -Recurse -Force -ErrorAction Ignore target/jpackage-input, target/app-image
New-Item -ItemType Directory -Force target/jpackage-input | Out-Null
Copy-Item $jar "target/jpackage-input/$AppName.jar"

& jpackage `
  --type app-image `
  --name $AppName `
  --app-version $version `
  --input target/jpackage-input `
  --main-jar "$AppName.jar" `
  --main-class $MainClass `
  --add-modules $Modules `
  --jlink-options $JlinkOpts `
  --dest target/app-image
if ($LASTEXITCODE -ne 0) { throw "jpackage failed: $LASTEXITCODE" }

# Distro-patched JDKs (Fedora) rewrite conf/security/java.security to include
# crypto-policy files that jlink does not copy into the image; swap in the
# stock java.security.upstream where the packaging JDK ships it (no-op on
# Temurin). Windows path layout: <image>/runtime/conf/security.
$jdkSecurity = Join-Path $JavaHome 'conf/security/java.security.upstream'
$imageSecurity = "target/app-image/$AppName/runtime/conf/security/java.security"
if ((Test-Path $jdkSecurity) -and (Test-Path $imageSecurity)) {
  Write-Host 'Replacing distro-patched java.security with java.security.upstream'
  Copy-Item $jdkSecurity $imageSecurity -Force
}

New-Item -ItemType Directory -Force dist | Out-Null
$zip = "dist/$AppName-$version-windows.zip"
Compress-Archive -Path "target/app-image/$AppName" -DestinationPath $zip -Force
Write-Host "Created $zip"
