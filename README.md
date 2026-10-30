# TickLog

Client-side Fabric mod that tracks your Minecraft stats and shows them on [ticklog.net](https://ticklog.net).

## Tracked Stats

- Playtime in singleplayer, on servers and in the main menu
- Blocks traveled, walking and flying with an elytra
- Blocks broken and placed
- Mob and player kills
- Deaths
- Jumps

Stats only count in Survival and Adventure mode. Playtime counts in every mode.

## How It Works

- On game start, the mod verifies your account through Mojang's session server, the same check Minecraft uses when you join a server.
- Every 30 seconds, it reports where you are playing and your stats since the last report.
- Block changes, kills and deaths only count once the server has confirmed them.
- To manage your profile, log in on ticklog.net with Discord and link your Minecraft account.

## Privacy

The mod sends your UUID, name, the address of the server you are playing on, your playtime and your stats. It doesn't store anything on your computer. Details are in the [Privacy Policy](https://ticklog.net/privacy-policy).

## Building

One source tree, one jar per Minecraft version. The versions live in `versions/`:

```
./gradlew build -Pmc=26.3      # one version
./build-all.sh                 # every version
```

Jars end up in `build/libs/` as `ticklog-<mod version>+<minecraft version>.jar`.

## Requirements

- Minecraft 26.1 to 26.3 (pick the jar for your version)
- Fabric Loader 0.19+
- Fabric API
- Java 25

## License

MIT
