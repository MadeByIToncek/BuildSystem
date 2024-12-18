package space.itoncek.buildSystem;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.GameRule;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.entity.Player;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.BatchingProgressMonitor;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.jetbrains.annotations.Nullable;
import org.json.JSONArray;
import org.json.JSONObject;
import space.itoncek.buildSystem.files.Config;
import space.itoncek.buildSystem.files.Repository;


import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

public class GitDriver {
	private final BuildSystem bs;
	private final File conf;
	private final File db;
	private final Config cfg;
	private final ArrayList<Repository> repositories;
	private final CredentialsProvider creds;

	public GitDriver(BuildSystem bs) throws IOException, ClassNotFoundException {
		this.bs = bs;
		conf = new File(bs.getDataFolder() + "/config.json");
		db = new File(bs.getDataFolder() + "/db.j");
		cfg = loadCFG();
		creds = new UsernamePasswordCredentialsProvider(cfg.getUser(),cfg.getPassword());
		repositories = loadRepos();
	}

	public World addRepo(String reponame, String worldName, Consumer<Component> messageCallback) throws GitAPIException, IOException {
		Git git = Git.cloneRepository()
				.setCredentialsProvider(creds)
				.setURI(cfg.getBaseAddress()+reponame)
				.setDirectory(new File("./" + worldName))
				.setProgressMonitor(new MinecraftProgressMonitor(messageCallback))
				.call();

		Repository r = new Repository(cfg.getBaseAddress()+reponame, new File("./" + worldName),worldName);
		r.git = git;
		repositories.add(r);
		World w = bs.getServer().createWorld(new WorldCreator(worldName).generator(new BuildSystem.VoidGenerator()).generateStructures(false));
		assert w != null;
		w.setGameRule(GameRule.DO_WEATHER_CYCLE,false);
		w.setGameRule(GameRule.DO_DAYLIGHT_CYCLE,false);
		w.setGameRule(GameRule.DO_MOB_SPAWNING,false);
		w.setGameRule(GameRule.DO_FIRE_TICK,false);
		return w;
	}

	private ArrayList<Repository> loadRepos() throws IOException, ClassNotFoundException {
		if (!db.exists()) {
			db.getParentFile().mkdirs();
			db.createNewFile();
			ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(db));
			oos.writeObject(new ArrayList<Repository>());
			oos.close();
		}
		ObjectInputStream ois = new ObjectInputStream(new FileInputStream(db));
		Object o = ois.readObject();
		ois.close();
		if (o instanceof ArrayList<?>) {
			return (ArrayList<Repository>) o;
		} else throw new RuntimeException("Unable to parse DB");
	}

	public void saveRepos() throws IOException, GitAPIException {
		Bukkit.getOnlinePlayers().forEach(Player::kick);
		for (Repository r : repositories) {
			saveRepo(r,"System shutdown commit", Bukkit::broadcast);
			r.git.close();
			r.git = null;
		}
		db.delete();
		ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(db));
		oos.writeObject(repositories);
		oos.close();
	}

	public @Nullable Repository getRepo(String worldname) {
		for (Repository r : repositories) {
			if(r.worldname.equals(worldname)) return r;
		}
		return null;
	}

	public World saveRepo(Repository r, String commitMessage, Consumer<Component> messageCallback) throws GitAPIException {
		bs.getServer().unloadWorld(Objects.requireNonNull(bs.getServer().getWorld(r.worldname)),true);

		r.git.add()
				.addFilepattern(".")
				.call();
		r.git.commit()
				.setCredentialsProvider(creds)
				.setSign(false)
				.setMessage(commitMessage)
				.setAuthor("System", "system@itoncek.space")
				.call();
		r.git.push()
				.setCredentialsProvider(creds)
				.setProgressMonitor(new MinecraftProgressMonitor(messageCallback))
				.call();

		return bs.getServer().createWorld(new WorldCreator(r.worldname).generator(new BuildSystem.VoidGenerator()).generateStructures(false));
	}

	private Config loadCFG() throws IOException {
		if (!conf.exists()) {
			conf.getParentFile().mkdirs();
			conf.createNewFile();
			Files.writeString(conf.toPath(), generateTemplateConfig().toString(4), StandardOpenOption.WRITE);
		}
		Config temp = new Config(conf);
		if (temp.valid()) {
			return temp;
		} else {
			conf.delete();
			return loadCFG();
		}
	}

	public static JSONObject generateTemplateConfig() {
		return new JSONObject().put("basePath","http://localhost:3000/").put("user","system").put("password","syssyssys").put("repositories", new JSONArray());
	}

	public @Nullable List<String> listRepos() {
		return repositories.stream().map(Repository::worldName).toList();
	}

	private static class MinecraftProgressMonitor extends BatchingProgressMonitor {

		private final Consumer<Component> messageCallback;

		public MinecraftProgressMonitor(Consumer<Component> messageCallback) {
			this.messageCallback = messageCallback;
		}

		/** {@inheritDoc} */
		@Override
		protected void onUpdate(String taskName, int workCurr, Duration duration) {
			StringBuilder s = new StringBuilder();
			format(s, taskName, workCurr, duration);
			send(s);
		}

		/** {@inheritDoc} */
		@Override
		protected void onEndTask(String taskName, int workCurr, Duration duration) {
			StringBuilder s = new StringBuilder();
			format(s, taskName, workCurr, duration);
			s.append("\n"); //$NON-NLS-1$
			send(s);
		}

		private void format(StringBuilder s, String taskName, int workCurr,
							Duration duration) {
			s.append("\r"); //$NON-NLS-1$
			s.append(taskName);
			s.append(": "); //$NON-NLS-1$
			while (s.length() < 25)
				s.append(' ');
			s.append(workCurr);
			appendDuration(s, duration);
		}

		/** {@inheritDoc} */
		@Override
		protected void onUpdate(String taskName, int cmp, int totalWork, int pcnt,
								Duration duration) {
			StringBuilder s = new StringBuilder();
			format(s, taskName, cmp, totalWork, pcnt, duration);
			send(s);
		}

		/** {@inheritDoc} */
		@Override
		protected void onEndTask(String taskName, int cmp, int totalWork, int pcnt,
								 Duration duration) {
			StringBuilder s = new StringBuilder();
			format(s, taskName, cmp, totalWork, pcnt, duration);
			s.append("\n"); //$NON-NLS-1$
			send(s);
		}

		private void format(StringBuilder s, String taskName, int cmp,
							int totalWork, int pcnt, Duration duration) {
			s.append("\r"); //$NON-NLS-1$
			s.append(taskName);
			s.append(": "); //$NON-NLS-1$
			while (s.length() < 25)
				s.append(' ');

			String endStr = String.valueOf(totalWork);
			String curStr = String.valueOf(cmp);
			while (curStr.length() < endStr.length())
				curStr = " " + curStr; //$NON-NLS-1$
			if (pcnt < 100)
				s.append(' ');
			if (pcnt < 10)
				s.append(' ');
			s.append(pcnt);
			s.append("% ("); //$NON-NLS-1$
			s.append(curStr);
			s.append('/');
			s.append(endStr);
			s.append(')');
			appendDuration(s, duration);
		}

		private void send(StringBuilder s) {
			messageCallback.accept(Component.text(s.toString(), TextColor.color(40, 40, 40)));
		}
	}
}
