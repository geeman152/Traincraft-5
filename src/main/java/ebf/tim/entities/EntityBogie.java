package ebf.tim.entities;


import com.mojang.authlib.GameProfile;
import cpw.mods.fml.common.registry.IEntityAdditionalSpawnData;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import ebf.tim.blocks.rails.BlockRailCore;
import ebf.tim.utility.CommonUtil;
import fexcraft.tmt.slim.Vec3f;
import io.netty.buffer.ByteBuf;
import mods.railcraft.api.carts.IMinecart;
import mods.railcraft.api.carts.IRoutableCart;
import mods.railcraft.api.tracks.ITrackSwitch;
import mods.railcraft.api.tracks.ITrackTile;
import net.minecraft.block.Block;
import net.minecraft.block.BlockAir;
import net.minecraft.block.BlockRailBase;
import net.minecraft.entity.item.EntityMinecart;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;

/**
 * <h1>Bogie Core</h1>
 * this controls the behavior of the bogies in trains and rollingstock.
 * @author Eternal Blue Flame
 */
public class EntityBogie extends EntityMinecart implements IMinecart, IRoutableCart, IEntityAdditionalSpawnData {

    /** used to keep a reference to the parent train/rollingstock.*/
    private int parentId = 0;
    /**defines if this is the front bogie of the transport*/
    private boolean isFront=true;
    /**used to calculate the X/Y/Z velocity based on the direction the rail is facing, similar to how vanilla minecarts work.*/
    private static final double[][][] martix = new double[][][] {
            //straight
            {{0, -0.5}, {0, 0.5}, {0, -1}},
            {{ -0.5, 0}, {0.5, 0}, {-1, 0}},
            //slope
            {{ -0.5, 0}, {0.5, 0}, {-1, 0}},
            {{ -0.5, 0}, {0.5, 0}, {-1, 0}},
            {{0, -0.5}, {0, 0.5}, {0, -1}},
            {{0, -0.5}, {0, 0.5}, {0, -1}},
            //turns
            {{0, 0.5}, {0.5, 0}, {-0.5, 0.5}},
            {{0, 0.5}, { -0.5, 0}, {0.5, 0.5}},
            {{0, -0.5}, { -0.5, 0}, {0.5, -0.5}},
            {{0, -0.5}, {0.5, 0}, {-0.5, -0.5}}
    };

    /**cached values for the rail path and motion vectors
     * prevents need to generate a new variable multiple times per tick and reduces GC strain*/
    private double railPathX=0, railPathZ=0,motionSqrt,railPathX2, railPathZ2;
    private int railMetadata, xFloor=0,yFloor=0,zFloor=0;
    private Block blockNext, blockCurrent;
    float railmax;

    double[] velocity = new double[]{0,0,0,0,0,0};
    /**normally this variable exists already in 1.7, this additional declaration of it is support for 1.8.9+*/
    public float yOffset=0.425f;


    public EntityBogie(World world) {
        super(world);
    }

    /**
     * used to initialize the entity
     * @param parent The EntityID of the parent entity (Must extend GenericRailTransport).
     * @param front whether or not this is the front bogie.
     */
    public EntityBogie(World world, double xPos, double yPos, double zPos, int parent, boolean front) {
        super(world);
        posX = xPos;
        posY = yPos;
        posZ = zPos;
        parentId = parent;
        isFront = front;
    }

    public World getWorld(){return worldObj;}

    /**Small networking check to add the bogie to the host train/rollingstock. Or to remove the bogie from the world if the host doesn't exist.*/
    @Override
    public void readSpawnData(ByteBuf additionalData) {
        isFront = additionalData.readBoolean();
        parentId = additionalData.readInt();
    }
    /**sends the networking check on spawn/respawn so this can see if it should exist in the first place.*/
    @Override
    public void writeSpawnData(ByteBuf buffer) {
        buffer.writeBoolean(isFront);
        buffer.writeInt(parentId);
    }

    /**plays a sound during entity movement*/
    @Override
    protected void func_145780_a(int p_145780_1_, int p_145780_2_, int p_145780_3_, Block p_145780_4_) {}

