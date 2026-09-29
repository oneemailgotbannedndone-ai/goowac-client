package dev.goowac.client;
import dev.goowac.client.detection.*;
import dev.goowac.client.module.*;
import dev.goowac.client.music.*;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;

public final class GoowacClient implements ClientModInitializer {
    public static final ModuleManager MODULES = new ModuleManager();
    public static final SpotifyService SPOTIFY = new SpotifyService();
    public static final DetectionTestEngine DETECTION_TEST = new DetectionTestEngine(MODULES);
    private static KeyBinding menu, hud, sprint, coords, detection;

    public void onInitializeClient() {
        set("HUD",true); set("FPS",true); set("Coordinates",true); set("Module List",true); set("Spotify Now Playing",true);
        menu=key("open_menu",GLFW.GLFW_KEY_RIGHT_SHIFT);
        hud=key("toggle_hud",GLFW.GLFW_KEY_H);
        sprint=key("toggle_sprint",GLFW.GLFW_KEY_G);
        coords=key("copy_coords",GLFW.GLFW_KEY_C);
        detection=key("detection_test",GLFW.GLFW_KEY_D);
        SPOTIFY.start();

        ClientTickEvents.END_CLIENT_TICK.register(c -> {
            while(menu.wasPressed()) if(c.currentScreen==null) c.setScreen(new ClientMenuScreen());
            while(hud.wasPressed()) toggle("HUD",c);
            while(sprint.wasPressed()) toggle("Sprint",c);
            while(coords.wasPressed()) copyCoords(c);
            while(detection.wasPressed()) toggleDetectionTest(c);
            if(c.player!=null && enabled("Sprint") && c.player.forwardSpeed>0) c.player.setSprinting(true);
            DETECTION_TEST.tick(c);
        });
        HudRenderCallback.EVENT.register((d,t) -> renderHud(d));
    }

    private static KeyBinding key(String id,int code) {
        return KeyBindingHelper.registerKeyBinding(new KeyBinding("key.goowacclient."+id,InputUtil.Type.KEYSYM,code,KeyBinding.Category.MISC));
    }

    private static void renderHud(net.minecraft.client.gui.DrawContext d) {
        MinecraftClient c=MinecraftClient.getInstance();
        if(c.player==null||c.options.hudHidden||!enabled("HUD")) return;
        int x=8,y=8;
        d.drawTextWithShadow(c.textRenderer,"GOOWAC CLIENT",x,y,0xFFE0B6FF); y+=12;
        if(enabled("FPS")) { d.drawTextWithShadow(c.textRenderer,"FPS: "+c.getCurrentFps(),x,y,0xFFFFFFFF); y+=12; }
        if(enabled("Coordinates")) { d.drawTextWithShadow(c.textRenderer,String.format("XYZ: %.1f / %.1f / %.1f",c.player.getX(),c.player.getY(),c.player.getZ()),x,y,0xFFD8D8E8); y+=12; }
        if(enabled("Module List")) for(Module m:MODULES.all()) if(m.enabled()&&!m.name().equals("HUD")&&!m.name().equals("Module List")) { d.drawTextWithShadow(c.textRenderer,"• "+m.name(),x,y,0xFFB58CFF); if((y+=11)>120) break; }
        if(enabled("Spotify Now Playing")) { SpotifyTrack t=SPOTIFY.current(); if(t.playing()) d.drawTextWithShadow(c.textRenderer,"♫ "+trim(t.title()+" — "+t.artist(),72),8,c.getWindow().getScaledHeight()-34,0xFFE5C8FF); }
        if(enabled("Spotify Lyrics")&&!SPOTIFY.lyrics().isBlank()) {
            String[] lines=SPOTIFY.lyrics().replace("","").split("
");
            int sy=c.getWindow().getScaledHeight()-86,count=0;
            for(int i=Math.max(0,lines.length-4);i<lines.length&&count<4;i++){
                String line=lines[i].replaceFirst("^\\[[0-9]{1,2}:[0-9]{2}(?:\\.[0-9]{1,3})?]\\s*","");
                if(!line.isBlank())d.drawTextWithShadow(c.textRenderer,trim(line,88),8,sy+count++*12,0xFFF2EDF7);
            }
        }
        if(enabled("Detection Test") && DETECTION_TEST.active()) {
            int sy = 132;
            d.drawTextWithShadow(c.textRenderer,"DETECTION TEST · LOCAL ONLY",8,sy,0xFFFFD38C);
            sy += 12;
            d.drawTextWithShadow(c.textRenderer,"Triggered: "+DETECTION_TEST.triggeredCount()+"/"+DETECTION_TEST.results().size(),8,sy,0xFFFFFFFF);
            sy += 12;
            int shown = 0;
            for(DetectionTestResult result : DETECTION_TEST.results()) {
                if (shown++ >= 4) break;
                int color = result.triggered() ? 0xFFFF8B8B : 0xFF9FE3A6;
                d.drawTextWithShadow(c.textRenderer,(result.triggered() ? "!" : "✓")+" "+result.check(),8,sy,color);
                sy += 11;
            }
        }
    }

    private static String trim(String s,int max) { return s.length()<=max?s:s.substring(0,max-1)+"…"; }

    private static void copyCoords(MinecraftClient c) {
        if(c.player==null)return;
        String s=String.format("%.1f %.1f %.1f",c.player.getX(),c.player.getY(),c.player.getZ());
        try{Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(s),null);notify(c,"Copied coordinates: "+s);}
        catch(Exception e){notify(c,"Could not access the system clipboard.");}
    }

    private static void toggleDetectionTest(MinecraftClient client) {
        if (DETECTION_TEST.active()) {
            DETECTION_TEST.stop();
            notify(client,"Detection Test stopped.");
        } else {
            DETECTION_TEST.start(client);
            notify(client,"Detection Test started: synthetic local checks only.");
        }
        Module module = MODULES.find("Detection Test");
        if (module != null) module.setEnabled(DETECTION_TEST.active());
    }

    public static boolean enabled(String n) { Module m=MODULES.find(n); return m!=null&&m.enabled(); }
    private static void set(String n,boolean v) { Module m=MODULES.find(n); if(m!=null)m.setEnabled(v); }
    private static void toggle(String n,MinecraftClient c) { Module m=MODULES.find(n); if(m==null)return; m.toggle(); notify(c,m.name()+(m.enabled()?" enabled":" disabled")); }
    private static void notify(MinecraftClient c,String s) { if(c.player!=null)c.player.sendMessage(Text.literal("§d[Goowac] §f"+s),true); }
}
