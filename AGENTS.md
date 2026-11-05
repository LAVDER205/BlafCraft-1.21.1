# Codex instructions for BlafCraft

## Project
- Java mod, mod ID `blafcraft`, base package `net.blafteam.blafcraft`.
- Use JDK 21 and the committed Gradle wrapper (currently 9.2.1).
- Loader: NeoForge; ModDevGradle plugin 2.0.140.
- Read `gradle.properties` before making version-dependent changes. It declares Minecraft 1.21.1 but NeoForge 21.0.167 and Parchment for 1.21. Do not assume the declared Minecraft version is the resolved development runtime. Investigate compatibility separately; do not silently upgrade versions.

## Layout
- `src/main/java/net/blafteam/blafcraft/`: gameplay, registrations, networking, client code, and data generators.
- `src/main/resources/`: assets and hand-authored data.
- `src/main/templates/META-INF/neoforge.mods.toml`: metadata template; Gradle expands properties.
- `src/generated/resources/`: configured data generator output; inspect generated changes before committing.

## Workflow and safety
- Make changes on a dedicated `codex/<task>` branch based on the user's intended base.
- Never push directly to main, merge a PR, enable auto-merge, force-push, publish a release, or change repository permissions without explicit user authorization.
- Keep PRs focused. Preserve unrelated user changes.
- Keep Minecraft, loader, plugin, wrapper, mod ID, registry IDs, and network formats unchanged unless required by the requested task. Explain any compatibility or saved-world impact.
- Keep client-only classes out of dedicated-server code paths.
- Never commit credentials, local configuration, worlds, run directories, caches, or build outputs.

## Validation
- Linux/macOS: `./gradlew --no-daemon build`.
- Windows PowerShell: `.\gradlew.bat --no-daemon build`.
- Use the wrapper rather than a system Gradle. Dependency downloads require network access.
- Run `runData` only for changes needing generated resources; review its diff.
- Use `runClient` / `runServer` for relevant manual checks when an appropriate interactive environment is available. Do not accept a Minecraft EULA on the user's behalf.
- Do not treat `runGameTestServer` as a required check without checking for registered GameTests; it can fail when none exist.
- Report the commands run, results, and any checks not performed. A successful build does not establish gameplay or version compatibility.
- Inspect the final diff for accidental resource changes and secrets; submit a PR for user review.
