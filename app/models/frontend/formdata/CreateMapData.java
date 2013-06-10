package models.frontend.formdata;

import play.data.validation.Constraints.Required;

public class CreateMapData {
    @Required
    private String path;

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }
}
