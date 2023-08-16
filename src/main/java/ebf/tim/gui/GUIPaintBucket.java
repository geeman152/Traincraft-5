package ebf.tim.gui;


import ebf.tim.TrainsInMotion;
import ebf.tim.api.SkinRegistry;
import ebf.tim.api.TransportSkin;
import ebf.tim.entities.GenericRailTransport;
import ebf.tim.networking.PacketPaint;
import ebf.tim.utility.ClientProxy;
import ebf.tim.utility.CommonUtil;
import ebf.tim.utility.EventManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.RenderHelper;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.glu.Project;

import java.util.*;

/**
 *@author Oskiek
 */
public class GUIPaintBucket extends GuiScreen {


    public GuiButton buttonSkinDropdown;

    public GuiButton buttonRecolor;
    public GuiButton buttonLivery;

    List<String>  skinList = new ArrayList<>();
    TransportSkin currentTransportSkin;

    int page = 0;

    int guiScreen=0;

    public static int guiTop;
    public static int guiLeft;
    public GenericRailTransport entity;

    public GUIPaintBucket(GenericRailTransport t){
        entity=t;

    }

    @Override
    public void initGui() {
        super.initGui();
        if(entity !=null && skinList.size()==0) {
            skinList = new ArrayList<>();
            if(entity.getSkinList(Minecraft.getMinecraft().thePlayer, true)!=null) {
                List<TransportSkin> TransportSkins = new ArrayList<>(entity.getSkinList(Minecraft.getMinecraft().thePlayer, true).values());
                Collections.sort(TransportSkins, new Comparator<TransportSkin>() {
                    @Override
                    public int compare(TransportSkin o1, TransportSkin o2) {
                        return o1.id - o2.id;
                    }
                });
                for (TransportSkin s : TransportSkins) {
                    skinList.add(s.modid + ":" + s.name);
                }
            }
            currentTransportSkin = entity.getCurrentSkin();
            page = skinList.indexOf(currentTransportSkin.modid + ":" + currentTransportSkin.getName());
        }

        Keyboard.enableRepeatEvents(true);
    }

    @Override
    public void updateScreen() {}

    public static int percentTop(int value){return (int)(guiTop*(value*0.01f));}
    public static int percentLeft(int value){return (int)(guiLeft*(value*0.01f));}

    public int field_147073_u;

    @Override
    public void drawScreen(int parWidth, int parHeight, float p_73863_3_) {
        super.drawScreen(parWidth, parHeight, p_73863_3_);
        guiLeft=new ScaledResolution(Minecraft.getMinecraft(), Minecraft.getMinecraft().displayWidth, Minecraft.getMinecraft().displayHeight).getScaledWidth();
        guiTop=new ScaledResolution(Minecraft.getMinecraft(), Minecraft.getMinecraft().displayWidth, Minecraft.getMinecraft().displayHeight).getScaledHeight();

        if (skinList==null || skinList.size()==0){
            return;
        }
        currentTransportSkin = entity.getTextureByID(Minecraft.getMinecraft().thePlayer,true, skinList.get(page));

        if(currentTransportSkin ==null){return;}

        switch(guiScreen) {
            case 0:{defineButtons();guiSkinSelect();break;}
        }

    }


    @Override
    public boolean doesGuiPauseGame() {return true;}


/** adds the correct buttons based on which case happens*/
    public void defineButtons(){
        switch (guiScreen){
            case 0:{
                buttonList =new ArrayList();
                buttonList.add(
                    new GUIButton( percentLeft(15)-10,percentTop(56), 20,20,"<<") {
                        @Override
                        public String getHoverText() {
                            return "Previous Page";
                        }
                        @Override
                        public void onClick() {
                            page = (page <= 0 ? entity.getSkinList(Minecraft.getMinecraft().thePlayer, true).keySet().size() -1: page - 1);
                            currentTransportSkin =entity.getSkinList(Minecraft.getMinecraft().thePlayer, true).get(skinList.get(page));
                        }

                        @Override
                        public FontRenderer getFont(){return fontRendererObj;}
                    }
                );

                buttonList.add(
                        new GUIButton( percentLeft(75)-10,percentTop(56), 20,20,">>") {
                            @Override
                            public String getHoverText() {
                                return "Next Page";
                            }
                            @Override
                            public void onClick() {
                                page = (page+1 >= entity.getSkinList(Minecraft.getMinecraft().thePlayer, true).keySet().size() ? 0 : page + 1);
                                currentTransportSkin =entity.getSkinList(Minecraft.getMinecraft().thePlayer, true).get(skinList.get(page));
                            }

                            @Override
                            public FontRenderer getFont(){return fontRendererObj;}
                        }
                );


                buttonList.add(
                        new GUIButton( percentLeft(83)-16,percentTop(56), 32,20,"Apply") {
                            @Override
                            public String getHoverText() {
                                return "Apply Skin";
                            }
                            @Override
                            public void onClick() {
                                if(guiScreen==0) {//TransportSkin select
                                    TrainsInMotion.keyChannel.sendToServer(new PacketPaint(skinList.get(page), entity.getEntityId()));
                                } else if(guiScreen==1){//color select

                                } else {//livery select

                                }
                                entity.renderData.needsModelUpdate=true;
                            }

                            @Override
                            public FontRenderer getFont(){return fontRendererObj;}
                        }
                );


                buttonList.add(
                        new GUIButton( percentLeft(75)-16,percentTop(45), 64,20, "Close") {
                            @Override
                            public String getHoverText() {
                                return "Close Menu";
                            }
                            @Override
                            public void onClick() {
                                mc.displayGuiScreen(null);
                            }

                            @Override
                            public FontRenderer getFont(){return fontRendererObj;}
                        }
                );
                buttonList.add(
                        new GUIButton(percentLeft(83) - 61, percentTop(56), 40, 20, "Random") {
                            @Override
                            public String getHoverText() {
                                return "Randomize Skin";
                            }

                            @Override
                            public void onClick() {
                                TransportSkin random = SkinRegistry.getTransportSkins(entity.getClass()).get(SkinRegistry.getTransportSkins(entity.getClass()).keySet().toArray()[new Random().nextInt(SkinRegistry.getTransportSkins(entity.getClass()).keySet().size())]);
                                page = skinList.indexOf(random.modid + ":" + random.getName());
                                TrainsInMotion.keyChannel.sendToServer(new PacketPaint(skinList.get(page), entity.getEntityId()));
                                mc.displayGuiScreen(null);
                            }

                            @Override
                            public FontRenderer getFont() {
                                return fontRendererObj;
                            }
                        }
                );
                break;
            }
        }
    }