    /**used by the game to tell different types of minecarts from eachother, this doesnt effect us, so just use something random*/
    @Override
    public int getMinecartType() {
        return 10001;
    }
    /**returns if the cart can make itself move*/
    @Override
    public boolean isPoweredCart() {
        return true;
    }
    /**returns if the cart can be ridden*/
    @Override
    public boolean canBeRidden() {
        return false;
    }
    /**if the cart can be pushed by an entity*/
    @Override
    public boolean canBePushed() {
        return false;
    }
    /**returns if the rider can interact with this, I don't think this makes any difference given the design of the bogie*/
    @Override
    public boolean canRiderInteract() {
        return false;
    }
    /**defines the max speed the minecart can move on a rail that is not an extension of BlockRailBase*/
    @Override
    public float getMaxCartSpeedOnRail() {
        return 1.2F;
    }
    /**defines the update tick of the entity, in this case we rely on the transport to provide that for us, keeps things synced on the chance entities ever get individualized threads*/
    @Override
    public void onUpdate() {

        //client only, update position
        if (this.getWorld().isRemote){
            super.onUpdate();
        }
        //init stuff for detector/activator rails
        if(yFloor==0) {
            xFloor = CommonUtil.floorDouble(this.posX);
            yFloor = CommonUtil.floorDouble(this.posY);
            zFloor = CommonUtil.floorDouble(this.posZ);
            blockCurrent=CommonUtil.getBlockAt(getWorld(),xFloor,yFloor,zFloor);
        }

        //do detector rail things
        if(blockCurrent instanceof BlockRailBase && ((BlockRailBase)blockCurrent).isPowered()){
            int meta = CommonUtil.getRailMeta(getWorld(),this,xFloor,yFloor,zFloor);
            if((meta & 8) == 0) {
                CommonUtil.setBlockMeta(getWorld(), xFloor,yFloor,zFloor, meta | 8);
            }
        }

        //todo: reroute to host for activator rail stuff, activator rail activates train.
        //host would also need identification if it was front or back bogie
        if (blockCurrent == Blocks.activator_rail) {
            this.onActivatorRailPass(xFloor,yFloor,zFloor,
                    (CommonUtil.getRailMeta(getWorld(),this,xFloor,yFloor,zFloor) & 8) != 0);
        }

        if(ticksExisted%40==0 || ticksExisted==0) {
            //be sure to remove this if the parent is null, or in a different castle, I mean world.
            if (getWorld().getEntityByID(parentId) instanceof GenericRailTransport) {
                if (getWorld().isRemote) {
                    ((GenericRailTransport) getWorld().getEntityByID(parentId)).setBogie(this, isFront);
                }
            } else {
                getWorld().removeEntity(this);
            }
        }
    }
    /**returns if this can be collided with, since we don't process collisions, we return false*/
    @Override
    public boolean canBeCollidedWith() {
        return true;
    }
    /**disables reading from NBT*/
    @Override
    public void readFromNBT(NBTTagCompound tag){}
    /**disables writing to NBT, which kills the entity on game end.*/
    @Override
    public void writeToNBT(NBTTagCompound tag){}
    @Override
    public boolean writeToNBTOptional(NBTTagCompound tagCompound){return false;}
    @Override
    public boolean writeMountToNBT(NBTTagCompound tagCompound){return false;}


    public void addVelocity(GenericRailTransport host, double speed){
        //cache rotation so it only has to be processed once per tick
        if(velocity[4]==0 && velocity[5]==0){
            Vec3f vec = CommonUtil.rotatePoint(new Vec3f(1,0,0),0,host.rotationYaw,0);
            velocity[4]=vec.xCoord;
            velocity[5]=vec.zCoord;
        }

        velocity[0]+=speed*velocity[4];
        velocity[1]+=speed*velocity[5];
    }

    public void addLinking(GenericRailTransport host, double speed){
        //cache rotation so it only has to be processed once per tick
        if(velocity[4]==0 && velocity[5]==0){
            Vec3f vec = CommonUtil.rotatePoint(new Vec3f(1,0,0),0,host.rotationYaw,0);
            velocity[4]=vec.xCoord;
            velocity[5]=vec.zCoord;
        }

        velocity[2]+=speed*velocity[4];
        velocity[3]+=speed*velocity[5];
    }

