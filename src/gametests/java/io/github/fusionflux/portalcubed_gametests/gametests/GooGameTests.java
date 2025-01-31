package io.github.fusionflux.portalcubed_gametests.gametests;

import io.github.fusionflux.portalcubed_gametests.PortalCubedGameTests;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;

import net.minecraft.world.level.block.RedstoneLampBlock;

import org.quiltmc.qsl.testing.api.game.QuiltGameTest;

public class GooGameTests implements QuiltGameTest {
	private static final String GROUP = PortalCubedGameTests.ID + ":goo/";

	//Test goo bucket dispenser behavior
	@GameTest(template = GROUP + "goo_dispense")
	public void gooDispense(GameTestHelper helper) {
		helper.pullLever(3, 3, 2);
		helper.pullLever(1, 3, 2);

		helper.succeedWhen(() -> {
			helper.assertBlockNotPresent(Blocks.AIR, new BlockPos(3, 2, 1));
			helper.assertBlockPresent(Blocks.AIR, new BlockPos(1, 2, 1));
		});
	}

	//Test the goo immunity item tag
	@GameTest(template = GROUP + "goo_destroy_item")
	public void gooDestroyItem(GameTestHelper helper) {
		helper.pullLever(3, 4, 3);
		helper.pullLever(1, 4, 3);

		helper.succeedWhen(() -> {
			helper.assertBlockProperty(new BlockPos(3, 2, 0), RedstoneLampBlock.LIT, true);
			helper.assertBlockProperty(new BlockPos(1, 2, 0), RedstoneLampBlock.LIT, false);
		});
	}

	//Test the goo immunity entity type tag
	@GameTest(template = GROUP + "goo_destroy_entity")
	public void gooDestroyEntity(GameTestHelper helper) {
		helper.pullLever(3, 4, 3);
		helper.pullLever(1, 4, 3);

		helper.succeedWhen(() -> {
			helper.assertBlockProperty(new BlockPos(3, 2, 0), RedstoneLampBlock.LIT, true);
			helper.assertBlockProperty(new BlockPos(1, 2, 0), RedstoneLampBlock.LIT, false);
		});
	}

	//Test container items dropping their contents when destroyed by goo
	@GameTest(template = GROUP + "goo_destroy_container")
	public void gooDestroyContainer(GameTestHelper helper) {
		helper.pullLever(2, 6, 4);
		helper.pullLever(6, 6, 4);

		helper.runAfterDelay(10, () -> {  //Wait for items to be destroyed and drop their items before removing the goo
			helper.pullLever(2, 6, 2);
			helper.pullLever(6, 6, 2);

			helper.succeedWhen(() -> {
				helper.assertBlockProperty(new BlockPos(1, 2, 1), RedstoneLampBlock.LIT, true);
				helper.assertBlockProperty(new BlockPos(3, 2, 1), RedstoneLampBlock.LIT, true);
				helper.assertBlockProperty(new BlockPos(5, 2, 1), RedstoneLampBlock.LIT, false);
				helper.assertBlockProperty(new BlockPos(7, 2, 1), RedstoneLampBlock.LIT, false);
			});
		});
	}
}
