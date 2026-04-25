package com.nameless.indestructible.mixin;

import com.nameless.indestructible.util.IMixinCapabilityDispatcher;
import net.minecraftforge.common.capabilities.CapabilityDispatcher;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(CapabilityDispatcher.class)
public abstract class MixinCapabilityDispatcherAccessor implements IMixinCapabilityDispatcher {
    @Mutable
    @Shadow(remap = false)
    private ICapabilityProvider[] caps;

    @Override
    public ICapabilityProvider[] getCaps() {
        return this.caps;
    }

    @Override
    public void setCaps(ICapabilityProvider[] caps) {
        this.caps = caps;
    }
}