    public void guiSkinSelect(){

        GL11.glPushMatrix();
        GL11.glColor4f(1F, 1F, 1F, 0.5F);
        float offsetFromScreenLeft = width * 0.5f;

        int longest =fontRendererObj.getStringWidth(currentTransportSkin.getName());

        if(currentTransportSkin.getDescription()!=null) {
            for (String s : currentTransportSkin.getDescription()){
                if(fontRendererObj.getStringWidth(s)>longest){
                    longest=fontRendererObj.getStringWidth(s);
                }
            }
        }
        EventManager.drawTooltipBox((int)(width*0.125f),(int)(height*0.65f),(int)(width*0.75f),(int)(height*0.345f),  ClientProxy.WAILA_BGCOLOR, ClientProxy.WAILA_GRADIENT1, ClientProxy.WAILA_GRADIENT2,100);

        EventManager.drawTooltipBox((int)(width*0.175f),(int)(height*0.56f),(int)(width*0.5525f),(int)(height*0.085f),  ClientProxy.WAILA_BGCOLOR, ClientProxy.WAILA_GRADIENT1, ClientProxy.WAILA_GRADIENT2,100);

        fontRendererObj.drawString(CommonUtil.translate(currentTransportSkin.getName()),
                (int)(offsetFromScreenLeft - fontRendererObj.getStringWidth(currentTransportSkin.getName())*0.65f),
                (int)(height*0.59f),ClientProxy.WAILA_FONTCOLOR,false);

        if(currentTransportSkin.getDescription()!=null) {
            for(int i = 0; i< currentTransportSkin.getDescription().length; i++) {
                fontRendererObj.drawString(currentTransportSkin.getDescription()[i],
                        (int) (offsetFromScreenLeft - fontRendererObj.getStringWidth(currentTransportSkin.getDescription()[i]) * 0.5f),
                        (int) ((height * 0.1f) * 7)+(10*i), ClientProxy.WAILA_FONTCOLOR, false);
            }
        }



        EventManager.drawTooltipBox((int)(width*0.125f),(int)(height*0.2f),(int)(width*0.35f),(int)(height*0.35f),  ClientProxy.WAILA_BGCOLOR, ClientProxy.WAILA_GRADIENT1, ClientProxy.WAILA_GRADIENT2,100);

        GL11.glPopMatrix();
        //check scaling Width vs scaling Height, we want the bigger of the two
        float scale = Math.max(entity.getHitboxSize()[0], entity.getHitboxSize()[1]);
        if(scale!=0){
            scale = 0.08f/(scale /0.08f);
        }




        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        GL11.glPushMatrix();
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glLoadIdentity();
        GL11.glTranslatef(-0.4f,0.65f,-1f);

        Project.gluPerspective(2.9F, 1.1f, 0.05f, 2);
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glLoadIdentity();
        RenderHelper.enableStandardItemLighting();
        GL11.glTranslatef(0,0,-0.4f);

        GL11.glRotatef(15,1,0,0);
        GL11.glRotatef(180,0,0,1);
        GL11.glRotatef(field_147073_u+=1,0,1,0);
        if (field_147073_u>360){
            field_147073_u=0;
        }

        GL11.glTranslatef(0,(entity.getHitboxSize()[0]/entity.getHitboxSize()[1])*0.00085f,0);
        GL11.glScalef(scale,scale,scale);

        ClientProxy.transportRenderer.render(entity,0,0,0,0, true,
                currentTransportSkin);
        RenderHelper.disableStandardItemLighting();
        GL11.glPopMatrix();

    }


    @Override
    protected void actionPerformed(GuiButton button) {
        if (button instanceof GUIButton) {
            ((GUIButton) button).onClick();
        }
    }
}