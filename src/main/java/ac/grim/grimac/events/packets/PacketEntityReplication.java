package ac.grim.grimac.events.packets;

import ac.grim.grimac.GrimAC;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.data.packetentity.latency.EntityMetadataData;
import ac.grim.grimac.utils.data.packetentity.latency.EntityMountData;
import ac.grim.grimac.utils.data.packetentity.latency.EntityMoveData;
import ac.grim.grimac.utils.data.packetentity.latency.SpawnEntityData;
import io.github.retrooper.packetevents.event.PacketListenerAbstract;
import io.github.retrooper.packetevents.event.impl.PacketPlaySendEvent;
import io.github.retrooper.packetevents.packettype.PacketType;
import io.github.retrooper.packetevents.packetwrappers.play.out.entity.WrappedPacketOutEntity;
import io.github.retrooper.packetevents.packetwrappers.play.out.entitydestroy.WrappedPacketOutEntityDestroy;
import io.github.retrooper.packetevents.packetwrappers.play.out.entitymetadata.WrappedPacketOutEntityMetadata;
import io.github.retrooper.packetevents.packetwrappers.play.out.entityteleport.WrappedPacketOutEntityTeleport;
import io.github.retrooper.packetevents.packetwrappers.play.out.mount.WrappedPacketOutMount;
import io.github.retrooper.packetevents.packetwrappers.play.out.spawnentityliving.WrappedPacketOutSpawnEntityLiving;
import io.github.retrooper.packetevents.utils.vector.Vector3d;
import it.unimi.dsi.fastutil.Pair;
import org.bukkit.entity.Entity;

public class PacketEntityReplication extends PacketListenerAbstract {

    @Override
    public void onPacketPlaySend(PacketPlaySendEvent event) {
        byte packetID = event.getPacketId();

        if (packetID == PacketType.Play.Server.SPAWN_ENTITY || packetID == PacketType.Play.Server.SPAWN_ENTITY_SPAWN || packetID == PacketType.Play.Server.SPAWN_ENTITY_LIVING) {
            WrappedPacketOutSpawnEntityLiving packetOutEntity = new WrappedPacketOutSpawnEntityLiving(event.getNMSPacket());

            GrimPlayer player = GrimAC.playerGrimHashMap.get(event.getPlayer());
            if (player == null) return;

            Entity entity = packetOutEntity.getEntity();
            if (entity == null) return;

            player.compensatedEntities.spawnEntityQueue.add(new SpawnEntityData(entity, packetOutEntity.getPosition(), player.lastTransactionSent.get()));
        }

        if (packetID == PacketType.Play.Server.REL_ENTITY_MOVE || packetID == PacketType.Play.Server.REL_ENTITY_MOVE_LOOK) {
            WrappedPacketOutEntity.WrappedPacketOutRelEntityMove move = new WrappedPacketOutEntity.WrappedPacketOutRelEntityMove(event.getNMSPacket());

            GrimPlayer player = GrimAC.playerGrimHashMap.get(event.getPlayer());
            if (player == null) return;

            if (move.getDeltaX() != 0 || move.getDeltaY() != 0 || move.getDeltaZ() != 0)
                player.compensatedEntities.moveEntityQueue.add(new EntityMoveData(move.getEntityId(),
                        move.getDeltaX(), move.getDeltaY(), move.getDeltaZ(), player.lastTransactionSent.get(), true));
        }

        if (packetID == PacketType.Play.Server.ENTITY_TELEPORT) {
            WrappedPacketOutEntityTeleport teleport = new WrappedPacketOutEntityTeleport(event.getNMSPacket());

            GrimPlayer player = GrimAC.playerGrimHashMap.get(event.getPlayer());
            if (player == null) return;

            Vector3d position = teleport.getPosition();

            player.compensatedEntities.moveEntityQueue.add(new EntityMoveData(teleport.getEntityId(),
                    position.getX(), position.getY(), position.getZ(), player.lastTransactionSent.get(), false));
        }

        if (packetID == PacketType.Play.Server.ENTITY_METADATA) {
            WrappedPacketOutEntityMetadata entityMetadata = new WrappedPacketOutEntityMetadata(event.getNMSPacket());

            GrimPlayer player = GrimAC.playerGrimHashMap.get(event.getPlayer());
            if (player == null) return;

            player.compensatedEntities.importantMetadataQueue.add(new EntityMetadataData(entityMetadata.getEntityId(), entityMetadata.getWatchableObjects(), player.lastTransactionSent.get()));
        }

        if (packetID == PacketType.Play.Server.MOUNT) {
            WrappedPacketOutMount mount = new WrappedPacketOutMount(event.getNMSPacket());

            GrimPlayer player = GrimAC.playerGrimHashMap.get(event.getPlayer());
            if (player == null) return;

            player.compensatedEntities.mountVehicleQueue.add(new EntityMountData(mount.getEntityId(), mount.getPassengerIds(), player.lastTransactionSent.get()));
        }

        if (packetID == PacketType.Play.Server.ENTITY_DESTROY) {
            WrappedPacketOutEntityDestroy destroy = new WrappedPacketOutEntityDestroy(event.getNMSPacket());

            GrimPlayer player = GrimAC.playerGrimHashMap.get(event.getPlayer());
            if (player == null) return;

            int lastTransactionSent = player.lastTransactionSent.get();
            int[] destroyEntityIds = destroy.getEntityIds().isPresent() ? destroy.getEntityIds().get() : null;

            player.compensatedEntities.destroyEntityQueue.add(new Pair<Integer, int[]>() {
                @Override
                public Integer left() {
                    return lastTransactionSent;
                }

                @Override
                public int[] right() {
                    return destroyEntityIds;
                }
            });
        }
    }
}
