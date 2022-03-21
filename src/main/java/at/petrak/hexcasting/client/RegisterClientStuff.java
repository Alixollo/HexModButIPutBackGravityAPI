package at.petrak.hexcasting.client;

import at.petrak.hexcasting.HexConfig;
import at.petrak.hexcasting.api.client.ScryingLensOverlayRegistry;
import at.petrak.hexcasting.api.spell.SpellDatum;
import at.petrak.hexcasting.client.particles.ConjureParticle;
import at.petrak.hexcasting.common.blocks.HexBlocks;
import at.petrak.hexcasting.common.blocks.circles.BlockEntitySlate;
import at.petrak.hexcasting.common.blocks.circles.impetuses.BlockAbstractImpetus;
import at.petrak.hexcasting.common.blocks.circles.impetuses.BlockEntityAbstractImpetus;
import at.petrak.hexcasting.common.items.HexItems;
import at.petrak.hexcasting.common.items.ItemFocus;
import at.petrak.hexcasting.common.items.ItemScroll;
import at.petrak.hexcasting.common.items.ItemSlate;
import at.petrak.hexcasting.common.items.magic.ItemManaBattery;
import at.petrak.hexcasting.common.items.magic.ItemPackagedSpell;
import at.petrak.hexcasting.common.particles.HexParticles;
import com.mojang.datafixers.util.Pair;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.ParticleFactoryRegisterEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.registries.RegistryObject;

import java.util.List;
import java.util.function.BiConsumer;

public class RegisterClientStuff {
    @SubscribeEvent
    public static void init(FMLClientSetupEvent evt) {
        evt.enqueueWork(() -> {
            ItemProperties.register(HexItems.FOCUS.get(), ItemFocus.DATATYPE_PRED,
                (stack, level, holder, holderID) -> {
                    var tag = stack.getOrCreateTag();
                    var isSealed = tag.getBoolean(ItemFocus.TAG_SEALED);
                    var baseNum = isSealed ? 100f : 0f;
                    if (stack.hasTag()) {
                        var typename = tag.getCompound(ItemFocus.TAG_DATA).getAllKeys().iterator().next();
                        return baseNum + switch (typename) {
                            case SpellDatum.TAG_ENTITY -> 1f;
                            case SpellDatum.TAG_DOUBLE -> 2f;
                            case SpellDatum.TAG_VEC3 -> 3f;
                            case SpellDatum.TAG_WIDGET -> 4f;
                            case SpellDatum.TAG_LIST -> 5f;
                            case SpellDatum.TAG_PATTERN -> 6f;
                            default -> 0f; // uh oh
                        };
                    } else {
                        return baseNum;
                    }
                });
            for (RegistryObject<Item> packager : new RegistryObject[]{
                HexItems.CYPHER,
                HexItems.TRINKET,
                HexItems.ARTIFACT,
            }) {
                ItemProperties.register(packager.get(), ItemPackagedSpell.HAS_PATTERNS_PRED,
                    (stack, level, holder, holderID) ->
                        stack.getOrCreateTag().contains(ItemPackagedSpell.TAG_PATTERNS) ? 1f : 0f
                );
            }

            ItemProperties.register(HexItems.BATTERY.get(), ItemManaBattery.MANA_PREDICATE,
                (stack, level, holder, holderID) -> {
                    var item = (ItemManaBattery) stack.getItem();
                    var tag = stack.getOrCreateTag();
                    return item.getManaFullness(tag);
                });
            ItemProperties.register(HexItems.BATTERY.get(), ItemManaBattery.MAX_MANA_PREDICATE,
                (stack, level, holder, holderID) -> {
                    var item = (ItemManaBattery) stack.getItem();
                    var tag = stack.getOrCreateTag();
                    var max = item.getMaxManaAmt(tag);
                    return (float) Math.sqrt((float) max / HexConfig.chargedCrystalManaAmount.get() / 10);
                });

            ItemProperties.register(HexItems.SCROLL.get(), ItemScroll.ANCIENT_PREDICATE,
                (stack, level, holder, holderID) -> stack.getOrCreateTag().contains(ItemScroll.TAG_OP_ID) ? 1f : 0f);

            ItemProperties.register(HexItems.SLATE.get(), ItemSlate.WRITTEN_PRED,
                (stack, level, holder, holderID) -> ItemSlate.hasPattern(stack) ? 1f : 0f);

            HexTooltips.init();
        });

        renderLayers(ItemBlockRenderTypes::setRenderLayer);

        addScryingLensStuff();
    }

    private static void renderLayers(BiConsumer<Block, RenderType> consumer) {
        consumer.accept(HexBlocks.CONJURED.get(), RenderType.cutout());
    }

    private static void addScryingLensStuff() {
        ScryingLensOverlayRegistry.addPredicateDisplayer(
            (state, pos, observer, world, lensHand) -> state.getBlock() instanceof BlockAbstractImpetus,
            (state, pos, observer, world, lensHand) -> {
                if (world.getBlockEntity(pos) instanceof BlockEntityAbstractImpetus beai) {
                    var dustCount = (float) beai.getMana() / (float) HexConfig.dustManaAmount.get();
                    var tc = new TranslatableComponent("hexcasting.tooltip.lens.impetus.mana",
                        String.format("%.2f", dustCount));
                    return List.of(new Pair<>(new ItemStack(HexItems.AMETHYST_DUST.get()), tc));
                } else {
                    return List.of();
                }
            });
        ScryingLensOverlayRegistry.addDisplayer(Blocks.REDSTONE_WIRE,
            (state, pos, observer, world, lensHand) -> List.of(
                new Pair<>(
                    new ItemStack(Items.REDSTONE),
                    new TextComponent(String.valueOf(state.getValue(BlockStateProperties.POWER)))
                        .withStyle(ChatFormatting.RED))
            ));
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void registerParticles(ParticleFactoryRegisterEvent event) {
        // does whatever a particle can
        var particleMan = Minecraft.getInstance().particleEngine;
        particleMan.register(HexParticles.LIGHT_PARTICLE.get(), ConjureParticle.Provider::new);
        particleMan.register(HexParticles.CONJURE_PARTICLE.get(), ConjureParticle.Provider::new);
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers evt) {
        evt.registerBlockEntityRenderer(HexBlocks.SLATE_TILE.get(), BlockEntitySlate.Renderer::new);
    }
}
