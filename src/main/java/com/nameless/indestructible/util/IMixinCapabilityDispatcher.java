package com.nameless.indestructible.util;

import net.minecraftforge.common.capabilities.ICapabilityProvider;

public interface IMixinCapabilityDispatcher {
    ICapabilityProvider[] getCaps();

    void setCaps(ICapabilityProvider[] caps);
}
