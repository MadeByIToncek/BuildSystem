package space.itoncek.buildSystem.events;


import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import space.itoncek.buildSystem.BuildSystem;

public class JoinLeaveEvents implements Listener {

	private final BuildSystem bs;

	public JoinLeaveEvents(BuildSystem bs) {
		this.bs = bs;
	}

	@EventHandler(ignoreCancelled = true)
	public void onPlayerJoin(PlayerJoinEvent event) {
		event.getPlayer().teleport(new Location(bs.w, 0,101,0));
		event.getPlayer().setGameMode(GameMode.ADVENTURE);
	}

}
