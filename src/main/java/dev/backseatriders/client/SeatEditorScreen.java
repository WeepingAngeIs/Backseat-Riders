package dev.backseatriders.client;

import dev.backseatriders.SeatEditorPayloads;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.Map;

/** A small live 2D top-down preview and editor for generated fallback seats. */
public final class SeatEditorScreen extends Screen {
    private final String profile;
    private final Screen previous;
    private final Map<Integer, Offset> offsets = new HashMap<>();
    private final Map<Integer, String> locators = new HashMap<>();
    private int count;
    private int selectedSeat;
    private int left;
    private int top;
    private EditBox locatorInput;
    private String validationMessage = "";

    private SeatEditorScreen(SeatEditorPayloads.Open data, Screen previous) {
        super(Component.literal("Rideable Seat Editor"));
        this.profile = data.profile();
        this.count = Math.max(2, Math.min(16, data.count()));
        this.previous = previous;
        CompoundTag tag = data.offsets();
        for (int index = 0; index < 16; index++) {
            String key = Integer.toString(index);
            if (!tag.contains(key, net.minecraft.nbt.Tag.TAG_COMPOUND)) continue;
            CompoundTag row = tag.getCompound(key);
            offsets.put(index, new Offset(row.getDouble("x"), row.getDouble("y"), row.getDouble("z")));
        }
        CompoundTag locatorTag = data.locators();
        for (String key : locatorTag.getAllKeys()) {
            try {
                int index = Integer.parseInt(key);
                if (index >= 0 && index < 16 && locatorTag.contains(key, net.minecraft.nbt.Tag.TAG_STRING)) {
                    locators.put(index, locatorTag.getString(key));
                }
            } catch (NumberFormatException ignored) { }
        }
    }

    public static void open(SeatEditorPayloads.Open data) {
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.setScreen(new SeatEditorScreen(data, minecraft.screen));
    }

