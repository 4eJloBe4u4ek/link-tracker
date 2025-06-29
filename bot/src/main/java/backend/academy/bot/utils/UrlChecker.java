package backend.academy.bot.utils;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class UrlChecker {
    private static final Set<String> ALLOWED_PROTOCOLS = new HashSet<>(Arrays.asList("http", "https"));

    public static boolean isValidUrl(String urlString) {
        HttpURLConnection connection = null;
        try {
            URL url = new URI(urlString).toURL();
            if (!ALLOWED_PROTOCOLS.contains(url.getProtocol())) {
                return false;
            }
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("HEAD");
            connection.connect();

            return connection.getResponseCode() == HttpURLConnection.HTTP_OK;
        } catch (URISyntaxException | IllegalArgumentException | IOException e) {
            return false;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }
}
