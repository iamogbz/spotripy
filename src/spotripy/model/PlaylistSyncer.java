/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package spotripy.model;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.JSONObject;
import spotripy.model.SpotifyREST.Playlist;

/**
 *
 * @author Emmanuel
 */
public class PlaylistSyncer {

    private final static long MIN_FILE_SIZE = 1024 * 1024 * 3; // 3MB
    private final static long MAX_FILE_SIZE = 1024 * 1024 * 20; // 20MB
    private final static String FILE_EXT = "mp3";
    private final static Logger logger = Logger.getLogger(PlaylistSyncer.class.getName());

    private final Configuration config; // playlist sync persitent config
    private HashMap<String, String> cacheDb; // playlist sync download url cacheDb

    /**
     * Spotify OAuth Access Token. Empty on initialisation.
     */
    public static String spotifyAccessToken = "";

    /**
     * Default download save path.
     */
    public static String downloadSavePath = System.getProperty("user.dir");

    /**
     * Spotify user id.
     */
    public String userID;

    /**
     * Spotify playlist id.
     */
    public String playlistID;

    /**
     * Creates instance of this class with default values.
     */
    public PlaylistSyncer() {
        this.userID = "";
        this.playlistID = "";
        config = Configuration.getInstance();
    }

    private void loadCache(String playlistFolder) {
        File cacheFile = new File(downloadSavePath + File.separator + playlistFolder + File.separator + ".cache");
        cacheDb = new HashMap<>();
        try (InputStream file = new FileInputStream(cacheFile);
                InputStream buffer = new BufferedInputStream(file);
                ObjectInput input = new ObjectInputStream(buffer);) {
            // deserialize the List
            cacheDb = (HashMap<String, String>) input.readObject();
        } catch (ClassNotFoundException ex) {
            logger.log(Level.WARNING, "Cannot load cache. No interpreter.");
        } catch (IOException ex) {
            logger.log(Level.WARNING, "Cannot load cache.");
        }
    }

    private synchronized void saveCache(String playlistName) {
        File cacheFile = new File(downloadSavePath + File.separator + playlistName + File.separator + ".cache");
        try (OutputStream file = new FileOutputStream(cacheFile);
                OutputStream buffer = new BufferedOutputStream(file);
                ObjectOutput output = new ObjectOutputStream(buffer);) {
            output.writeObject(cacheDb);
        } catch (IOException ex) {
            logger.log(Level.WARNING, "Cannot save cache.");
        }
    }

    /**
     * Load persistent configuration
     */
    public void loadConfig() {
        userID = config.getProperty("user-id", userID);
        playlistID = config.getProperty("playlist-id", playlistID);
        downloadSavePath = config.getProperty("playlist-folder", downloadSavePath);
        spotifyAccessToken = config.getProperty("spotify-oauth-token", spotifyAccessToken);
    }

    /**
     * Print to sout String representation of configuration.
     */
    public void showConfig() {
        int len = 23;
        String prefix = "", suffix = ": ";
        System.out.println(header("CONFIGURATION"));
        System.out.println(fit("User ID", len, prefix, suffix) + userID + "\n" + fit("Playlist ID", len, prefix, suffix)
                + playlistID + "\n" + fit("Playlist Folder", len, prefix, suffix) + downloadSavePath + "\n"
                + fit("Spotify Oauth Token", len, prefix, suffix) + spotifyAccessToken);
        System.out.println();
    }

    private String fit(String text, int length, String prefix, String suffix) {
        StringBuilder sb = new StringBuilder(prefix + text);
        sb.setLength(length - suffix.length());
        return sb.toString() + suffix;
    }

    private String header(String title) {
        String border = "-------------------------------------------------";
        return border + "\n " + title + "\n" + border;
    }

    /**
     * Save persistent configuration
     */
    public void saveConfig() {
        config.setProperty("user-id", userID);
        config.setProperty("playlist-id", playlistID);
        config.setProperty("playlist-folder", downloadSavePath);
        config.setProperty("spotify-oauth-token", spotifyAccessToken);
        config.saveProperties();
    }

