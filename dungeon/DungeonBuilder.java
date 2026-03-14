package com.example.cosmod.dungeon;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class DungeonBuilder {

    private static final int CORRIDOR_W=7, CORRIDOR_H=6, CORRIDOR_L=12;
    private static final int ROOM_W=16, ROOM_H=8, ROOM_L=16;
    private static final int MINIBOSS_W=20, MINIBOSS_H=9, MINIBOSS_L=20;
    private static final int BOSS_W=28, BOSS_H=14, BOSS_L=28;

    private final ServerLevel level;
    private final BlockPos origin;
    private final Random rng = new Random(12345);

    public DungeonBuilder(ServerLevel level, BlockPos origin) {
        this.level=level; this.origin=origin;
    }

    public void build() {
        int x=origin.getX(), y=origin.getY(), z=origin.getZ();
        buildEntrance(x,y,z); z+=4;
        buildCorridor(x,y,z,CORRIDOR_L); z+=CORRIDOR_L;
        buildCombatRoom(x-ROOM_W/2+CORRIDOR_W/2,y,z,ROOM_W,ROOM_H,ROOM_L,1); z+=ROOM_L;
        buildCorridor(x,y,z,CORRIDOR_L); z+=CORRIDOR_L;
        buildCombatRoom(x-ROOM_W/2+CORRIDOR_W/2,y,z,ROOM_W,ROOM_H,ROOM_L,2); z+=ROOM_L;
        buildCorridor(x,y,z,CORRIDOR_L); z+=CORRIDOR_L;
        buildSculptureRoom(x-ROOM_W/2+CORRIDOR_W/2,y,z,ROOM_W,ROOM_H,ROOM_L); z+=ROOM_L;
        buildCorridor(x,y,z,CORRIDOR_L); z+=CORRIDOR_L;
        buildMinibossRoom(x-MINIBOSS_W/2+CORRIDOR_W/2,y,z); z+=MINIBOSS_L;
        buildCorridor(x,y,z,CORRIDOR_L); z+=CORRIDOR_L;
        buildBossRoom(x-BOSS_W/2+CORRIDOR_W/2,y,z);
    }

    private void buildEntrance(int x,int y,int z) {
        for(int dx=-3;dx<=3;dx++) for(int dz=-3;dz<=3;dz++) set(x+dx,y,z+dz,DungeonBlocks.FLOOR_TILE);
        for(int dy=1;dy<=6;dy++){set(x-2,y+dy,z,dy%2==0?DungeonBlocks.ACCENT_POLY:DungeonBlocks.PILLAR);set(x+2,y+dy,z,dy%2==0?DungeonBlocks.ACCENT_POLY:DungeonBlocks.PILLAR);}
        for(int dx=-2;dx<=2;dx++) set(x+dx,y+6,z,DungeonBlocks.WALL);
        set(x,y+7,z,DungeonBlocks.WALL_CHISELED);
        for(int dy=1;dy<=5;dy++) for(int dx=-1;dx<=1;dx++) set(x+dx,y+dy,z,DungeonBlocks.AIR);
        set(x-2,y+5,z+1,DungeonBlocks.LANTERN); set(x+2,y+5,z+1,DungeonBlocks.LANTERN);
    }

    private void buildCorridor(int x,int y,int z,int length) {
        int hw=CORRIDOR_W/2;
        for(int dz=0;dz<length;dz++){
            for(int dx=-hw;dx<=hw;dx++){set(x+dx,y,z+dz,DungeonBlocks.FLOOR);set(x+dx,y+CORRIDOR_H,z+dz,DungeonBlocks.CEILING);}
            for(int dy=1;dy<CORRIDOR_H;dy++){set(x-hw-1,y+dy,z+dz,DungeonBlocks.WALL);set(x+hw+1,y+dy,z+dz,DungeonBlocks.WALL);for(int dx=-hw;dx<=hw;dx++) set(x+dx,y+dy,z+dz,DungeonBlocks.AIR);}
            if(rng.nextInt(10)==0) set(x+rng.nextInt(CORRIDOR_W)-hw,y+CORRIDOR_H-1,z+dz,DungeonBlocks.COBWEB);
        }
        for(int dz=2;dz<length;dz+=4) set(x,y+CORRIDOR_H-1,z+dz,DungeonBlocks.LANTERN);
    }

    private void buildCombatRoom(int x,int y,int z,int w,int h,int l,int roomIdx) {
        buildRoomShell(x,y,z,w,h,l,DungeonBlocks.FLOOR,DungeonBlocks.WALL,DungeonBlocks.CEILING);
        buildPillar(x+1,y,z+1,h-1); buildPillar(x+w-2,y,z+1,h-1); buildPillar(x+1,y,z+l-2,h-1); buildPillar(x+w-2,y,z+l-2,h-1);
        set(x+w/2,y+h-1,z+l/2,DungeonBlocks.SHROOMLIGHT); set(x+w/2,y+h-1,z+2,DungeonBlocks.LANTERN); set(x+w/2,y+h-1,z+l-3,DungeonBlocks.LANTERN); set(x+2,y+h-1,z+l/2,DungeonBlocks.LANTERN); set(x+w-3,y+h-1,z+l/2,DungeonBlocks.LANTERN);
        for(int i=0;i<5;i++) set(x+1+rng.nextInt(w-2),y+1+rng.nextInt(h-2),z,DungeonBlocks.WALL_CRACKED);
        List<BlockPos> doors=new ArrayList<>();
        for(int dy=1;dy<=4;dy++) for(int dx=-1;dx<=1;dx++){doors.add(new BlockPos(x+w/2+dx,y+dy,z));doors.add(new BlockPos(x+w/2+dx,y+dy,z+l-1));}
        AABB rb=new AABB(x+1,y,z+1,x+w-1,y+h,z+l-1);
        DungeonGimmickHandler.COMBAT_ROOMS.add(new DungeonGimmickHandler.RoomData(rb,doors,new BlockPos(x+w/2,y+1,z+l/2),roomIdx));
        openDoorway(x+w/2,y,z,4); openDoorway(x+w/2,y,z+l-1,4);
    }

    private void openDoorway(int cx,int y,int z,int h){for(int dy=1;dy<=h;dy++) for(int dx=-1;dx<=1;dx++) set(cx+dx,y+dy,z,DungeonBlocks.AIR);}

    private void buildSculptureRoom(int x,int y,int z,int w,int h,int l) {
        buildRoomShell(x,y,z,w,h,l,DungeonBlocks.FLOOR_TILE,DungeonBlocks.WALL,DungeonBlocks.CEILING);
        int cx=x+w/2,cz=z+l/2;
        for(int dx=-2;dx<=2;dx++) for(int dz=-2;dz<=2;dz++) set(cx+dx,y+1,cz+dz,DungeonBlocks.ACCENT_POLY);
        set(cx,y+2,cz,Blocks.BONE_BLOCK.defaultBlockState()); set(cx,y+3,cz,Blocks.BONE_BLOCK.defaultBlockState()); set(cx,y+4,cz,Blocks.BONE_BLOCK.defaultBlockState());
        set(cx,y+5,cz,Blocks.CARVED_PUMPKIN.defaultBlockState());
        set(cx-1,y+3,cz,Blocks.BONE_BLOCK.defaultBlockState().setValue(net.minecraft.world.level.block.RotatedPillarBlock.AXIS,net.minecraft.core.Direction.Axis.X));
        set(cx+1,y+3,cz,Blocks.BONE_BLOCK.defaultBlockState().setValue(net.minecraft.world.level.block.RotatedPillarBlock.AXIS,net.minecraft.core.Direction.Axis.X));
        int[][]corners={{x+2,z+2},{x+w-3,z+2},{x+2,z+l-3},{x+w-3,z+l-3}};
        for(int[]c:corners){for(int dy=1;dy<=4;dy++) set(c[0],y+dy,c[1],dy%2==0?DungeonBlocks.ACCENT:DungeonBlocks.PILLAR);set(c[0],y+5,c[1],DungeonBlocks.LANTERN);}
        for(int dx=2;dx<w-2;dx+=2){set(x+dx,y+3,z,DungeonBlocks.WALL_CHISELED);set(x+dx,y+3,z+l-1,DungeonBlocks.WALL_CHISELED);}
        for(int dz=2;dz<l-2;dz+=2){set(x,y+3,z+dz,DungeonBlocks.WALL_CHISELED);set(x+w-1,y+3,z+dz,DungeonBlocks.WALL_CHISELED);}
        set(x+w/2,y+h-1,z+l/2,DungeonBlocks.SHROOMLIGHT);
        openDoorway(x+w/2,y,z,4); openDoorway(x+w/2,y,z+l-1,4);
    }

    private void buildMinibossRoom(int x,int y,int z) {
        int w=MINIBOSS_W,h=MINIBOSS_H,l=MINIBOSS_L;
        buildRoomShell(x,y,z,w,h,l,DungeonBlocks.FLOOR_TILE,DungeonBlocks.WALL,DungeonBlocks.CEILING);
        int cx=x+w/2,cz=z+l/2;
        buildGrandPillar(x+1,y,z+1,h); buildGrandPillar(x+w-3,y,z+1,h); buildGrandPillar(x+1,y,z+l-3,h); buildGrandPillar(x+w-3,y,z+l-3,h);
        for(int dx=2;dx<w-2;dx+=3){set(x+dx,y+h-1,z,DungeonBlocks.GILDED);set(x+dx,y+h-1,z+l-1,DungeonBlocks.GILDED);}
        for(int dx=3;dx<w-3;dx+=4) for(int dz=3;dz<l-3;dz+=4) set(x+dx,y+h-1,z+dz,DungeonBlocks.SHROOMLIGHT);
        List<BlockPos> mbDoors=new ArrayList<>();
        for(int dy=1;dy<=4;dy++) for(int dx=-1;dx<=1;dx++){mbDoors.add(new BlockPos(x+w/2+dx,y+dy,z));mbDoors.add(new BlockPos(x+w/2+dx,y+dy,z+l-1));}
        AABB mb=new AABB(x+1,y,z+1,x+w-1,y+h,z+l-1); BlockPos mc=new BlockPos(x+w/2,y+1,z+l/2);
        DungeonGimmickHandler.COMBAT_ROOMS.add(new DungeonGimmickHandler.RoomData(mb,mbDoors,mc,4));
        DungeonGimmickHandler.MINIBOSS_ROOMS.add(new DungeonGimmickHandler.RoomData(mb,mbDoors,mc,4));
        openDoorway(x+w/2,y,z,4); openDoorway(x+w/2,y,z+l-1,4);
    }

    private void buildBossRoom(int x,int y,int z) {
        int w=BOSS_W,h=BOSS_H,l=BOSS_L;
        for(int dx=0;dx<w;dx++) for(int dz=0;dz<l;dz++){boolean e=dx<2||dx>=w-2||dz<2||dz>=l-2;boolean d=(dx+dz)%4==0;set(x+dx,y,z+dz,e?DungeonBlocks.BOSS_PILLAR:d?DungeonBlocks.BOSS_ACCENT:DungeonBlocks.BOSS_FLOOR);}
        for(int dx=0;dx<w;dx++) for(int dz=0;dz<l;dz++){boolean c=Math.abs(dx-w/2)<5&&Math.abs(dz-l/2)<5;set(x+dx,y+h,z+dz,c?DungeonBlocks.SHROOMLIGHT:DungeonBlocks.BOSS_WALL);}
        for(int dy=1;dy<h;dy++){BlockState wl=dy%4==0?DungeonBlocks.BOSS_ACCENT:DungeonBlocks.BOSS_WALL;for(int dx=0;dx<w;dx++){set(x+dx,y+dy,z,wl);set(x+dx,y+dy,z+l-1,wl);}for(int dz=0;dz<l;dz++){set(x,y+dy,z+dz,wl);set(x+w-1,y+dy,z+dz,wl);}}
        for(int dx=1;dx<w-1;dx++) for(int dz=1;dz<l-1;dz++) for(int dy=1;dy<h;dy++) set(x+dx,y+dy,z+dz,DungeonBlocks.AIR);
        int[]pz={z+2,z+l/2-1,z+l-4};for(int p:pz){buildBossPillar(x+1,y,p,h);buildBossPillar(x+w-2,y,p,h);}
        for(int dx=2;dx<w-2;dx++){set(x+dx,y+h-1,z+2,DungeonBlocks.BOSS_WALL);set(x+dx,y+h-1,z+l-4,DungeonBlocks.BOSS_WALL);}
        for(int dz=l/4;dz<=3*l/4;dz+=l/4){for(int dy=1;dy<=4;dy++){set(x+1,y+dy,z+dz,DungeonBlocks.AIR);set(x+w-2,y+dy,z+dz,DungeonBlocks.AIR);}set(x+1,y+5,z+dz,DungeonBlocks.LANTERN);set(x+w-2,y+5,z+dz,DungeonBlocks.LANTERN);}
        int cx=x+w/2,cz=z+l/2;
        for(int dx=-4;dx<=4;dx++) for(int dz=-4;dz<=4;dz++) set(cx+dx,y+1,cz+dz,DungeonBlocks.BOSS_FLOOR);
        for(int dx=-2;dx<=2;dx++) for(int dz=-2;dz<=2;dz++) set(cx+dx,y+2,cz+dz,DungeonBlocks.BOSS_FLOOR);
        for(int dx=-4;dx<=4;dx++){set(cx+dx,y+2,cz-4,DungeonBlocks.BOSS_ACCENT);set(cx+dx,y+2,cz+4,DungeonBlocks.BOSS_ACCENT);}
        for(int dz=-4;dz<=4;dz++){set(cx-4,y+2,cz+dz,DungeonBlocks.BOSS_ACCENT);set(cx+4,y+2,cz+dz,DungeonBlocks.BOSS_ACCENT);}
        for(int dx=-6;dx<=6;dx++) for(int dz=-6;dz<=6;dz++) if((Math.abs(dx)>=5||Math.abs(dz)>=5)&&Math.abs(dx)<=6&&Math.abs(dz)<=6) set(cx+dx,y,cz+dz,DungeonBlocks.LAVA);
        for(int dx=-2;dx<=2;dx++) for(int dz=-2;dz<=2;dz++) set(cx+dx,y+h,cz+dz,DungeonBlocks.SHROOMLIGHT);
        List<BlockPos> bd=new ArrayList<>();
        for(int dy=1;dy<=5;dy++) for(int dx=-2;dx<=2;dx++) bd.add(new BlockPos(x+w/2+dx,y+dy,z+l-2));
        DungeonGimmickHandler.BOSS_DOORS=bd;
        DungeonGimmickHandler.BOSS_CHESTS=new java.util.ArrayList<>();
        DungeonGimmickHandler.PORTAL_POS=new BlockPos(x+w/2,y+1,z+l-3);
        openDoorway(x+w/2,y,z,5);
        for(int dy=1;dy<=5;dy++) for(int dx=-2;dx<=2;dx++) set(x+w/2+dx,y+dy,z+l-2,DungeonBlocks.IRON_BARS);
    }

    private void buildRoomShell(int x,int y,int z,int w,int h,int l,BlockState floor,BlockState wall,BlockState ceil){
        for(int dx=0;dx<w;dx++) for(int dz=0;dz<l;dz++){set(x+dx,y,z+dz,floor);set(x+dx,y+h,z+dz,ceil);}
        for(int dy=1;dy<h;dy++){for(int dx=0;dx<w;dx++){set(x+dx,y+dy,z,wall);set(x+dx,y+dy,z+l-1,wall);}for(int dz=0;dz<l;dz++){set(x,y+dy,z+dz,wall);set(x+w-1,y+dy,z+dz,wall);}}
        for(int dx=1;dx<w-1;dx++) for(int dz=1;dz<l-1;dz++) for(int dy=1;dy<h;dy++) set(x+dx,y+dy,z+dz,DungeonBlocks.AIR);
    }
    private void buildPillar(int x,int y,int z,int h){for(int dy=0;dy<=h;dy++) set(x,y+dy,z,dy==0||dy==h?DungeonBlocks.PILLAR_TOP:DungeonBlocks.PILLAR);}
    private void buildGrandPillar(int x,int y,int z,int h){for(int dy=0;dy<=h;dy++){BlockState s=(dy==0||dy==h)?DungeonBlocks.WALL_CHISELED:(dy%3==0)?DungeonBlocks.ACCENT_POLY:DungeonBlocks.PILLAR;set(x,y+dy,z,s);set(x+1,y+dy,z,DungeonBlocks.PILLAR);set(x,y+dy,z+1,DungeonBlocks.PILLAR);set(x+1,y+dy,z+1,DungeonBlocks.PILLAR);}}
    private void buildBossPillar(int x,int y,int z,int h){for(int dy=0;dy<=h;dy++){BlockState s=(dy%3==0)?DungeonBlocks.BOSS_ACCENT:DungeonBlocks.BOSS_PILLAR;set(x,y+dy,z,s);}set(x,y+1,z,DungeonBlocks.BOSS_ACCENT);set(x,y+h-1,z,DungeonBlocks.BOSS_ACCENT);set(x,y+h-2,z+1,DungeonBlocks.LANTERN);}
    private void set(int x,int y,int z,BlockState s){level.setBlock(new BlockPos(x,y,z),s,3);}
}
