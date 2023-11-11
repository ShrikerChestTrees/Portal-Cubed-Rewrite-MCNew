package io.github.fusionflux.portalcubed.content;

import io.github.fusionflux.portalcubed.content.test.TestBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;

import org.quiltmc.qsl.block.extensions.api.QuiltBlockSettings;

import static io.github.fusionflux.portalcubed.PortalCubed.REGISTRAR;

import io.github.fusionflux.portalcubed.content.button.FloorButtonBlock;
import io.github.fusionflux.portalcubed.content.button.OldApFloorButtonBlock;
import io.github.fusionflux.portalcubed.content.button.P1FloorButtonBlock;
import io.github.fusionflux.portalcubed.framework.item.MultiBlockItem;
import io.github.fusionflux.portalcubed.framework.registration.RenderTypes;
import io.github.fusionflux.portalcubed.framework.registration.block.BlockItemProvider;

public class PortalCubedBlocks {
	public static final TestBlock TEST_BLOCK = REGISTRAR.blocks.create("test_block", TestBlock::new)
			.copyFrom(Blocks.STONE)
			.settings(QuiltBlockSettings::dynamicBounds)
			.item(BlockItemProvider::noItem)
			.build();

	public static final FloorButtonBlock FLOOR_BUTTON_BLOCK = REGISTRAR.blocks.create("floor_button", FloorButtonBlock::new)
			.copyFrom(Blocks.STONE)
			.item((block, properties) -> new MultiBlockItem(block, properties))
			.renderType(RenderTypes.CUTOUT)
			.build();
	public static final FloorButtonBlock OLD_AP_FLOOR_BUTTON_BLOCK = REGISTRAR.blocks.create("old_ap_floor_button", OldApFloorButtonBlock::new)
			.copyFrom(Blocks.STONE)
			.item((block, properties) -> new MultiBlockItem(block, properties))
			.renderType(RenderTypes.CUTOUT)
			.build();
	public static final FloorButtonBlock PORTAL_1_FLOOR_BUTTON_BLOCK = REGISTRAR.blocks.create("portal_1_floor_button", P1FloorButtonBlock::new)
			.copyFrom(Blocks.STONE)
			.item((block, properties) -> new MultiBlockItem(block, properties))
			.renderType(RenderTypes.CUTOUT)
			.build();

	public static void init() {
	}
}