    public boolean sync() throws Exception {
        if (downloadSavePath == null || downloadSavePath.isEmpty()) {
            throw new PlaylistSyncException("Download folder not supplied");
        } else {
            File downloadFolder = new File(downloadSavePath);
            if (!downloadFolder.exists()) {
                throw new InaccessibleFolderException("Path '" + downloadSavePath + "' does not exist");
            } else if (!downloadFolder.isDirectory()) {
                throw new InaccessibleFolderException("Path '" + downloadSavePath + "' is not a folder");
            } else if (!downloadFolder.canWrite()) {
                throw new InaccessibleFolderException("Path '" + downloadSavePath + "' is not writable");
            } else if (userID == null || playlistID == null || userID.isEmpty() || playlistID.isEmpty()) {
                throw new PlaylistNotFoundException("User/Playlist not supplied");
            } else if (spotifyAccessToken == null || spotifyAccessToken.isEmpty()) {
                throw new PlaylistSyncException("OAuth Access Token not supplied");
            } else {
                showConfig(); // print out the configuration
                logger.info("Retrieving playlist");
                SpotifyREST.AccessToken = spotifyAccessToken;
                Playlist playlist = null;
                try {
                    playlist = SpotifyREST.getPlaylist(userID, playlistID);
                } catch (Exception ex) {
                    switch (SpotifyREST.getResponseCode()) {
                    case 401:
                        String message = new JSONObject(SpotifyREST.getResponseBody().trim()).getJSONObject("error")
                                .getString("message");
                        throw new ExpiredTokenException(message);
                    default:
                        throw new IOException("Connection Error", ex);
                    }
                }
                if (playlist == null) {
                    throw new PlaylistNotFoundException("Playlist not retrieved");
                } else {
                    logger.log(Level.INFO, "Playlist: {0} ({1} tracks)",
                            new Object[] { playlist.name, playlist.tracks.size() });
                    // --- create playlist folder
                    logger.info("Preparing playlist download folder");
                    String folderName = sanitize(playlist.name);
                    // Check if file already exists and if
                    // the file size is larger than the min
                    File playlistFolder = new File(downloadSavePath + File.separator + folderName);
                    logger.log(Level.INFO, "Path: {0}", playlistFolder.getAbsolutePath());
                    playlistFolder.mkdir(); // FileUtils creates folder on download
                    loadCache(folderName); // load cacheDb of download urls
                    // --- loop through tracks and try downloading
                    logger.info("Searching mp3skull for download links");
                    for (String track : playlist.tracks) {
                        File mp3File = new File(
                                playlistFolder.getAbsolutePath() + File.separator + sanitize(track) + "." + FILE_EXT);
                        // Check if file already exists and if
                        // the file size is larger than the min
                        if (mp3File.exists() && mp3File.length() > MIN_FILE_SIZE) {
                            logger.log(Level.INFO, "Track already downloaded: ''{0}'' {1}",
                                    new Object[] { mp3File.getName(), mp3File.getParent() });
                        } else {
                            boolean usingCache = false;
                            String downloadLink = cacheDb.get(track);
                            if (downloadLink == null) {
                                try {
                                    downloadLink = Mp3Skull.getDownloadLink(track);
                                } catch (IOException ex) {
                                    // logger.log(Level.INFO, null, ex);
                                }
                            } else {
                                usingCache = true;
                            }
                            if (downloadLink == null || downloadLink.isEmpty()) {
                                logger.log(Level.INFO, "Unable to find: {0}", track);
                            } else {
                                try {
                                    URL downloadURL = new URL(downloadLink);
                                    logger.log(Level.INFO, "Downloading ''{0}'' {1} {2}",
                                            new Object[] { track, usingCache ? ">u>" : "<n<", downloadLink });
                                    copyURLToFile(downloadURL, mp3File);
                                    cacheDb.put(track, downloadLink);
                                } catch (Exception ex) {
                                    logger.log(Level.INFO, "Unable to download: {0}", downloadLink);
                                    // cacheDb.remove(track); // remove failed URL from cache
                                } finally {
                                    saveCache(folderName);
                                }
                            }
                        }
                    }
                }
            }
        }
        return true;
    }

    /**
     * Remove unwanted characters for filenames.
     *
     * @param name String to sanitise
     * @return the sanitised string
     */
    private static String sanitize(String name) {
        // return URLEncoder.encode(name, StandardCharsets.UTF_8.displayName());
        return name.replaceAll("[^a-zA-Z0-9.-]+", "_");
    }

    private static void copyURLToFile(URL url, File file) throws Exception {
        // FileUtils.copyURLToFile(url, file); // Apache Commons
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestProperty("Content-Type", "application/force-download");
        conn.addRequestProperty("Accept", "*/*");
        conn.addRequestProperty("User-Agent", "FDM 3.x");
        conn.setInstanceFollowRedirects(false);
        Long byteSize = conn.getContentLengthLong();
        // only try copy if potential download is larger than minimum
        if (byteSize > MIN_FILE_SIZE && byteSize <= MAX_FILE_SIZE) {
            Long bytesWritten;
            try (InputStream in = conn.getInputStream()) {
                logger.log(Level.INFO, "Download started {0}", file.getName());
                bytesWritten = Files.copy(in, file.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
            logger.log(Level.INFO, "Content Length: {0}. Bytes Written: {1}.",
                    new Object[] { conn.getContentLengthLong(), bytesWritten });
        } else {
            throw new IOException("File size not within allowed range. " + byteSize + " bytes.");
        }
    }

    /**
     * Playlist Sync Exception Class
     */
    public static class PlaylistSyncException extends Exception {

        private final String message;

        /**
         * Create new exception.
         *
         * @param message Definitive error message
         */
        public PlaylistSyncException(String message) {
            this.message = message;
        }

        @Override
        public String getMessage() {
            return this.message;
        }

    }

    public static class InaccessibleFolderException extends PlaylistSyncException {
        /**
         * Create new exception.
         *
         * @param message Definitive error message
         */
        public InaccessibleFolderException(String message) {
            super(message);
        }
    }

    public static class PlaylistNotFoundException extends Exception {

        /**
         * Create new exception.
         *
         * @param message Definitive error message
         */
        public PlaylistNotFoundException(String message) {
            super(message);
        }
    }

    public static class ExpiredTokenException extends Exception {

        /**
         * Create new exception.
         *
         * @param message Definitive error message
         */
        public ExpiredTokenException(String message) {
            super(message);
        }
    }

    public static class TrackDownloadException extends Exception {

        /**
         * Create new exception.
         *
         * @param message Definitive error message
         */
        public TrackDownloadException(String message) {
            super(message);
        }
    }

}
