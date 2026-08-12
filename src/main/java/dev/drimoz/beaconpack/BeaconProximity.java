package dev.drimoz.beaconpack;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BeaconBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;

/**
 * Whether the player is standing near a lit beacon.
 *
 * <p>Used by the {@code require_beacon_to_configure} option, the strictest of the three safeguards
 * that keep the vanilla beacon on the progression path rather than replaced by its portable version.
 */
public final class BeaconProximity {

    public static final int RADIUS = 16;

    /**
     * Only checks that a beacon is lit, not how tall its pyramid is: the pyramid level is not
     * exposed by the block entity, and gating on "a working beacon exists here" already carries the
     * intent.
     */
    public static boolean activeBeaconNear(Player player) {
        Level level = player.level();
        BlockPos centre = player.blockPosition();

        int minChunkX = SectionPos.blockToSectionCoord(centre.getX() - RADIUS);
        int maxChunkX = SectionPos.blockToSectionCoord(centre.getX() + RADIUS);
        int minChunkZ = SectionPos.blockToSectionCoord(centre.getZ() - RADIUS);
        int maxChunkZ = SectionPos.blockToSectionCoord(centre.getZ() + RADIUS);

        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                LevelChunk chunk = level.getChunkSource().getChunkNow(chunkX, chunkZ);
                if (chunk == null) {
                    continue;
                }
                for (BlockEntity entity : chunk.getBlockEntities().values()) {
                    if (entity instanceof BeaconBlockEntity beacon
                            && !beacon.getBeamSections().isEmpty()
                            && beacon.getBlockPos().closerThan(centre, RADIUS)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private BeaconProximity() {}
}
