package crazypants.enderio.machine;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.enderio.core.common.util.Util;

import crazypants.enderio.capability.ItemTools;
import crazypants.enderio.capability.ItemTools.MoveResult;
import crazypants.enderio.capacitor.CapacitorHelper;
import crazypants.enderio.capacitor.ICapacitorData;
import info.loenwind.autosave.annotations.Storable;
import info.loenwind.autosave.annotations.Store;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.wrapper.SidedInvWrapper;

import static crazypants.enderio.ModObject.itemBasicCapacitor;

@Storable
public abstract class AbstractInventoryMachineEntity extends AbstractMachineEntity implements ISidedInventory {

  @Store
  protected ItemStack[] inventory;
  protected final SlotDefinition slotDefinition;

  private final @Nonnull int[] allSlots;

  public AbstractInventoryMachineEntity(SlotDefinition slotDefinition) {
    super();
    this.slotDefinition = slotDefinition;

    inventory = new ItemStack[slotDefinition.getNumSlots()];

    allSlots = new int[slotDefinition.getNumSlots()];
    for (int i = 0; i < allSlots.length; i++) {
      allSlots[i] = i;
    }
  }

  public SlotDefinition getSlotDefinition() {
    return slotDefinition;
  }

  public boolean isValidUpgrade(@Nonnull ItemStack itemstack) {
    for (int i = slotDefinition.getMinUpgradeSlot(); i <= slotDefinition.getMaxUpgradeSlot(); i++) {
      if (isItemValidForSlot(i, itemstack)) {
        return true;
      }
    }
    return false;
  }

  public boolean isValidInput(@Nonnull ItemStack itemstack) {
    for (int i = slotDefinition.getMinInputSlot(); i <= slotDefinition.getMaxInputSlot(); i++) {
      if (isItemValidForSlot(i, itemstack)) {
        return true;
      }
    }
    return false;
  }

