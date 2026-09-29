package dev.goowac.client;
import dev.goowac.client.module.*;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import java.util.*;
public final class ClientMenuScreen extends Screen {
    private static final int PURPLE=0xFFE0B6FF, PANEL=0xF0161722;
    private ModuleCategory category=ModuleCategory.RENDER;
    private int page; private String search=""; private TextFieldWidget searchBox;
    public ClientMenuScreen(){super(Text.literal("Goowac Client"));}
    @Override protected void init(){rebuild();}
    private void rebuild(){
        clearChildren(); int left=(width-790)/2,top=(height-500)/2;
        searchBox=new TextFieldWidget(textRenderer,left+26,top+58,240,20,Text.literal("Search modules")); searchBox.setText(search);
        searchBox.setChangedListener(v->{search=v;page=0;rebuild();}); addDrawableChild(searchBox);
        int x=left+286,y=top+58;
        for(ModuleCategory c:ModuleCategory.values()){ final ModuleCategory target=c;
            addDrawableChild(ButtonWidget.builder(Text.literal(c.name().replace('_',' ')),b->{category=target;page=0;rebuild();}).dimensions(x,y,110,20).build());
            x+=116; if(x>left+690){x=left+286;y+=25;}
        }
        List<Module> list=filtered(); int per=18,pages=Math.max(1,(list.size()+per-1)/per); page=Math.min(page,pages-1); int start=page*per;
        for(int i=0;i<per&&start+i<list.size();i++){Module m=list.get(start+i);int col=i%3,row=i/3,bx=left+26+col*240,by=top+150+row*34;
            String label=m.name()+(m.functional()?(m.enabled()?" ON":" OFF"):" •");
            addDrawableChild(ButtonWidget.builder(Text.literal(label),b->{if(!m.functional())return;m.toggle();b.setMessage(Text.literal(m.name()+(m.enabled()?" ON":" OFF")));}).dimensions(bx,by,225,20).build());
        }
        int ny=top+405;
        addDrawableChild(ButtonWidget.builder(Text.literal("< Prev"),b->{if(page>0){page--;rebuild();}}).dimensions(left+26,ny,90,20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Next >"),b->{if(page<pages-1){page++;rebuild();}}).dimensions(left+126,ny,90,20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Done"),b->close()).dimensions(left+584,ny,180,20).build());
    }
    private List<Module> filtered(){List<Module> out=new ArrayList<>();String q=search.trim().toLowerCase();for(Module m:GoowacClient.MODULES.category(category))if(q.isEmpty()||m.name().toLowerCase().contains(q)||m.description().toLowerCase().contains(q))out.add(m);return out;}
    @Override public void render(DrawContext c,int mx,int my,float delta){
        renderBackground(c,mx,my,delta);int left=(width-790)/2,top=(height-500)/2;
        c.fill(left,top,left+790,top+500,PANEL);c.fill(left,top,left+790,top+3,PURPLE);c.drawBorder(left,top,790,500,0xFF393347);
        c.drawTextWithShadow(textRenderer,"GOOWAC CLIENT",left+26,top+18,PURPLE);
        c.drawTextWithShadow(textRenderer,"82-module catalogue + Goowac extras",left+26,top+34,0xFFB8B5C8);
        c.drawTextWithShadow(textRenderer,"Category: "+category.name().replace('_',' '),left+26,top+118,0xFFD8D8E8);
        c.drawTextWithShadow(textRenderer,"• = catalogue-only server-impacting modules",left+26,top+432,0xFF8E8A9B);
        c.drawTextWithShadow(textRenderer,"Right Shift = menu   H = HUD   G = Sprint   C = copy coords",left+26,top+448,0xFF8E8A9B);super.render(c,mx,my,delta);
    }
    @Override public boolean shouldPause(){return false;}
}
