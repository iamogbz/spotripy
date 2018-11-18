package spotripy.model;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;
import org.jsoup.Connection.Response;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

/**
 * Wrapper class for performing searches
 *
 * @author Emmanuel
 */
public class Mp3Skull {

    private static final double MIN_FILE_SIZE = 3.0; // 2MB
    private static final String sizePatternName = "number";
    // Size pattern with flags (?im) CASE_INSENSITIVE AND MULTILINE
    private static final Pattern sizePattern = Pattern.compile("(?im)(?<" + sizePatternName + ">\\d+(.\\d+)) mb");
    private static final String searchKey = "fckh";
    private static String searchToken;
    private static String url = "http://mp3skull.com";

    /**
     * User agent to identify the request. So that mp3skull.com server plays nice.
     */
    public static String USER_AGENT = "Mozilla/5.0 (X11; U; Linux i686) Gecko/20071127 Firefox/2.0.0.11";

    /**
     * Get the search session token.
     *
     * @return the search token
     * @throws IOException when unable to connect
     */
    public static String getSearchToken() throws IOException {
        return getSearchToken(false);
    }

    /**
     * Get the search session token.
     *
     * @param refresh true if to get new token
     * @return the search token
     * @throws IOException when unable to connect
     */
    public static String getSearchToken(boolean refresh) throws IOException {
        if (refresh || searchToken == null) {
            Response res = Jsoup.connect(url).followRedirects(true).userAgent(USER_AGENT).execute();
            Mp3Skull.url = res.url().toString(); // update url to redirect site
            Document doc = res.parse();
            searchToken = doc.select("input[name=" + searchKey + "]").val();
        }
        return searchToken;
    }

    /**
     * Get the download URL for the specified song.
     *
     * @param songName the name of the song to search for including artists
     * @return download URL as string
     * @throws IOException when unable to connect
     */
    public static String getDownloadLink(String songName) throws IOException {
        // call search token earlier to also update Mp3Skull URL
        // https://mp3skull.com/mp3/song_name.html doesn't need search token
        String curSearchToken = getSearchToken();
        String searchURL = String.format(Mp3Skull.url + "/search_db.php?q=%s&%s=%s", sanitize(songName), searchKey,
                curSearchToken);
        Document doc = Jsoup.connect(searchURL).header("Cache-Control", "no-cache").followRedirects(true)
                .userAgent(USER_AGENT).data("ord", "br").post(); // sort results by bitrate in descending order
        // System.out.println(doc);
        Elements selection = doc.select("#song_html[class^=show]");
        for (Element res : selection) {
            String title = res.select("div.mp3_title > b").html();
            String info = res.select("div.left").html();
            Matcher matcher = sizePattern.matcher(info);
            double size = matcher.find() // if the matcher found anything
                    ? Double.valueOf(matcher.group(sizePatternName))
                    : 0;
            if (size > MIN_FILE_SIZE && acceptable(songName, title)) {
                return res.select("div.download_button > a").attr("href");
            }
        }
        return null;
    }

    /**
     * Checks if the title found on mp3skull is an acceptable match for the song
     * being searched for. It checks that only when the song name has things like
     * "instrumentals", "remix" etc. does it match titles that have the same flags.
     *
     * @param songname the song being searched for
     * @param mp3title the title found on mp3skull
     * @return true if the song name and mp3 title have the same flags.
     */
    public static boolean acceptable(String songname, String mp3title) {
        String[] filters = { "instrumental", "remix" };
        for (String filter : filters) {
            String regex = "(?i).*" + filter + ".*";
            if (songname.matches(regex) != mp3title.matches(regex)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Replace illegal characters "[^a-zA-Z0-9]+" in a string with " ".
     *
     * @param name the String to sanitise
     * @return the name stripped of all illegal characters
     */
    private static String sanitize(String name) {
        // return URLEncoder.encode(name, StandardCharsets.UTF_8.displayName());
        return name.replaceAll("[^a-zA-Z0-9]+", " ");
    }

}
