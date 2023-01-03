package com.songoda.ultimateclaims.claim.region;

public class ClaimCorners {
    private final String chunkID;
    private final double[] x, z;

    public ClaimCorners(ClaimedChunk chunk, double[] x, double[] z) {
        this.chunkID = chunk.getX() + ";" + chunk.getZ();

        this.x = x;
        this.z = z;
    }

    public String getChunkID() {
        return chunkID;
    }

    public double[] getX() {
        return x;
    }

    public double[] getZ() {
        return z;
    }
}