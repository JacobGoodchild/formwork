# Putting Penguin Forms on the JetBrains Marketplace

This is your decision, Jacob. Nothing here happens by itself: nothing is uploaded until you do step 4.
Everything Penguin Forms needs on its side is ready (listed at the end). What's left needs **your** accounts,
so only you can do it.

## What you'll end up with

Anyone using Rider can find "Penguin Forms" in **Settings → Plugins → Marketplace**, click Install, and get
updates automatically. It's free to publish.

## 1. Make a JetBrains account (5 minutes)

1. Go to <https://account.jetbrains.com> and sign up (free). You may already have one from Rider.
2. Go to <https://plugins.jetbrains.com>, sign in, and accept the **Developer Agreement** when it asks.
3. You'll publish as yourself ("Jacob Goodchild"). That matches the vendor name already in the plugin.

## 2. Decide two small things

- **A contact email for the listing.** The Marketplace shows it to users. It doesn't have to be your main
  one: a separate address for the project is fine. Put it in `src/main/resources/META-INF/plugin.xml` on the
  `<vendor ...>` line as `email="you@example.com"` (or ask me to).
- **Whether the GitHub repository is public.** The listing links to it (the vendor link). If you'd rather keep
  the code private, tell me and I'll take the link out.

## 3. Make a signing key (10 minutes, once)

JetBrains asks that plugins are "signed": a digital stamp that proves an update really came from you.

1. On your Linux laptop, open a Terminal in an empty folder **outside** the project and run these three
   commands (they make a private key, protected by a password you choose, and a certificate):

   ```bash
   openssl genpkey -aes-256-cbc -algorithm RSA -out private_encrypted.pem -pkeyopt rsa_keygen_bits:4096
   ```

   ```bash
   openssl rsa -in private_encrypted.pem -out private.pem
   ```

   ```bash
   openssl req -key private.pem -new -x509 -days 3650 -out chain.crt
   ```

   (The last one asks some questions; your name and country are enough, the rest can be left empty.)
   Then delete `private.pem` (it's the unprotected copy, only needed for that last command). Keep
   `private_encrypted.pem` and `chain.crt`.
2. **Never put these files in the project or send them to anyone.** Keep a copy on a USB stick: if you lose
   them, you'd have to make new ones.
3. On GitHub: your repository → **Settings → Secrets and variables → Actions → New repository secret**, add:
   - `CERTIFICATE_CHAIN`: the whole text of `chain.crt`
   - `PRIVATE_KEY`: the whole text of `private_encrypted.pem`
   - `PRIVATE_KEY_PASSWORD`: the password you chose

   GitHub keeps secrets hidden, even from me.

## 4. The first upload (by hand)

The very first version has to be uploaded on the website:

1. On GitHub, open **Actions → CI**, pick the newest green run, and download **penguin-forms-plugin**
   (unzip it once; inside is `penguin-forms-0.1.0.zip`).
   - Ask me first to make a "Marketplace build" of it (I build it with `-PriderOnly=true`, so it's offered to Rider
     users only rather than every JetBrains IDE, and signed if the secrets from step 3 are set).
2. On <https://plugins.jetbrains.com>: your profile → **Upload plugin**. Choose the zip, licence **MIT**,
   category **Code tools** or **UI**, and tick that it's free.
3. JetBrains checks it automatically (the same "plugin checker" I run, see below), then a person reviews it.
   That usually takes **a few working days**. They email you if something needs changing.

## 5. Later updates

Each new version gets a higher number (`pluginVersion` in `gradle.properties`, e.g. 0.1.0 → 0.2.0) and a few lines
at the top of `docs/CHANGE-NOTES.html`. After the first upload, updates can be published from GitHub instead of the
website: you make a **Permanent Token** on the Marketplace (profile → My Tokens), save it as a GitHub secret called
`PUBLISH_TOKEN`, and the build's `publishPlugin` task uploads with it. Updates are checked too, usually faster.

## Good to know

- **The plugin ID `dev.penguinforms` can never change** once published. The name and description can.
- The listing should show screenshots: use the newest ones in `docs/screenshots/` (e.g. the `tabs-dark-…` ones),
  or better, ones from your own Rider.
- Penguin Forms depends on **NetForms** (version 0.1.5, MIT). If NetForms changes a lot, Penguin Forms may need
  an update to follow it.
- Trademarks: the plugin's name is just "Penguin Forms", and the description says Rider, Visual Studio, Windows
  Forms and Linux belong to their owners and that Penguin Forms isn't endorsed by them. The icon is our own drawing
  (a penguin in front of a form window), not Tux and not a JetBrains or Microsoft logo.

## Already done on Penguin Forms' side

| Item | Where |
|---|---|
| Name (≤ 30 characters, no trademarks), ID, vendor, description, trademark notice | `plugin.xml` |
| Icon, light and dark (40×40 SVG, our own drawing) | `META-INF/pluginIcon.svg`, `pluginIcon_dark.svg` |
| Change notes | `docs/CHANGE-NOTES.html` (put into the plugin when it's built) |
| MIT licence, third-party notices (NetForms, Selawik font) | `LICENSE`, plugin About text |
| Signing and upload settings that read only from secrets | `build.gradle.kts` (`signing`, `publishing`) |
| "Rider only" Marketplace build | `./gradlew buildPlugin -PriderOnly=true` |
| JetBrains' plugin checker against Rider 2026.1.5.3 and 2026.2.4.1 | Actions → CI → **Run workflow** (job `verify-plugin`); report in the run's `plugin-verifier-report` |
| No network use, no telemetry | README, safety reviews in CLAUDE.md |
