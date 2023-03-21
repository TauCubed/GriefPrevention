package me.ryanhamshire.GriefPrevention.util;

import com.comphenix.protocol.wrappers.WrappedDataValue;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;

public class DataWatchers {

    public static final WrappedDataValue GLOWING = new WrappedDataValue(0, WrappedDataWatcher.Registry.get(Byte.class), (byte) 0x40); // status: set glowing
    public static final WrappedDataValue NO_GRAVITY = new WrappedDataValue(5, WrappedDataWatcher.Registry.get(Boolean.class), true); // noGravity: true

}
