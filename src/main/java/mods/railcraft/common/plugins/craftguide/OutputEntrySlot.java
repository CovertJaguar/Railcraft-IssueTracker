/* 
 * Copyright (c) CovertJaguar, 2014 http://railcraft.info
 * 
 * This code is the property of CovertJaguar
 * and may only be used with explicit written
 * permission unless otherwise specified on the
 * license page at http://railcraft.info/wiki/info:license.
 */
package mods.railcraft.common.plugins.craftguide;

import mods.railcraft.api.crafting.ICrusherCraftingManager;
import net.minecraft.util.IChatComponent;
import uristqwerty.CraftGuide.api.ItemFilter;
import uristqwerty.CraftGuide.api.ItemSlot;
import uristqwerty.CraftGuide.api.Renderer;
import uristqwerty.CraftGuide.api.SlotType;

import java.util.List;

/**
 * An ItemSlot that appends probability information to the name of
 * the item it contains. To use it, instead of putting an ItemStack
 * or List in the data array, put an Object[] containing the item
 * at index 0, and an Integer representing the probability at index
 * 1.
 *
 * Also worth noting, this class does not require any additional support
 * within the rest of CraftGuide. If you copy it into a different package,
 * and use that copy, it will function just fine with older versions of
 * CraftGuide, as well as newer ones. It also shows how the API is set up
 * to make that sort of extension possible.
 */
public class OutputEntrySlot extends ItemSlot {
    public OutputEntrySlot(int x, int y, int width, int height) {
        super(x, y, width, height);
    }

    public OutputEntrySlot(int x, int y, int width, int height, boolean drawQuantity) {
        super(x, y, width, height, drawQuantity);
    }

    @Override
    public void draw(Renderer renderer, int x, int y, Object[] data, int dataIndex, boolean isMouseOver) {
        implementation.draw(this, renderer, x, y, stack(data, dataIndex), isMouseOver);
    }

    @Override
    public List<String> getTooltip(int x, int y, Object[] data, int dataIndex) {
        if (data[dataIndex] == null || stack(data, dataIndex) == null) {
            return implementation.getTooltip(this, null);
        }

        List<String> tooltip = implementation.getTooltip(this, stack(data, dataIndex));
        ICrusherCraftingManager.IOutputEntry outputEntry = (ICrusherCraftingManager.IOutputEntry) data[dataIndex];
        for (IChatComponent line : outputEntry.getGenRule().getToolTip())
            tooltip.add(line.getFormattedText());
        return tooltip;
    }

    @Override
    public boolean matches(ItemFilter search, Object[] data, int dataIndex, SlotType type) {
        return implementation.matches(this, search, stack(data, dataIndex), type);
    }

    @Override
    public ItemFilter getClickedFilter(int x, int y, Object[] data, int dataIndex) {
        return implementation.getClickedFilter(x, y, stack(data, dataIndex));
    }

    private Object stack(Object[] data, int dataIndex) {
        return data[dataIndex] != null ? ((Object[]) data[dataIndex])[0] : null;
    }
}