    @Override
    protected void init() {
        left = (width - 420) / 2;
        top = (height - 250) / 2;
        addRenderableWidget(Button.builder(Component.literal("-"), button -> {
            if (count > 2) count--;
            selectedSeat = Math.min(selectedSeat, count - 1);
        }).bounds(left + 26, top + 48, 32, 20).build());
        addRenderableWidget(Button.builder(Component.literal("+"), button -> {
            if (count < 16) count++;
        }).bounds(left + 142, top + 48, 32, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Prev"), button -> selectSeat((selectedSeat + count - 1) % count))
            .bounds(left + 26, top + 77, 32, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Next"), button -> selectSeat((selectedSeat + 1) % count))
            .bounds(left + 142, top + 77, 32, 20).build());

        addAxisButtons("X", 110, OffsetAxis.X);
        addAxisButtons("Y", 138, OffsetAxis.Y);
        addAxisButtons("Z", 166, OffsetAxis.Z);

        locatorInput = new EditBox(font, left + 26, top + 193, 180, 20, Component.literal("Model locator"));
        locatorInput.setMaxLength(64);
        locatorInput.setValue(locators.getOrDefault(selectedSeat, ""));
        addRenderableWidget(locatorInput);

        addRenderableWidget(Button.builder(Component.literal("Save"), button -> saveAndClose())
            .bounds(left + 26, top + 230, 88, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Cancel"), button -> onClose())
            .bounds(left + 122, top + 230, 88, 20).build());
    }

    private void addAxisButtons(String label, int y, OffsetAxis axis) {
        addRenderableWidget(Button.builder(Component.literal("- 0.1"), button -> adjust(axis, -0.1))
            .bounds(left + 26, top + y, 54, 20).build());
        addRenderableWidget(Button.builder(Component.literal("+ 0.1"), button -> adjust(axis, 0.1))
            .bounds(left + 152, top + y, 54, 20).build());
    }

    private void adjust(OffsetAxis axis, double delta) {
        Offset current = offsetFor(selectedSeat);
        offsets.put(selectedSeat, switch (axis) {
            case X -> new Offset(clamp(current.x + delta), current.y, current.z);
            case Y -> new Offset(current.x, clamp(current.y + delta), current.z);
            case Z -> new Offset(current.x, current.y, clamp(current.z + delta));
        });
    }

    private void selectSeat(int seat) {
        selectedSeat = seat;
        if (locatorInput != null) locatorInput.setValue(locators.getOrDefault(selectedSeat, ""));
    }

    private static double clamp(double value) { return Math.max(-4.0, Math.min(4.0, value)); }

    private Offset offsetFor(int seat) {
        double automaticZ = seat == 0 ? -0.25 : 0.30 + (seat - 1) * 0.28;
        return offsets.getOrDefault(seat, new Offset(0.0, 0.0, automaticZ));
    }

    private void saveAndClose() {
        String locator = locatorInput == null ? "" : locatorInput.getValue().trim();
        if (!locator.isEmpty() && !dev.backseatriders.SeatEditorPolicy.isValidLocator(locator)) {
            validationMessage = "Invalid locator name";
            return;
        }
        if (locator.isEmpty()) locators.remove(selectedSeat);
        else locators.put(selectedSeat, locator);
        CompoundTag packed = new CompoundTag();
        offsets.forEach((index, offset) -> {
            CompoundTag row = new CompoundTag();
            row.putDouble("x", offset.x);
            row.putDouble("y", offset.y);
            row.putDouble("z", offset.z);
            packed.put(Integer.toString(index), row);
        });
        CompoundTag packedLocators = new CompoundTag();
        locators.forEach((index, value) -> packedLocators.putString(Integer.toString(index), value));
        PacketDistributor.sendToServer(new SeatEditorPayloads.Save(profile, count, packed, packedLocators));
        Minecraft.getInstance().setScreen(previous);
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(previous);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(font, title, width / 2, top + 10, 0xFFFFFF);
        graphics.drawString(font, "Profile: " + profile, left + 24, top + 31, 0xC8D8E8);
        graphics.drawString(font, "Minimum seats: " + count, left + 66, top + 54, 0xFFFFFF);
        graphics.drawString(font, "Editing seat: " + (selectedSeat + 1), left + 66, top + 83, 0xFFFFFF);

        Offset current = offsetFor(selectedSeat);
        graphics.drawString(font, String.format(java.util.Locale.ROOT, "X %.1f", current.x), left + 88, top + 116, 0xFFFFFF);
        graphics.drawString(font, String.format(java.util.Locale.ROOT, "Y %.1f", current.y), left + 88, top + 144, 0xFFFFFF);
        graphics.drawString(font, String.format(java.util.Locale.ROOT, "Z %.1f", current.z), left + 88, top + 172, 0xFFFFFF);

        drawPreview(graphics);
        graphics.drawString(font, "Locator (blank = body fallback)", left + 26, top + 181, 0xA8B8C8);
        if (!validationMessage.isEmpty()) graphics.drawString(font, validationMessage, left + 26, top + 217, 0xFFFF6666);
        graphics.drawCenteredString(font, "Top view | generated-seat markers", left + 312, top + 196, 0xA8B8C8);
        graphics.drawCenteredString(font, "Native seat totals are preserved", left + 312, top + 209, 0xA8B8C8);
    }

    private void drawPreview(GuiGraphics graphics) {
        int panelX = left + 226;
        int panelY = top + 34;
        graphics.fill(panelX, panelY, panelX + 172, panelY + 153, 0xCC15202B);
        graphics.fill(panelX + 57, panelY + 12, panelX + 115, panelY + 140, 0xFF566777);
        graphics.fill(panelX + 67, panelY + 18, panelX + 105, panelY + 134, 0xFF697D8E);
        for (int seat = 0; seat < count; seat++) {
            Offset offset = offsetFor(seat);
            int x = panelX + 86 + (int) Math.round(offset.x * 8.0);
            int y = panelY + 76 + (int) Math.round(offset.z * 8.0);
            x = Math.max(panelX + 8, Math.min(panelX + 164, x));
            y = Math.max(panelY + 8, Math.min(panelY + 145, y));
            int color = seat == selectedSeat ? 0xFFFFD54A : 0xFF83D6FF;
            graphics.fill(x - 4, y - 4, x + 5, y + 5, color);
            graphics.drawString(font, Integer.toString(seat + 1), x + 7, y - 4, 0xFFFFFFFF);
        }
        graphics.drawCenteredString(font, "Front", panelX + 86, panelY + 2, 0xFFDFE8F0);
    }

    private enum OffsetAxis { X, Y, Z }
    private record Offset(double x, double y, double z) {}
}

