package services.backend.project.filestore;

import org.apache.commons.lang3.RandomStringUtils;

import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;

public final class PathFactory {
    private PathFactory() {
    }

    public static PathFactory path() {
        return new PathFactory();
    }

    public PathFactoryHashedFile hash(final String hash) {
        return new PathFactoryHashedFile(hash);
    }

    public String tmp() {
        return "tmp/" + randomAlphanumeric(30);
    }

    public static class PathFactoryHashedFile {
        private final String hash;

        private PathFactoryHashedFile(final String hash) {
            this.hash = hash;
        }

        public String zipped() {
            return getPathForPrefix("zipped/");
        }

        public String raw() {
            return getPathForPrefix("raw/");
        }


        private String getPathForPrefix(String prefix) {
            return prefix + hash.substring(0, 3) + "/" + hash.substring(3);
        }
    }
}