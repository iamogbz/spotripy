package spotripy.controller;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.logging.Level;
import java.util.logging.Logger;
import spotripy.model.PlaylistSyncer;

/**
 *
 * @author Emmanuel
 */
public class App {

    private final static String TERMINAL = "\n$> ";
    private final static Logger logger = Logger.getLogger(App.class.getName());

    private enum Context {

        PERSIST_CONFIG, CONFIRM_CONFIG, ROOT
    }

    public static void main(String[] args) {

        System.out.println("Welcome to Spotripy");
        System.out.println("Visit https://github.com/iamogbz/spotripy for documentation");
        BufferedReader r = new BufferedReader(new InputStreamReader(System.in));
        String input = "";
        Context context = Context.ROOT;
        PlaylistSyncer playcer = new PlaylistSyncer();
        do {
            if (input.length() < 0) {
                logger.log(Level.WARNING, "Weird input ''{0}''", input);
            }
            if (context.equals(Context.ROOT)) {
                if (input.startsWith("start rip")) {
                    // handle options
                    if (input.startsWith("start rip! -")) {
                        if (input.substring("start rip! -".length()).equals("y")) {
                            playcer.saveConfig();
                        }
                        doSync(playcer);
                    } else if (input.startsWith("start rip!")) {
                        context = Context.PERSIST_CONFIG;
                        System.out.println("Overwrite configuration: (yes|no)");
                    } else {
                        context = Context.CONFIRM_CONFIG;
                        playcer.showConfig();
                        System.out.println("Confirm configuration: (yes|no)");
                    }
                } else if (input.toLowerCase().startsWith("playlist ")) {
                    String[] parts = input.substring("playlist ".length()).split(" ");
                    playcer.userID = (parts.length > 0) ? parts[0] : playcer.userID;
                    playcer.playlistID = (parts.length > 1) ? parts[1] : playcer.playlistID;
                } else if (input.toLowerCase().startsWith("folder ")) {
                    PlaylistSyncer.downloadSavePath = input.substring("folder ".length());
                } else if (input.toLowerCase().startsWith("token ")) {
                    PlaylistSyncer.spotifyAccessToken = input.substring("token ".length());
                } else if (input.toLowerCase().startsWith("load config")) {
                    playcer.loadConfig();
                } else if (input.toLowerCase().startsWith("save config")) {
                    playcer.saveConfig();
                } else if (input.toLowerCase().startsWith("show config")) {
                    playcer.showConfig();
                } else if (input.toLowerCase().startsWith("help")) {
                    System.out.println("Commands");
                    System.out.println("start rip"); // ask to confirm configuration
                    System.out.println("start rip!"); // ask to confirm save of configuration
                    System.out.println("start rip! -y"); // save configuration before exit
                    System.out.println("start rip! -n"); // discard configuration after exit
                    System.out.println("playlist {user_id} {playlist_id}");
                    System.out.println("folder {absolute_download_path}");
                    System.out.println("token {oauth_access}");
                    System.out.println("load config");
                    System.out.println("save config");
                    System.out.println("show config");
                    System.out.println("quit | exit");
                }
            } else if (context.equals(Context.CONFIRM_CONFIG)) {
                if (input.equals("yes")) {
                    context = Context.PERSIST_CONFIG;
                    System.out.println("Overwrite configuration: (yes|no)");
                } else {
                    System.out.println("Rip aborted.");
                    context = Context.ROOT;
                }
            } else if (context.equals(Context.PERSIST_CONFIG)) {
                if (input.equals("yes")) {
                    playcer.saveConfig();
                }
                context = Context.ROOT;
                doSync(playcer);
            }
            System.out.print(TERMINAL);
            try {
                input = r.readLine().trim();
            } catch (IOException ex) {
                //logger.log(Level.SEVERE, null, ex);
                System.out.println("\nError input caught!");
            }
        } while (!input.equals("quit") && !input.equals("exit"));
    }

    private static boolean doSync(PlaylistSyncer playcer) {
        try {
            return playcer.sync();
        } catch (Exception ex) {
            //logger.log(Level.INFO, null, ex);
            System.out.println("Error: " + ex.getMessage());
        }
        return false;
    }

}
