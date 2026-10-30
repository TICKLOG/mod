# TickLog

Client-side Fabric mod that tracks your Minecraft playtime and shows it on [ticklog.net](https://ticklog.net).

## How It Works

- On game start, the mod verifies your account through Mojang's session server, the same check Minecraft uses when you join a server.
- Every 30 seconds, it reports where you are playing: in the main menu, in singleplayer or on a server. The website adds up the time between these reports.
- To manage your profile, log in on ticklog.net with Discord and link your Minecraft account.

## Privacy

The mod sends your UUID, name, the address of the server you are playing on and your playtime. It doesn't store anything on your computer. Details are in the [Privacy Policy](https://ticklog.net/privacy-policy).

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
