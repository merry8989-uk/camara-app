# Signing

**No signing material lives in this repository.** The keystore is stored as an
encrypted GitHub Actions secret and decoded only inside the CI runner.

## Why it works this way

Android refuses to install an APK as an update unless it is signed with the same
certificate as the installed copy. CI runners start empty, so if the key isn't
provided they generate a fresh one per run — different certificate every time, and
you have to uninstall before installing the next build.

So the key is kept stable, but **private**: stored as a secret rather than committed.

## Repository secrets

| Secret | Contents |
| --- | --- |
| `CAMARA_KEYSTORE_BASE64` | the `.p12` keystore, base64-encoded |
| `CAMARA_STORE_PASSWORD` | keystore password |
| `CAMARA_KEY_PASSWORD` | key password |

Plus a plain workflow variable, `CAMARA_KEY_ALIAS` (`camara`).

CI writes the decoded keystore to `keystore/camara-dev.p12` (a git-ignored path) and
points `CAMARA_KEYSTORE` at it. Locally you can do the same — set the environment
variables, or drop your own keystore at `keystore/camara-dev.p12`.

## Rotating the key

```bash
# 1. Make a new keystore (keep it OUT of the repo)
keytool -genkeypair -v -keystore camara-release.jks -alias camara \
  -keyalg RSA -keysize 2048 -validity 10000

# 2. Base64 it and update the secret
base64 -w0 camara-release.jks     # paste into CAMARA_KEYSTORE_BASE64
```

Then update `CAMARA_STORE_PASSWORD` / `CAMARA_KEY_PASSWORD`.

⚠️ Rotating changes the certificate, so **every existing install must be uninstalled
once** before the new build will install. Rotate deliberately.

## A note on history

An earlier version of this project committed a **throwaway** keystore
(`keystore/camara-dev.p12`) to the repository. That file has been deleted from the
working tree, but **it still exists in git history** — deleting a file does not
rewrite past commits. That key is no longer used by any build, but it should be
treated as public and never reused.

The current key was generated after that and has never been committed, so it is
genuinely private.

## Before you publish for real

1. Load the passwords from secrets (as CI does here) — never hard-code them.
2. **Back the keystore up somewhere safe.** Lose it and you can never update the
   app again; you would have to publish under a new package name.
3. Build an **App Bundle** (`./gradlew bundleRelease`) for the Play Store. Play
   re-signs with its own key and delivers per-device splits, which shrinks the
   download further.
4. Consider enabling Play App Signing so Google holds the distribution key.
