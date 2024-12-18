package space.itoncek.buildSystem.files;

import org.json.JSONObject;
import space.itoncek.buildSystem.GitDriver;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public class Config {
	private File f;

	public Config(File f) {
		this.f = f;
	}

	public boolean valid() throws IOException {
		JSONObject obj = new JSONObject(Files.readString(f.toPath()));
		JSONObject template = GitDriver.generateTemplateConfig();

		for (String templateKey : template.keySet()) {
			if (!obj.has(templateKey)) return false;
		}
		return true;
	}

	public String getBaseAddress() throws IOException {
		return getConfig().getString("basePath");
	}

	public String getUser() throws IOException {
		return getConfig().getString("user");
	}

	public String getPassword() throws IOException {
		return getConfig().getString("password");
	}

	private JSONObject getConfig() throws IOException {
		return new JSONObject(Files.readString(f.toPath()));
	}
}
