package space.itoncek.buildSystem.commands;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import space.itoncek.buildSystem.BuildSystem;
import space.itoncek.buildSystem.files.Repository;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

public class GitCommand implements CommandExecutor {
	private final BuildSystem bs;

	public GitCommand(BuildSystem bs) {
		this.bs = bs;
	}

	@Override
	public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
		if(args.length >= 1) {
			switch (args[0]) {
				case "add" -> {
					if (args.length != 3)
						sender.sendMessage(Component.text("Wrong amount of arguments, correct /git add [user_name/repo_name] [world_name]", TextColor.color(133, 0, 0)));
					else {
						try {
							sender.sendMessage(Component.text("Cloning repository " + args[1] + " as world " + args[2],TextColor.color(23, 188, 9)));
							World w = bs.git.addRepo(args[1], args[2], Bukkit::broadcast);
							sender.sendMessage(Component.text("Repository cloned, teleporting to " + args[2],TextColor.color(23, 188, 9)));
							if (!w.getBlockAt(0, 0, 0).getType().equals(Material.BARRIER)) {
								w.getBlockAt(0, 0, 0).setType(Material.BARRIER);
							}
							if (sender instanceof Player p) {
								p.teleport(new Location(w, 0, 1, 0));
							}
						} catch (GitAPIException | IOException e) {
							bs.getSLF4JLogger().error("Error while adding a world",e);
							sender.sendMessage(Component.text("Unable to add world, please check console to see why!", TextColor.color(133, 0, 0)));
						}
					}
				}
				case "commit" -> {
					if (args.length != 2) {
						sender.sendMessage(Component.text("Wrong amount of arguments, correct /git commit [world_name]", TextColor.color(133, 0, 0)));
						return true;
					}

					Repository repo = bs.git.getRepo(args[1]);
					if (repo == null) {
						sender.sendMessage(Component.text("Can't find a world with \"%s\" as it's name.".formatted(args[1]), TextColor.color(133, 0, 0)));
						return true;
					}
					List<? extends Player> list = Bukkit.getOnlinePlayers().stream().filter(p -> p.getLocation().getWorld().getName().equals(args[1])).toList();
					HashMap<Player,Location> locmap = list.stream().collect(Collectors.toMap(x->x, Entity::getLocation, (prev, next) -> next, HashMap::new));
					list.forEach(p -> p.teleport(new Location(bs.w, 0, 101, 0)));

					World w = null;
					try {
						String msg = "Manual commit, induced by " + (sender instanceof Player p ? p.getName() : "CONSOLE");
						w = bs.git.saveRepo(repo, msg, Bukkit::broadcast);
					} catch (GitAPIException e) {
						sender.sendMessage(Component.text("Error while saving, please check console for more info!", TextColor.color(133, 0, 0)));
						bs.getSLF4JLogger().error("Error while saving a world",e);
					} finally {
						World finalW = w;
						locmap.forEach((p, l)-> {
							l.setWorld(finalW);
							p.teleport(l);
						});
					}
				}
			}
		}
		return true;
	}

	public GitCommandHelper getHelper() {
		return new GitCommandHelper();
	}

	public class GitCommandHelper implements TabCompleter {
		@Override
		public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
			return switch (args.length) {
				case 1 -> List.of("add", "commit");
				case 2 -> switch (args[0]) {
					case "add" -> List.of("[url]");
					case "commit" -> bs.git.listRepos();
					default -> List.of();
				};
				case 3 -> switch (args[0]) {
					case "add" -> List.of("[world_name]");
					default -> List.of();
				};
				default -> List.of();
			};
		}
	}
}
