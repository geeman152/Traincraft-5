package train.core;

import cpw.mods.fml.client.registry.RenderingRegistry;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.registry.VillagerRegistry;
import javazoom.jl.decoder.JavaLayerUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.SoundCategory;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import train.blocks.bench.GuiTrainCraftingBlock;
import train.blocks.bench.TileTrainWbench;
import train.blocks.distil.GuiDistil;
import train.blocks.distil.TileEntityDistil;
import train.blocks.generator.GuiGeneratorDiesel;
import train.blocks.generator.TileGeneratorDiesel;
import train.blocks.hearth.GuiOpenHearthFurnace;
import train.blocks.hearth.TileEntityOpenHearthFurnace;
import train.blocks.lantern.GuiLantern;
import train.blocks.lantern.TileLantern;
import train.core.handlers.ClientTickHandler;
import train.core.handlers.TCInputHandler;
import train.core.helpers.JLayerHook;
import train.entity.gui.GuiJukebox;
import train.entity.gui.GuiZepp;
import train.entity.gui.HUDloco;
import train.entity.rollingStock.EntityJukeBoxCart;
import train.entity.zeppelin.EntityZeppelinOneBalloon;
import train.entity.zeppelin.EntityZeppelinTwoBalloons;
import train.library.GuiIDs;
import train.library.Info;
import train.render.RenderZeppelins;
import train.render.models.ModelSwitchStandOff;
import train.render.models.ModelSwitchStandOn;

import java.util.Calendar;

public class ClientProxy extends CommonProxy {

	//NOTE: these had to be moved here rather than the render, because they wouldn't init soon enough in the TESR and crashed
    public static ModelSwitchStandOn modelSwitch = new ModelSwitchStandOn();
    public static ModelSwitchStandOff modelSwitch2 = new ModelSwitchStandOff();

    public static boolean isHoliday() {
		Calendar cal = Calendar.getInstance();
		return(cal.get(Calendar.MONTH) == Calendar.DECEMBER || (cal.get(Calendar.MONTH) == Calendar.JANUARY) && cal.get(Calendar.DATE) < 7);
	}

	@Override
	public void registerEvents(FMLPreInitializationEvent event) {
		super.registerEvents(event);
		registerEvent(new ClientTickHandler());
		registerEvent(new HUDloco());
	}

	@Deprecated //TESR should be handled as noted in TCBlocks.init()
	@Override
	public void registerRenderInformation() {
		FMLCommonHandler.instance().bus().register(new ClientTickHandler());

		RenderingRegistry.registerEntityRenderingHandler(EntityZeppelinTwoBalloons.class, new RenderZeppelins());
		RenderingRegistry.registerEntityRenderingHandler(EntityZeppelinOneBalloon.class, new RenderZeppelins());

	}

	@Override
	public Object getClientGuiElement(int ID, EntityPlayer player, World world, int x, int y, int z) {
    	if(player==null){return null;}

		if(ID==GuiIDs.ZEPPELIN){
			Entity entity = player.ridingEntity;
			return new GuiZepp(player.inventory, entity);
		} else if (ID==GuiIDs.JUKEBOX){
			Entity entity1 = getEntity(world, x);
			if(entity1 instanceof EntityJukeBoxCart) {
				return new GuiJukebox(player, (EntityJukeBoxCart) entity1);
			}
		}

		return null;
	}

	@Override
	public int addArmor(String armor) {
		return RenderingRegistry.addNewArmourRendererPrefix(armor);
	}

	@Override
	public void registerVillagerSkin(int villagerId, String textureName) {
		VillagerRegistry.instance().registerVillagerSkin(villagerId, new ResourceLocation(Info.resourceLocation,Info.villagerPrefix + textureName));
	}

	@Override
	public Minecraft getMinecraft() {
		return Minecraft.getMinecraft();
	}
	
	@Override
	public EntityPlayer getPlayer() {
		return getMinecraft().thePlayer;
	}

	
	@Override
	public float getJukeboxVolume() {
		return Minecraft.getMinecraft().gameSettings.getSoundLevel(SoundCategory.RECORDS) * Minecraft.getMinecraft().gameSettings.getSoundLevel(SoundCategory.MASTER);
	}
	@Override
	public void registerKeyBindingHandler() {
            FMLCommonHandler.instance().bus().register(TCInputHandler.instance);
            MinecraftForge.EVENT_BUS.register(TCInputHandler.instance);
    }
	
	@Override
	public void setHook() {
		JavaLayerUtils.setHook(new JLayerHook(Minecraft.getMinecraft()));
	}
}