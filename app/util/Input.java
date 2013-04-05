package util;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.AutoCloseInputStream;
import play.Play;

import java.io.IOException;
import java.io.InputStream;

public class Input {
    private Input() {
    }

    public static String resourceToString(final String resourcePath) throws IOException {
        final InputStream input = new AutoCloseInputStream(Play.application().resourceAsStream(resourcePath));
        return IOUtils.toString(input, "UTF-8");
    }
}
