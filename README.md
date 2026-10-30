# Ticklog

Client-side Fabric mod that tracks your Minecraft stats per account and shows them on [ticklog.net](https://ticklog.net).

## How it works

- On game start the mod proves account ownership through Mojang's session server (the same handshake as joining a server) and gets a token for this account.
- Every 30 seconds it tells ticklog.net where you are: launcher, singleplayer or a server address. The website adds up the time between those pings.
- With each ping it sends what you did since the last one: blocks traveled (walked and flown), broken and placed, mob and player kills, deaths and jumps. Only Survival and Adventure count.
- To see your stats and change settings, log in on ticklog.net with the Microsoft account you play with.

Nothing is stored on your PC. The mod sends your UUID, name, playtime, stats and the server address, nothing else.

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
