package com.spotripy;

import java.io.IOException;

public interface ChanteyFinder {

    /**
     * Get the download URL for the specified song.
     *
     * @param songName the name of the song to search for including artists
     * @return download URL as string
     * @throws IOException when unable to connect
     */
    public String getDownloadLink(String songName) throws IOException;
}
