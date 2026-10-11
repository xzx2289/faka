# ACG-Faka Native Android Admin (v0.3.0)

A native Kotlin + Jetpack Compose Android client for an already deployed ACG-Faka instance. This project is not a PHP server replacement and does not embed the ACG-Faka website source into the APK.

## Implemented in this iteration

- HTTPS server root URL configuration (saved locally on the device).
- Native login form using the existing `/user/captcha/image?action=adminLogin` image endpoint and `/admin/api/authentication/login` login API.
- In-memory cookie/session handling for the server's existing admin session.
- Native plugin list from `/admin/api/plugin/getPlugins` and enable/disable through `/admin/api/plugin/setConfig?id=<plugin>`.
- Read-only paginated lists for orders, commodities, cards, users, and management logs, using the existing controller endpoints found in the uploaded source archive.
- GitHub Actions workflow that installs Gradle 8.9 and produces a debug APK artifact.

## Not yet complete

- The app does not yet implement all edit/create/delete/export operations for every module. The non-plugin data pages are currently read-only.
- Website settings are deliberately not written yet: the current `Config::setting` endpoint is a POST save operation requiring a complete validated settings payload. Sending a partial or guessed payload could alter the live shop configuration.
- The uploaded source does not contain the proposed public `/mobile-api/v1/bootstrap` endpoint. Forced update and configurable forced announcements require a server-side adapter and a protected way to edit the policy. The current app identifies this as pending instead of falsely reporting it as active.
- The APK has not been built or device-tested in this environment. Use GitHub Actions to build and inspect the log before installing it.

## Build APK with GitHub Actions

1. Create a GitHub repository and upload the **contents** of this project folder to the repository root (so `app/`, `build.gradle.kts`, `settings.gradle.kts`, and `.github/` are at the root).
2. Open **Actions → Build Android APK → Run workflow**.
3. Wait for the run to finish successfully.
4. Download the `acg-faka-mobile-debug-apk` artifact and extract `app-debug.apk`.
5. Install it on your Android phone. This is a debug APK; a production release should use a private signing key and a release build.

## Security notes

- Use HTTPS and a trusted certificate. The client rejects plain HTTP by design.
- Login cookies are held in memory and are cleared when you press logout; passwords are not persisted.
- Do not upload server `.env`, database backups, payment secrets, admin passwords, or customer data to the GitHub repository.
- Keep the ACG-Faka server patched. The APK should use the server's existing authentication and permission checks, not direct database access.
