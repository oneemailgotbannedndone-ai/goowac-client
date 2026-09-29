package dev.goowac.client.module;
import java.util.*;
public final class ModuleManager {
    private final List<Module> modules = new ArrayList<>();
    private final Map<ModuleCategory, List<Module>> byCategory = new EnumMap<>(ModuleCategory.class);
    public ModuleManager() { for (ModuleCategory c : ModuleCategory.values()) byCategory.put(c, new ArrayList<>()); registerAll(); }
    private void add(ModuleCategory c, String n, String d, boolean f) {
        Module m = new Module(n, c, d, f); modules.add(m); byCategory.get(c).add(m);
    }
    private void registerAll() {
        String[] combat = {"Aim Assist","Anchor Macro","Auto Crystal","Auto Double Hand","Auto Hit Crystal","Auto Inv Totem","Auto Jump Reset","Auto Totem","Crystal Optimizer","Double Anchor","Elytra Swap","HitBox","Hover Totem","Mace Bomber","Mace Swap","No Hit Delay","Shield Breaker","Spear Swap","Static HitBoxes","Totem Offhand","Trigger Bot"};
        for (String n : combat) add(ModuleCategory.COMBAT,n,"Catalogue entry; server-impacting automation is not implemented",false);
        String[] misc = {"Auto Clicker","Auto Eat","Auto Firework","Auto Log","Auto Loot","Auto Mine","Auto Reconnect","Auto Tool","Auto TPA","Auto Walk","Cord Snapper","Elytra Glide","Fakeplayer","Fast Place","Freecam","Key Pearl","Key Wind Charge","Name Protect","Skin Protect","Sprint","Weather Notifier"};
        for (String n : misc) add(ModuleCategory.MISC,n,"Client utility or catalogue entry",n.equals("Name Protect") || n.equals("Skin Protect") || n.equals("Sprint") || n.equals("Weather Notifier") || n.equals("Auto Walk") || n.equals("Cord Snapper"));
        String[] donut = {"Anti Trap","Auto Sell","Auto Spawner Sell","Chunk Finder","Fake Pay","Fake Stats","Item Dropper","Netherite Finder","Player Chunks","Spawner Protect"};
        for (String n : donut) add(ModuleCategory.DONUT,n,"Donut utility catalogue entry; server actions are not implemented",n.equals("Chunk Finder") || n.equals("Netherite Finder") || n.equals("Player Chunks"));
        String[] base = {"Block Entity Debug","Hole ESP","Light Finder","Prime Chunk Finder","RTP Base Finder","Sus Chunk Finder","Suspicious ESP","Tunnel Base Finder","Seed Chunk Finder"};
        for (String n : base) add(ModuleCategory.BASE_FINDING,n,"Visual/debug catalogue entry",true);
        String[] render = {"Block ESP","Block Notifier","Free Look","Fullbright","HUD","Jump Circles","Mob ESP","Name Tags","Ore Sim","Pearl Trajectory","Player ESP","RealHitBox","Music HUD","Storage ESP","SwingSpeed","Target HUD"};
        for (String n : render) add(ModuleCategory.RENDER,n,"Client-side visual catalogue entry",true);
        String[] client = {"Chat Macro","Discord Presence","Friends","Proxy","Radio"};
        for (String n : client) add(ModuleCategory.CLIENT,n,"Client feature entry",n.equals("Friends") || n.equals("Discord Presence"));
        add(ModuleCategory.CLIENT,"FPS","Current client FPS display",true);
        add(ModuleCategory.CLIENT,"Coordinates","Player coordinates display",true);
        add(ModuleCategory.CLIENT,"Spotify Now Playing","Read current Spotify track on macOS",true);
        add(ModuleCategory.CLIENT,"Spotify Lyrics","Show lyrics for current Spotify track",true);
        add(ModuleCategory.CLIENT,"Module List","Show enabled modules on HUD",true);
    }
    public List<Module> all() { return Collections.unmodifiableList(modules); }
    public List<Module> category(ModuleCategory c) { return Collections.unmodifiableList(byCategory.get(c)); }
    public Module find(String name) { return modules.stream().filter(m -> m.name().equalsIgnoreCase(name)).findFirst().orElse(null); }
}
