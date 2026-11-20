package de.obsidiansurround;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/** Client-side survival-mode surround. Never edits the world directly. */
public final class AutoObsidianSurroundClient implements ClientModInitializer {
    private static final int MAX_TICKS = 200;
    private static final int MAX_ATTEMPTS_PER_TICK = 2;
    private static final double MAX_REACH_SQUARED = 4.5 * 4.5;
    private static final KeyBinding START_KEY = KeyBindingHelper.registerKeyBinding(
            new KeyBinding("key.auto_obsidian_surround.start", InputUtil.Type.KEYSYM,
                    GLFW.GLFW_KEY_G, KeyBinding.Category.MISC));

    private final List<BlockPos> remaining = new ArrayList<>();
    private boolean running = false;
    private int ticks = 0;
    private int originalSlot = -1;

    @Override
    public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(this::tick);
    }

    private void tick(MinecraftClient mc) {
        while (START_KEY.wasPressed()) {
            if (running) {
                stop(mc, "Abgebrochen");
            } else {
                start(mc);
            }
        }
        if (!running) return;
        if (mc.player == null || mc.world == null || mc.interactionManager == null) {
            stop(mc, "Welt verlassen");
            return;
        }
        if (mc.currentScreen != null) return;
        if (++ticks > MAX_TICKS) {
            stop(mc, "Zeitlimit erreicht");
            return;
        }
        int slot = findObsidian(mc.player.getInventory());
        if (slot == -1) {
            stop(mc, "Kein Obsidian in der Hotbar");
            return;
        }
        // Place walls from the ground up, then close the roof.
        int placed = 0;
        for (BlockPos target : List.copyOf(remaining)) {
            if (mc.world.getBlockState(target).isOf(Blocks.OBSIDIAN)) {
                remaining.remove(target);
                continue;
            }
            if (!mc.world.getBlockState(target).isReplaceable()) continue;
            if (placed >= MAX_ATTEMPTS_PER_TICK) break;
            if (tryPlace(mc, target, slot)) {
                placed++;
            }
        }
        if (remaining.isEmpty()) stop(mc, "Fertig");
    }

    private void start(MinecraftClient mc) {
        if (mc.player == null || mc.world == null || mc.interactionManager == null) return;
        int slot = findObsidian(mc.player.getInventory());
        if (slot < 0) {
            message(mc, "Du brauchst Obsidian in der Hotbar (Slot 1–9)");
            return;
        }
        remaining.clear();
        BlockPos feet = mc.player.getBlockPos();
        // A 3x3 shell at foot and head height (the player occupies the center).
        // A 3x3 roof one level above the player's head.
        for (int dy = 0; dy <= 2; dy++) {
            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) {
                    if (dy < 2 && x == 0 && z == 0) continue;
                    remaining.add(feet.add(x, dy, z));
                }
            }
        }
        originalSlot = mc.player.getInventory().getSelectedSlot();
        ticks = 0;
        running = true;
        message(mc, "Baue Schutz (G zum Stoppen)");
    }

    private boolean tryPlace(MinecraftClient mc, BlockPos target, int obsidianSlot) {
        if (mc.player == null || mc.world == null || mc.interactionManager == null) return false;
        // Vanilla placement requires an existing supporting neighbour.
        for (Direction d : Direction.values()) {
            BlockPos support = target.offset(d);
            if (mc.world.getBlockState(support).isReplaceable()) continue;
            Direction clickedFace = d.getOpposite();
            Vec3d hit = Vec3d.ofCenter(support).add(
                    clickedFace.getOffsetX() * 0.5,
                    clickedFace.getOffsetY() * 0.5,
                    clickedFace.getOffsetZ() * 0.5);
            if (mc.player.getEyePos().squaredDistanceTo(hit) > MAX_REACH_SQUARED) continue;
            mc.player.getInventory().setSelectedSlot(obsidianSlot);
            BlockHitResult bhr = new BlockHitResult(hit, clickedFace, support, false);
            ActionResult result = mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, bhr);
            if (result.isAccepted()) {
                mc.player.swingHand(Hand.MAIN_HAND);
                // Recheck the block next tick; a server can reject placement.
                return true;
            }
        }
        return false;
    }

    private int findObsidian(PlayerInventory inventory) {
        for (int i = 0; i < 9; i++) {
            if (inventory.getStack(i).isOf(Items.OBSIDIAN)) return i;
        }
        return -1;
    }

    private void stop(MinecraftClient mc, String reason) {
        running = false;
        remaining.clear();
        if (mc.player != null && originalSlot >= 0 && originalSlot < 9) {
            mc.player.getInventory().setSelectedSlot(originalSlot);
        }
        originalSlot = -1;
        message(mc, reason);
    }

    private void message(MinecraftClient mc, String message) {
        if (mc.player != null) mc.player.sendMessage(Text.literal("[Auto Surround] " + message), true);
    }
}
