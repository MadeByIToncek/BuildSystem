package space.itoncek.buildSystem.files;

import org.eclipse.jgit.api.Git;

import java.io.File;
import java.io.Serializable;

public final class Repository implements Serializable {
	public final String gitAddress;
	public final File directory;
	public final String worldname;
	public Git git;

	public Repository(String gitAddress, File directory, String worldname) {
		this.gitAddress = gitAddress;
		this.directory = directory;
		this.worldname = worldname;
	}

	public String worldName()  {return worldname;}

	@Override
	public String toString() {
		return "Repository[" +
			   "gitAddress=" + gitAddress + ", " +
			   "directory=" + directory + ']';
	}

}

