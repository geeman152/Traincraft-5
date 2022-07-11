package train.render;

import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import train.render.models.blocks.ModelWindMill;
import train.render.models.blocks.ModelWindMillWheel;

public class RenderWindMill extends TileEntitySpecialRenderer {

	private static final ModelWindMill modelWindMill = new ModelWindMill();
	private static final ModelWindMillWheel modelWindMillWheel = new ModelWindMillWheel();

	@Override
	public void render(TileEntity tileEntity, double x, double y, double z, float tick, int destroyStage, float alp) {
		if(tileEntity !=null) {
			modelWindMill.render(tileEntity, x, y, z);
			modelWindMillWheel.render(tileEntity, x, y, z);
		}
	}
}
