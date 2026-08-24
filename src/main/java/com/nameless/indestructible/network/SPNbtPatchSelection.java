package com.nameless.indestructible.network;

import com.nameless.indestructible.main.Indestructible;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Sends the server's per-entity NBT patch selection to tracking clients. */
public final class SPNbtPatchSelection {
    private final int entityId;
    private final boolean active;
    private final String providerId;

    public SPNbtPatchSelection(int entityId, String providerId, boolean active) {
        this.entityId = entityId;
        this.providerId = providerId == null ? "" : providerId;
        this.active = active;
    }

    public static SPNbtPatchSelection fromBytes(FriendlyByteBuf buf) {
        return new SPNbtPatchSelection(buf.readInt(), buf.readUtf(256), buf.readBoolean());
    }

    public static void toBytes(SPNbtPatchSelection message, FriendlyByteBuf buf) {
        buf.writeInt(message.entityId);
        buf.writeUtf(message.providerId, 256);
        buf.writeBoolean(message.active);
    }

    public static void handle(SPNbtPatchSelection message, Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> Indestructible.applyClientNbtSelection(
                message.entityId, message.providerId, message.active));
        context.get().setPacketHandled(true);
    }
}
