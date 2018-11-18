/**
 * Spotify REST API implementation. Using http://www.json.org/java/ for JSON
 * parsing
 */
package spotripy.model;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.URL;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ssl.HttpsURLConnection;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Spotify REST API Class.
 *
 * @author Emmanuel
 */
public class SpotifyREST {

    /**
     * OAuth 2.0 Access Token
     */
    public static String AccessToken;
    public static HttpsURLConnection conn;

    private static String lastResponse;
    private static final Logger logger = Logger.getLogger("SpotifyREST");

    /**
     * Create new HttpsURLConnection with defined URL.
     *
     * @param url String URL
     * @return new HttpsURLConnection or NULL on failure
     * @throws MalformedURLException if URL is not properly formatted
     */
    private static HttpsURLConnection connect(String url) throws MalformedURLException {
        return connect(url, null);
    }

    /**
     * Create new HttpsURLConnection with defined URL through proxy.
     *
     * @param url   String URL
     * @param proxy Proxy configuration
     * @return new HttpsURLConnection or NULL on failure
     * @throws MalformedURLException if URL is not properly formatted
     */
    private static HttpsURLConnection connect(String url, Proxy proxy) throws MalformedURLException {
        URL apiUrl = new URL(url);
        Proxy connProxy = proxy == null ? Proxy.NO_PROXY : proxy;
        try {
            return (HttpsURLConnection) apiUrl.openConnection(connProxy);
        } catch (IOException ex) {
            logger.log(Level.SEVERE, null, ex);
        }
        return null;
    }

    /**
     * Get response code of last request.
     *
     * @return response code or 400 on error
     */
    public static int getResponseCode() {
        try {
            return conn.getResponseCode();
        } catch (IOException ex) {
            return 400;
        }
    }

    /**
     * Get raw response body of last request.
     *
     * @return response body or null on error
     */
    public static String getResponseBody() {
        try {
            if (conn != null) {
                return (lastResponse == null) ? buildResponse(conn.getErrorStream()) : lastResponse;
            }
        } catch (IOException ex) {
            logger.log(Level.WARNING, null, ex);
        }
        return null;
    }

    /**
     * Perform get operation on connection.
     *
     * @param url     the URL endpoint
     * @param headers Map of headers to values
     * @return HTTP response as string
     * @throws Exception
     */
    private static String doGet(String url, Map<String, String> headers) throws Exception {
        lastResponse = null;
        try {
            conn = connect(url);
        } catch (MalformedURLException ex) {
            logger.log(Level.SEVERE, null, ex);
        }
        conn.setRequestMethod("GET");
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            conn.setRequestProperty(entry.getKey(), entry.getValue());
        }
        lastResponse = buildResponse(conn.getInputStream());
        return lastResponse;
    }

    private static String buildResponse(InputStream is) throws IOException {
        StringBuilder response;
        try (BufferedReader in = new BufferedReader(new InputStreamReader(is))) {
            String inputLine;
            response = new StringBuilder();
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
        }
        return response.toString();
    }

    /**
     * Get track list of specified playlist.
     *
     * @param userID     spotify user id
     * @param playlistID spotify playlist id
     * @return Playlist with songs e.g. "Yoga - Janelle Monae ft. Jidenna"
     * @throws Exception
     */
    public static Playlist getPlaylist(String userID, String playlistID) throws Exception {
        // build url
        String url = String.format("https://api.spotify.com/v1/users/%s/playlists/%s", userID, playlistID);
        // set headers
        Map<String, String> headers = new HashMap<>();
        headers.put("Accept", "application/json");
        headers.put("Authorization", "Bearer " + AccessToken);
        // get playlist name
        String response = doGet(url, headers);
        JSONObject data = new JSONObject(response);
        Playlist playlist = new Playlist(data.getString("name"));
        // get all playlist tracks
        url += "/tracks";
        do {
            // logger.info(url);
            response = doGet(url, headers);
            data = new JSONObject(response);
            JSONArray items = data.getJSONArray("items");
            for (int i = 0; i < items.length(); i++) {
                JSONObject item = items.getJSONObject(i);
                JSONObject track = item.getJSONObject("track");
                String trackName = track.getString("name");
                JSONArray artists = track.getJSONArray("artists");
                for (int j = 0; j < artists.length(); j++) {
                    // add artists to trackName
                    String artist = artists.getJSONObject(j).getString("name");
                    trackName += trackName.toLowerCase().contains(artist.toLowerCase()) ? "" : " " + artist;
                }
                playlist.addTrack(trackName);
            }
            url = data.get("next") == JSONObject.NULL ? null : data.getString("next");
            // loop until no next set of tracks
        } while (url != null);
        return playlist;
    }

    public static void getPlaylistTracks(String userID, String playlistID) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * Remove duplicate words from a string (sentence).
     *
     * @param s the string to de-duplicate
     * @return the de-duplicated string
     */
    public static String deDuplicate(String s) {
        String placeHolder = " "; // String to replace with temporarily
        String wordPattern = "[a-zA-Z0-9']+"; // Regex to define word blocks

        Pattern dp = Pattern.compile(wordPattern);
        Matcher dm = dp.matcher(s);
        while (dm.find()) {
            // System.out.println(dm.group() + " " + dm.end());
            String match = dm.group();
            s = s.substring(0, dm.end()) + s.substring(dm.end()).replaceAll(dm.group(),
                    String.format("%1$" + match.length() + "s", placeHolder));
        }
        return s.replaceAll("(" + placeHolder + ")+", placeHolder);
    }

    public static class Playlist {

        /**
         * Name of the playlist. Can only be set once
         */
        public final String name;

        /**
         * Playlist tracks. Can only be instantiated once
         */
        public final List<String> tracks;

        /**
         * Instantiate playlist object.
         *
         * @param name Playlist name
         */
        public Playlist(String name) {
            this.name = name;
            this.tracks = new LinkedList<>();
        }

        /**
         * Add track to playlist.
         *
         * @param track String of track information to be added
         * @return true is add is successful
         */
        public boolean addTrack(String track) {
            return tracks.add(track);
        }

        @Override
        public String toString() {
            String data = "Playlist: " + name + "\n" + tracks.toString();
            return data;
        }

    }

}
