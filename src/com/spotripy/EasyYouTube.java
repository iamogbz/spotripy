package com.spotripy;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;

import org.json.JSONArray;
import org.jsoup.Jsoup;

/**
 *
 * @author Emmanuel
 */
public class EasyYouTube implements ChanteyFinder {

    private static String USER_AGENT = "Mozilla/5.0 (X11; U; Linux i686) Gecko/20071127 Firefox/2.0.0.11";

    private static String getResponse(URLConnection conn) throws IOException {
        BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        StringBuilder response = new StringBuilder();
        String inputLine;
        while ((inputLine = in.readLine()) != null)
            response.append(inputLine);

        in.close();
        return response.toString();
    }

    public String getDownloadLink(String songName) throws IOException {
        String searchURL = String.format("https://www.youtube.com/results?search_query=%s&pbj=1", sanitize(songName));
        URL youTubeURL = new URL(searchURL);
        HttpURLConnection youTubeConn = (HttpURLConnection) youTubeURL.openConnection();
        youTubeConn.setRequestProperty("user-agent", USER_AGENT);
        youTubeConn.setRequestProperty("pragma", "no-cache");
        youTubeConn.setRequestProperty("referer", searchURL);
        youTubeConn.setRequestProperty("x-youtube-client-name", "1");
        youTubeConn.setRequestProperty("x-youtube-client-version", "2.20181115");
        String youTubeResponse = getResponse(youTubeConn);
        try {
            JSONArray value = new JSONArray(youTubeResponse);
            String videoId = value.getJSONObject(1).getJSONObject("response").getJSONObject("contents")
                    .getJSONObject("twoColumnSearchResultsRenderer").getJSONObject("primaryContents")
                    .getJSONObject("sectionListRenderer").getJSONArray("contents").getJSONObject(0)
                    .getJSONObject("itemSectionRenderer").getJSONArray("contents").getJSONObject(0)
                    .getJSONObject("videoRenderer").getString("videoId");
            String easyURL = String.format("https://www.easy-youtube-mp3.com/download.php?v=%s", videoId);
            return Jsoup.connect(easyURL).header("Cache-Control", "no-cache").userAgent(USER_AGENT).get()
                    .select(".btn.btn-success").get(0).attr("href");
        } catch (Exception e) {
            return null;
        }
    }

    private static String sanitize(String name) {
        return name.replaceAll("[^a-zA-Z0-9]+", " ").replaceAll(" ", "+");
    }

}
