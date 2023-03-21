package me.ryanhamshire.GriefPrevention.util;

import java.util.Random;
import java.util.UUID;

public class UUIDUtil {

    public static UUID fastRandomUUID(Random random) {
        byte[] randomBytes = new byte[16];
        random.nextBytes(randomBytes);

        randomBytes[6]  &= 0x0F;  /* clear version        */
        randomBytes[6]  |= 0x40;  /* set to version 4     */
        randomBytes[8]  &= 0x3F;  /* clear variant        */
        randomBytes[8]  |= 0x80;  /* set to IETF variant  */

        long msb = 0;
        long lsb = 0;
        for (int i=0; i<8; i++)
            msb = (msb << 8) | (randomBytes[i] & 0xFF);
        for (int i=8; i<16; i++)
            lsb = (lsb << 8) | (randomBytes[i] & 0xFF);

        return new UUID(msb, lsb);
    }

}
