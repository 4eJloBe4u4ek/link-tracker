package backend.academy.bot.utils;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

public class UrlChecker {
    public static boolean isValidUrl(String urlString) {
        HttpURLConnection connection = null;
        try {
            URL url = new URI(urlString).toURL();
            if (!url.getProtocol().equals("http") && !url.getProtocol().equals("https")) {
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
