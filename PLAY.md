# Publishing to Google Play

Camara ships two artefacts from one codebase. Which one you use depends on where it is
going.

| | `sideload` | `play` |
| --- | --- | --- |
| Built by | `./gradlew assembleSideloadRelease` | `./gradlew bundlePlayRelease` |
| Output | `app/build/outputs/apk/sideload/release/*.apk` | `app/build/outputs/bundle/playRelease/*.aab` |
| Goes to | the GitHub release | the Play Console |
| In-app updater | yes | **no** |
| `REQUEST_INSTALL_PACKAGES` | yes | **removed** |
| Installs directly | yes | no — Play does |

## Why two channels

Play does not allow an app to install its own updates. `REQUEST_INSTALL_PACKAGES` needs a
declaration, and it is granted only to a narrow set of apps — file managers, antivirus,
enterprise tooling. A camera app is not one of them, so a build that carries the
permission **and** uses it to install a downloaded APK gets rejected.

Rather than strip the updater from everyone, the `play` flavour:

- sets `BuildConfig.SELF_UPDATE_ENABLED = false`, so the Updates section is not in
  Settings at all, and
- removes the permission through `app/src/play/AndroidManifest.xml` with
  `tools:node="remove"`.

The `sideload` build keeps both, which is what people downloading from the releases page
need.

## Play App Signing

When you upload an `.aab`, **Google re-signs it**. There are two keys from then on:

- **Upload key** — the one you sign the bundle with. That is the keystore in this
  repository's secrets (`CAMARA_KEYSTORE_BASE64`).
- **App signing key** — Google's, generated when you enrol. It is what ends up on users'
  devices. Google holds it; you never see it.

This has two consequences worth knowing before you ship:

1. **A sideloaded install cannot be upgraded by a Play install.** They are signed by
   different keys, so Android sees them as different apps. Someone moving from the APK to
   the Play version has to uninstall first. Same the other way round.
2. **The upload key can be reset.** If it is lost, you can ask Google to reset it — you
   are not locked out the way you would be if you were managing the signing key yourself.

If you would rather manage the signing key yourself, opt out of Play App Signing when you
create the app. Then the keystore in the secrets becomes the app signing key, and **losing
it means you can never update the app again**. The backup delivered alongside the release
is the only copy outside the repository secret — keep it somewhere safe either way.

## Checklist before an upload

1. `versionCode` must be higher than the last upload. It lives in `app/build.gradle.kts`.
2. `targetSdk` must meet Play's current requirement (34 at the time of writing).
3. Download `camara-<version>.aab` from the release, or build it:
   `./gradlew bundlePlayRelease` with the keystore environment variables set.
4. Upload it in the Play Console, fill in the store listing, and complete the data safety
   and content rating forms.

## What is not done yet

- **No store listing assets.** Play needs screenshots, a feature graphic and a short
  description. None of that exists in this repository.
- **No privacy policy URL.** Play requires one for most apps, and a camera app that reads
  the media library will certainly be asked. The app itself collects nothing and has no
  analytics, but the policy page still has to exist and be hosted somewhere.
- **The data safety form has not been drafted.** Everything the app touches — camera,
  microphone, photos, videos — has to be declared.
- **No CI upload to Play.** The bundle is built and attached to the release; publishing it
  to a Play track is still a manual step in the Console.
