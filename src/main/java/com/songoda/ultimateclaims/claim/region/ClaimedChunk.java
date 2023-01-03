package com.songoda.ultimateclaims.claim.region;

import com.songoda.ultimateclaims.UltimateClaims;
import com.songoda.ultimateclaims.claim.Claim;
import org.bukkit.Chunk;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ClaimedChunk {

    private ClaimedRegion claimedRegion;
    private final String world;
    private final int x;
    private final int z;

    public ClaimedChunk(Chunk chunk) {
        this(chunk.getWorld().getName(), chunk.getX(), chunk.getZ());
    }

    public ClaimedChunk(String world, int x, int z) {
        this.world = world;
        this.x = x;
        this.z = z;
    }
    public String getWorld() {
        return world;
    }

    public int getX() {
        return x;
    }

    public int getZ() {
        return z;
    }

    public boolean isAttached(ClaimedChunk chunk) {
        if (!world.equalsIgnoreCase(chunk.getWorld()))
            return false;
        else if (chunk.getX() == x - 1 && z == chunk.getZ())
            return true;
        else if (chunk.getX() == x + 1 && z == chunk.getZ())
            return true;
        else if (chunk.getX() == x && z == chunk.getZ() - 1)
            return true;
        else return chunk.getX() == x && z == chunk.getZ() + 1;
    }

    public List<ClaimedChunk> getAttachedChunks() {
        List<ClaimedChunk> chunks = new ArrayList<>();

        for (ClaimedChunk chunk : claimedRegion.getChunks()) {
            if (isAttached(chunk)) {
                chunks.add(chunk);
            }
        }
        return chunks;
    }

    public void mergeRegions(Claim claim) {
        for (ClaimedChunk chunk : claim.getClaimedChunks()) {
            ClaimedRegion region = chunk.getRegion();
            if (isAttached(chunk) && region != claimedRegion) {
                claim.removeClaimedRegion(region);
                UltimateClaims.getInstance().getDataManager().deleteClaimedRegion(region);
                claimedRegion.addChunks(region.getChunks());
                UltimateClaims.getInstance().getDataManager().updateClaimedChunks(region.getChunks());
            }
        }
    }

    public ClaimedRegion getAttachedRegion(Claim claim) {
        ClaimedChunk claimedChunk = claim.getClaimedChunks().stream().filter(c -> c.isAttached(this)).findFirst().orElse(null);
        return claimedChunk == null ? null : claimedChunk.getRegion();
    }


    public ClaimedRegion getRegion() {
        return claimedRegion;
    }

    public void setRegion(ClaimedRegion claimedRegion) {
        this.claimedRegion = claimedRegion;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof ClaimedChunk) {
            ClaimedChunk other = (ClaimedChunk) o;
            return this.world.equals(other.world) && this.x == other.x && this.z == other.z;
        } else if (o instanceof Chunk) {
            Chunk other = (Chunk) o;
            return this.world.equals(other.getWorld().getName()) && this.x == other.getX() && this.z == other.getZ();
        } else return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.world, this.x, this.z);
    }
}