    public void drag(GenericRailTransport host, double drag){
        velocity[0]*=drag;
        velocity[1]*=drag;
    }

    /**
     * <h2> movement management</h2>
     * this is modified movement from the super class, should be more efficient, and reliable, but generally does the same thing, minus ability to collide.
     * @see EntityMinecart#onUpdate()
     * Some features are replaced using our own for compatibility with ZoraNoDensha
     * @see CommonUtil
     * returns true or false depending on whether or not it derails from having no rail.
     */
    public void minecartMove(GenericRailTransport host) {
        //server only
        if(!getWorld().isRemote) {

            //prevent moving without velocity
            if (Math.abs(velocity[0]) + Math.abs(velocity[1] + Math.abs(velocity[2]) + Math.abs(velocity[3])) < 0.0000001) {
                //return;
            }

            //reset rotation
            velocity[4]=0;velocity[5]=0;
            //update old position, add the gravity, and get the block below this,
            this.prevPosX = this.posX;
            this.prevPosY = this.posY;
            this.prevPosZ = this.posZ;

            Block block = CommonUtil.getBlockAt(getWorld(), xFloor, yFloor, zFloor);
            //todo: if (block instanceof BlockRailCore) {
            //update using spline movement
            //} else if (block instanceof BlockRailBase)

            if(!(block instanceof BlockRailBase)){
                if(CommonUtil.getBlockAt(getWorld(), xFloor, yFloor+1, zFloor) instanceof BlockRailBase) {
                    prevPosY=posY;
                    posY++;
                    yFloor++;
                    block = CommonUtil.getBlockAt(getWorld(), xFloor, yFloor, zFloor);
                } else if(CommonUtil.getBlockAt(getWorld(), xFloor, yFloor-1, zFloor) instanceof BlockRailBase) {
                    prevPosY=posY;
                    posY--;
                    yFloor--;
                    block = CommonUtil.getBlockAt(getWorld(), xFloor, yFloor, zFloor);
                } else if(block instanceof BlockAir) {
                    moveOffRail();
                }
            }

            //update on normal rails
            if (block instanceof BlockRailBase) {
                this.yOffset=(block instanceof BlockRailCore?0.425f:0.3425f);
                loopVanilla(host, Math.abs(velocity[0])+Math.abs(velocity[1])+Math.abs(velocity[2])+Math.abs(velocity[3]), (BlockRailBase) block);
                //update on ZnD rails, and ones that don't extend block rail base.
                //todo ZnD support, either by jar reference or API update
            //} else if (block instanceof ITrackBase) {
                //update position for ZnD rails.
                //moveBogieZnD(motionX, motionZ, floorX, floorY, floorZ, (ITrackBase) block);
            }
           velocity[2]=0;velocity[3]=0;
        }
    }

    private void moveOffRail(){
        railPathZ=0;railPathX=0;
        double moveLength = Math.abs(velocity[0]) + Math.abs(velocity[1] + Math.abs(velocity[2]) + Math.abs(velocity[3]));
        while (moveLength>0) {
            posX+=Math.min(0.35, velocity[0]+velocity[2]);
            posZ+=Math.min(0.35, velocity[1]+velocity[3]);
            moveLength -= 0.35;

            //update Y position and check for collision
            if(xFloor!=CommonUtil.floorDouble(this.posX) || zFloor!=CommonUtil.floorDouble(posZ)) {
                xFloor = CommonUtil.floorDouble(this.posX);
                yFloor = CommonUtil.floorDouble(this.posY);
                zFloor = CommonUtil.floorDouble(this.posZ);
                //fall
                if (CommonUtil.getBlockAt(getWorld(), xFloor, yFloor - 1, zFloor) instanceof BlockAir) {
                    posY--;
                }
            }
        }
    }

