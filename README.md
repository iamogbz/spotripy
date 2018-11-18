# spotripy

Download spotify playlist freely

## Help

Enter "help" in the program console (\$) to see list of available commands.

### Spotify OAuth Access Token

Even if you aren't a developer you can get a free token from spotify.
It lasts about an hour which is more than enough time for single use.

1. Visit https://developer.spotify.com/web-api/console/get-playlist/
2. Click the "GET TOKEN" button
3. Select the relevant scopes on the popup e.g. playlist-read-private
4. Click the "REQUEST TOKEN" button
5. Login and/or Accept the authorisation request
6. Use the generated OAuth Token in the program i.e. `$ token {OAUTH_ACCESS_TOKEN}`
