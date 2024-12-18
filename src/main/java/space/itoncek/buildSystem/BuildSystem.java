package space.itoncek.buildSystem;

import org.bukkit.GameRule;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.plugin.java.JavaPlugin;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.jetbrains.annotations.NotNull;
import space.itoncek.buildSystem.commands.GitCommand;
import space.itoncek.buildSystem.events.JoinLeaveEvents;

import java.io.IOException;
import java.util.Random;

public final class BuildSystem extends JavaPlugin {

	public World w;
	public GitDriver git;

	@Override
	public void onEnable() {
		generateLobby();
		try {
			git = new GitDriver(this);
		} catch (IOException | ClassNotFoundException e) {
			throw new RuntimeException(e);
		}
		getServer().getPluginManager().registerEvents(new JoinLeaveEvents(this),this);
		GitCommand g = new GitCommand(this);
		getCommand("git").setExecutor(g);
		getCommand("git").setTabCompleter(g.getHelper());
	}

	private void generateLobby() {
		// Plugin startup logic
		if (getServer().getWorlds().stream().noneMatch(x-> x.getName().equals("lobby"))) {
			w = getServer().createWorld(generateWoldCreator());
		} else if (getServer().getWorlds().stream().anyMatch(x->x.getName().equals("lobby"))){
			w = getServer().getWorlds().stream().filter(x->x.getName().equals("lobby")).findFirst().get();
		}
		assert w != null;
		w.setGameRule(GameRule.DO_WEATHER_CYCLE,false);
		w.setGameRule(GameRule.DO_DAYLIGHT_CYCLE,false);
		w.setGameRule(GameRule.DO_MOB_SPAWNING,false);
		w.setGameRule(GameRule.DO_FIRE_TICK,false);
		for (int x = -5; x <= 5; x++) {
			for (int z = -5; z <= 5; z++) {
				w.getBlockAt(x,99,z).setType(Material.BARRIER);
				w.getBlockAt(x,104,z).setType(Material.BARRIER);
			}
		}
		for (int x = -5; x <= 5; x++) {
			for (int y = 99; y <= 104; y++) {
				w.getBlockAt(x,y,-5).setType(Material.BARRIER);
				w.getBlockAt(x,y,5).setType(Material.BARRIER);
				w.getBlockAt(-5,y,x).setType(Material.BARRIER);
				w.getBlockAt(5,y,x).setType(Material.BARRIER);
			}
		}
	}

	private @NotNull WorldCreator generateWoldCreator() {
		WorldCreator w = new WorldCreator("lobby");
		w.generator(new VoidGenerator());
		return w;
	}

	@Override
	public void onDisable() {
		// Plugin shutdown logic
		try {
			git.saveRepos();
		} catch (IOException | GitAPIException e) {
			throw new RuntimeException(e);
		}
	}

	public static class VoidGenerator extends ChunkGenerator {
		@Override
		public @NotNull ChunkData generateChunkData(@NotNull World world, @NotNull Random random, int x, int z, @NotNull BiomeGrid biome) {
			ChunkData chunk = createChunkData(world);

			chunk.setRegion(0,world.getMinHeight(), 0, 15,world.getMaxHeight(), 15, Material.AIR);

			return chunk;
		}
	}
}