    private void loopVanilla(GenericRailTransport host, double moveLength, BlockRailBase block){

        //try to adhere to limiter track
        railmax = block.getRailMaxSpeed(getWorld(),this,xFloor, yFloor, zFloor);
        Block blockUp;
        if(railmax!=0.4f){
            moveLength=Math.min(moveLength,railmax);
        }
        railMetadata = CommonUtil.getRailMeta(getWorld(), this, xFloor, yFloor, zFloor);
        //actually move
        while (moveLength>0) {
            moveBogieVanilla(Math.min(0.3, moveLength));
            moveLength -= 0.3;

            //update the last used block to the one we just used, if it's actually different.
            if(xFloor!=CommonUtil.floorDouble(this.posX) || zFloor != CommonUtil.floorDouble(this.posZ)) {
                xFloor = CommonUtil.floorDouble(this.posX);
                yFloor = CommonUtil.floorDouble(this.posY);
                zFloor = CommonUtil.floorDouble(this.posZ);
                blockCurrent=CommonUtil.getBlockAt(getWorld(),xFloor,yFloor,zFloor);
                //check for collisions and skip update
                for (int i = 1; i < host.getHitboxSize()[1] - 1; i++) {
                    blockUp = CommonUtil.getBlockAt(getWorld(), xFloor, yFloor + i, zFloor);
                    if (!(blockUp instanceof BlockAir)) {
                        host.backBogie.motionX=0;
                        host.backBogie.motionZ=0;
                        host.frontBogie.motionX=0;
                        host.frontBogie.motionZ=0;
                        return;
                    }
                }
                //handle slope movement before other interactions
                if(!CommonUtil.isRailBlockAt(getWorld(), xFloor, yFloor, zFloor)){
                    this.prevPosY =posY;
                    if(CommonUtil.isRailBlockAt(getWorld(), xFloor, yFloor+1, zFloor)){
                        posY++;
                    } else if (CommonUtil.isRailBlockAt(getWorld(), xFloor, yFloor-1, zFloor)) {
                        posY--;
                    }
                    yFloor = CommonUtil.floorDouble(this.posY);
                }

                blockNext = CommonUtil.getBlockAt(getWorld(), xFloor, yFloor, zFloor);
                //now loop this again for the next increment of movement, if there is one
                if (blockNext instanceof BlockRailBase) {
                    block = (BlockRailBase) blockNext;
                    //do the rail functions.
                    if(shouldDoRailFunctions()) {
                        block.onMinecartPass(getWorld(), this, xFloor, yFloor, zFloor);
                    }
                    //get the direction of the rail from it's metadata
                    railMetadata = CommonUtil.getRailMeta(getWorld(), this, xFloor, yFloor, zFloor);
                }
                //get the direction of the rail from it's metadata
                else if (getWorld().getTileEntity(xFloor, yFloor, zFloor) instanceof ITrackTile && (((ITrackTile)getWorld().getTileEntity(xFloor, yFloor, zFloor)).getTrackInstance() instanceof ITrackSwitch)){
                    railMetadata = CommonUtil.getRailMeta(getWorld(),this,xFloor, yFloor, zFloor);//railcraft support
                }
            }
        }
    }


    private void moveBogieVanilla(double currentMotion){
        if(Math.abs(currentMotion)<0.000001){return;}
        //figure out the current rail's direction
        railPathX = (martix[railMetadata][2][0]);
        railPathZ = (martix[railMetadata][2][1]);

        //cover moving reverse of track direction using the rotation from the closed loop rather than the full motion
        if((velocity[0]+velocity[2]) * railPathX + (velocity[1]+velocity[3]) * railPathZ <= 0.0D) {
            railPathX = -railPathX;
            railPathZ = -railPathZ;
        }

        setPositionRelative((currentMotion * railPathX), 0, (currentMotion * railPathZ));

        motionSqrt = Math.abs(velocity[0])+Math.abs(velocity[1]);
        velocity[0] = (float)(motionSqrt * railPathX);
        velocity[1] = (float)(motionSqrt * railPathZ);

        motionSqrt = Math.abs(velocity[2])+Math.abs(velocity[3]);
        velocity[2] = (float)(motionSqrt * railPathX);
        velocity[3] = (float)(motionSqrt * railPathZ);

        motionSqrt = Math.abs(velocity[0])+Math.abs(velocity[1])+Math.abs(velocity[2])+Math.abs(velocity[3]);

        //define the rail path again, to center the transport.
        railPathX2 = xFloor + 0.5D + martix[railMetadata][0][0];
        railPathZ2 = zFloor + 0.5D + martix[railMetadata][0][1];
        railPathX = (xFloor + 0.5D + martix[railMetadata][1][0]) - railPathX2;
        railPathZ = (zFloor + 0.5D + martix[railMetadata][1][1]) - railPathZ2;

        //based on the path direction, try to center the bogie on the track
        if (railPathX == 0.0D) {
            motionSqrt = this.posZ - zFloor;
        } else if (railPathZ == 0.0D) {
            motionSqrt = this.posX - xFloor;
        } else {
            motionSqrt = ((this.posX - railPathX2) * railPathX + (this.posZ - railPathZ2) * railPathZ) * 2.0D;
        }
        //do the centering movement
        setPosition((railPathX2 + railPathX * motionSqrt), posY, (railPathZ2 + railPathZ * motionSqrt));
    }


