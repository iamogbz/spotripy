# spotripy

Download spotify playlist freely

## Run

Make commands to get started

Freshly build and run app

```
make run-build
```

Remove previously build files

```
make clean
```

Build app from source files

```
make build
```

Run app from build files

```
make run
```

Rebuild app in fresh environment

```
make clean-build
```


## Help

From in the app console, enter "help" to see list of available commands.

```
$ help
```

### Spotify OAuth Access Token

Even if you aren't a developer you can get a free token from spotify.
It lasts about an hour which is more than enough time for single use.

1. Visit https://developer.spotify.com/web-api/console/get-playlist/
2. Click the "GET TOKEN" button
3. Select the relevant scopes on the popup e.g. playlist-read-private
4. Click the "REQUEST TOKEN" button
5. Login and/or Accept the authorisation request
6. Use the generated OAuth Token in the program i.e. `$ token {OAUTH_ACCESS_TOKEN}`
