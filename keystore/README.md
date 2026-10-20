# Signing keystore

`camara-dev.p12` is a **throwaway development key**. It is committed to this
repository *on purpose*.

## Why commit a keystore?

Android refuses to install an APK as an update unless it is signed with the same
certificate as the installed copy. CI runners start empty and generate a fresh
debug key each time, so every build was signed differently and the app had to be
uninstalled before installing the next build.

Committing one key means **every build — local and CI — is signed identically**, so
installs update in place.

## This key is NOT secret

The password is in `app/build.gradle.kts` (`camara123`) and the file is public.
Anyone can sign an APK that Android will accept as an update to Camara. That is
acceptable for a hobby/test app with no sensitive data. It is **not** acceptable
for anything you distribute seriously.

## Details

| | |
| --- | --- |
| File | `keystore/camara-dev.p12` |
| Type | PKCS#12 |
| Alias | `camara` |
| Store / key password | `camara123` |
| Certificate | self-signed, RSA 2048, valid ~30 years |
| Certificate SHA-256 | `B4:FF:CE:56:D6:57:D7:3F:ED:0E:7B:D0:5B:0D:8A:55:64:D8:AD:6C:43:09:12:B6:16:DC:B6:F7:16:EB:1A:86` |

## Before you publish for real

1. Generate your own keystore and **keep it private** (never commit it):
   ```bash
   keytool -genkeypair -v -keystore camara-release.jks -alias camara \
     -keyalg RSA -keysize 2048 -validity 10000
   ```
2. Point `signingConfigs.camara` at it, and load the passwords from
   environment variables or `local.properties` rather than hard-coding them.
3. Back the keystore up. If you lose it you can never update the app again.
4. Build an **App Bundle** (`./gradlew bundleRelease`) for the Play Store — Play
   re-signs with its own key and delivers per-device splits, which shrinks the
   download further.