  public boolean isValidOutput(@Nonnull ItemStack itemstack) {
    for (int i = slotDefinition.getMinOutputSlot(); i <= slotDefinition.getMaxOutputSlot(); i++) {
      if (isItemValidForSlot(i, itemstack)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public final boolean isItemValidForSlot(int i, ItemStack itemstack) {
    if (itemstack == null || itemstack.getItem() == null) {
      return false;
    }
    if (slotDefinition.isUpgradeSlot(i)) {
      final ICapacitorData capacitorData = CapacitorHelper.getCapacitorDataFromItemStack(itemstack);
      return (itemstack.getItem() == itemBasicCapacitor.getItem() && itemstack.getItemDamage() > 0) || capacitorData != null; // TODO level
    }
    return isMachineItemValidForSlot(i, itemstack);
  }

  protected abstract boolean isMachineItemValidForSlot(int i, ItemStack itemstack);

  @Override
  protected boolean doPush(@Nullable EnumFacing dir) {
    if (dir == null || slotDefinition.getNumOutputSlots() <= 0 || !shouldDoWorkThisTick(20)) {
      return false;
    }
    MoveResult res = ItemTools.move(getPushLimit(), worldObj, getPos(), dir, getPos().offset(dir), dir.getOpposite());
    if (res == MoveResult.MOVED) {
      markDirty();
      return true;
    }
    return false;
  }

  @Override
  protected boolean doPull(@Nullable EnumFacing dir) {
    if (dir == null || slotDefinition.getNumInputSlots() <= 0 || !shouldDoWorkThisTick(20) || !hasSpaceToPull()) {
      return false;
    }
    MoveResult res = ItemTools.move(getPullLimit(), worldObj, getPos().offset(dir), dir.getOpposite(), getPos(), dir);
    if (res == MoveResult.MOVED) {
      markDirty();
      return true;
    }
    return false;
  }

  protected boolean hasSpaceToPull() {
    boolean hasSpace = false;
    for (int slot = slotDefinition.minInputSlot; slot <= slotDefinition.maxInputSlot && !hasSpace; slot++) {
      hasSpace = inventory[slot] == null ? true : inventory[slot].stackSize < Math.min(inventory[slot].getMaxStackSize(), getInventoryStackLimit(slot));
    }
    return hasSpace;
  }

  // ---- Inventory
  // ------------------------------------------------------------------------------

  @Override
  public boolean isUseableByPlayer(EntityPlayer player) {
    return canPlayerAccess(player);
  }

  @Override
  public boolean hasCapability(Capability<?> capability, EnumFacing facing) {
    if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
      return true;
    }
    return super.hasCapability(capability, facing);
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T> T getCapability(Capability<T> capability, EnumFacing facing) {
    if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
      return (T) new SidedInvWrapper(this, facing);
    }
    return super.getCapability(capability, facing);
  }

  @Override
  public int getSizeInventory() {
    return slotDefinition.getNumSlots();
  }

  public int getInventoryStackLimit(int slot) {
    return getInventoryStackLimit();
  }

  @Override
  public int getInventoryStackLimit() {
    return 64;
  }

  @Override
  public ItemStack getStackInSlot(int slot) {
    if (slot < 0 || slot >= inventory.length) {
      return null;
    }
    return inventory[slot];
  }

  @Override
  public ItemStack decrStackSize(int slot, int amount) {
    return Util.decrStackSize(this, slot, amount);
  }

  @Override
  public void setInventorySlotContents(int slot, @Nullable ItemStack contents) {
    if (contents == null) {
      inventory[slot] = contents;
    } else {
      inventory[slot] = contents.copy();
    }
    if (contents != null && contents.stackSize > getInventoryStackLimit(slot)) {
      contents.stackSize = getInventoryStackLimit(slot);
    }
    markDirty();
  }

  @Override
  public void clear() {
    for (int i = 0; i < inventory.length; ++i) {
      inventory[i] = null;
    }
    markDirty();
  }

  @Override
  public ItemStack removeStackFromSlot(int index) {
    ItemStack res = inventory[index];
    inventory[index] = null;
    markDirty();
    return res;
  }

  @Override
  public int getField(int id) {
    return 0;
  }

  @Override
  public void setField(int id, int value) {
  }

  @Override
  public int getFieldCount() {
    return 0;
  }

  @Override
  public void openInventory(EntityPlayer player) {
  }

  @Override
  public void closeInventory(EntityPlayer player) {
  }

  @Override
  public @Nonnull String getName() {
    return getMachineName();
  }

  @Override
  public boolean hasCustomName() {
    return false;
  }

  @Override
  public @Nonnull ITextComponent getDisplayName() {
    return hasCustomName() ? new TextComponentString(getName()) : new TextComponentTranslation(getName(), new Object[0]);
  }

  @Override
  public @Nonnull int[] getSlotsForFace(EnumFacing var1) {
    if (isSideDisabled(var1)) {
      return new int[0];
    }
    return allSlots;
  }

  @Override
  public boolean canInsertItem(int slot, ItemStack itemstack, EnumFacing side) {
    if (isSideDisabled(side) || !slotDefinition.isInputSlot(slot)) {
      return false;
    }
    ItemStack existing = inventory[slot];
    if (existing != null) {
      // no point in checking the recipes if an item is already in the slot
      // worst case we get more of the wrong item - but that doesn't change
      // anything
      return existing.isStackable() && existing.isItemEqual(itemstack);
    }
    // no need to call isItemValidForSlot as upgrade slots are not input slots
    return isMachineItemValidForSlot(slot, itemstack);
  }

  @Override
  public boolean canExtractItem(int slot, ItemStack itemstack, EnumFacing side) {
    if (isSideDisabled(side)) {
      return false;
    }
    if (!slotDefinition.isOutputSlot(slot)) {
      return false;
    }
    return canExtractItem(slot, itemstack);
  }

  protected boolean canExtractItem(int slot, ItemStack itemstack) {
    if (inventory[slot] == null || inventory[slot].stackSize < itemstack.stackSize) {
      return false;
    }
    return itemstack.getItem() == inventory[slot].getItem();
  }

}