    /*
    //todo ZnD support, either by jar reference or API update
    private void moveBogieZnD(double currentMotionX, double currentMotionZ, int floorX, int floorY, int floorZ, ITrackBase track){
        double[][] posVec6 = track.getPositionOnTrack(this);
        posX = posVec6[0][0];
        posY = posVec6[0][1];
        posZ = posVec6[0][2];
        //6[0] is xyz
        //6[1] is rotations
        //System.out.println(track.getDirectionOfSection().toString() + ":::" + track.getOrientation());
    }*/



    /*
     * <h1>Railcraft Functionality</h1>
     */


    /**checks if the parent transport matches the filter for railcraft items, if the filter is null or the parent is null, return false.*/
    @Override
    public boolean doesCartMatchFilter(ItemStack stack, EntityMinecart cart) {
        if (stack == null || getWorld().getEntityByID(parentId) == null) {
            return false;
        } else {
            Item cartItem = ((GenericRailTransport)getWorld().getEntityByID(parentId)).getItem();
            return cartItem != null && stack.getItem() == cartItem;
        }
    }
    /**returns the gameprofile of the owner for railcraft.*/
    @Override
    public GameProfile getOwner(){
        if (getWorld().getEntityByID(parentId) instanceof GenericRailTransport){
            GenericRailTransport parent = (GenericRailTransport) getWorld().getEntityByID(parentId);
            if (parent.getOwner() != null){
                return parent.getOwner();
            }
        }
        return null;
    }
    /**returns the destination of the parent for railcraft*/
    @Override
    public String getDestination() {
        if (getWorld().getEntityByID(parentId) instanceof GenericRailTransport){
            GenericRailTransport parent = (GenericRailTransport) getWorld().getEntityByID(parentId);
            if (parent.getOwner() != null){
                return parent.destination;
            }
        }
        return "";
    }
    /**this is supposed to set the ticket for the railcraft destination, but we don't use that, so we return false and do nothing.*/
    @Override
    public boolean setDestination(ItemStack ticket) {
        return false;
    }


    /*
     * <h2>Client Movement code</h2>
     */

    /**used to update positioning on client, and update the velocity multiplier*/
    @Override
    @SideOnly(Side.CLIENT)
    public void setPositionAndRotation2(double x, double y, double z, float yaw, float pitch, int turnProgress) {
        super.setPositionAndRotation2(x,y,z,yaw,pitch,turnProgress);
    }


    @Override
    public void setVelocity(double x, double y, double z) {
        motionX=(((long)(x*1000000))*0.000001);
        motionY=y==0?0:(((long)(y*1000000))*0.000001);
        motionZ=(((long)(z*1000000))*0.000001);


        isAirBorne = true;
    }
    /**used to add to the current velocity movement, also sets this as airborne*/
    @Override
    public void addVelocity(double velocityX, double velocityY, double velocityZ){
        setVelocity(motionX + velocityX, motionY + velocityY, motionZ + velocityZ);
    }
    /**override of the super method just so we can set the position without updating the hitbox, because we don't need to.*/
    @Override
    public void setPosition(double x, double y, double z) {
        posX=x;
        posY=y;
        posZ=z;
    }


    public void setPositionRelative(double x, double y, double z) {
        posX+=((int)(x*10000))*0.0001;
        if(y!=0) {//usually we won't be changing this, so this is more efficient
            posY += ((int) (y * 10000)) * 0.0001;
        }
        posZ+=((int)(z*10000))*0.0001;
    }


}