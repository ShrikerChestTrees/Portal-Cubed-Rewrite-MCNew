package io.github.fusionflux.portalcubed_gametests.gametests;

import io.github.fusionflux.portalcubed.content.prop.PropType;
import io.github.fusionflux.portalcubed.content.prop.entity.Prop;
import io.github.fusionflux.portalcubed_gametests.PortalCubedGameTests;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.RedstoneLampBlock;

import org.quiltmc.qsl.testing.api.game.QuiltGameTest;

public class PropGameTests implements QuiltGameTest {
	private static final String GROUP = PortalCubedGameTests.ID + ":props/";

	//Test for prop interaction on buttons.  Cubes should press, non-cubes should not.
	@GameTest(template = GROUP + "floor_button_cube")
	public void floorButtonCube(GameTestHelper helper) {
		spawnProp(helper, PropType.STORAGE_CUBE, new BlockPos(2, 3, 0));
		spawnProp(helper, PropType.BEANS, new BlockPos(2, 3, 4));
		helper.succeedWhen(() -> {
			helper.assertBlockProperty(new BlockPos(0, 2, 0), RedstoneLampBlock.LIT, true);
			helper.assertBlockProperty(new BlockPos(0, 2, 4), RedstoneLampBlock.LIT, false);
		});
	}

	//Test for prop interaction on cube buttons.  Cubes should press, non-cubes should not.
	@GameTest(template = GROUP + "cube_button")
	public void cubeButton(GameTestHelper helper) {
		spawnProp(helper, PropType.STORAGE_CUBE, new BlockPos(2, 3, 0));
		spawnProp(helper, PropType.BEANS, new BlockPos(2, 3, 4));
		helper.succeedWhen(() -> {
			helper.assertBlockProperty(new BlockPos(0, 2, 0), RedstoneLampBlock.LIT, true);
			helper.assertBlockProperty(new BlockPos(0, 2, 4), RedstoneLampBlock.LIT, false);
		});
	}

	//Test for entity interaction on buttons.  Anything that presses a stone pressure plate should press buttons.
	@GameTest(template = GROUP + "floor_button_entity")
	public void floorButtonEntity(GameTestHelper helper) {
		helper.spawn(EntityType.ARMOR_STAND, new BlockPos(2, 3, 0));
		helper.spawn(EntityType.ARROW, new BlockPos(2, 3, 4));
		helper.succeedWhen(() -> {
			helper.assertBlockProperty(new BlockPos(0, 2, 0), RedstoneLampBlock.LIT, true);
			helper.assertBlockProperty(new BlockPos(0, 2, 4), RedstoneLampBlock.LIT, false);
		});
	}

	//Test for props being fizzled by goo.  For now, just cubes, but could be expanded later
	@GameTest(template = GROUP + "fizzle_goo")
	public void fizzleGoo(GameTestHelper helper) {
		Prop storageCube = spawnProp(helper, PropType.STORAGE_CUBE, new BlockPos(1, 4, 1));
		helper.succeedWhen(() -> helper.assertEntityNotPresent(storageCube.getType()));
	}

	//Tests for companion cubes becoming charred when in contact with fire or lava
	@GameTest(template = GROUP + "burn_companion_cube")
	public void burnCompanionCube(GameTestHelper helper) {
		Prop lavaCompanionCube = spawnProp(helper, PropType.PORTAL_1_COMPANION_CUBE, new BlockPos(1, 4, 1));
		Prop fireCompanionCube = spawnProp(helper, PropType.PORTAL_1_COMPANION_CUBE, new BlockPos(3, 4, 1));
		Prop cauldronCompanionCube = spawnProp(helper, PropType.PORTAL_1_COMPANION_CUBE, new BlockPos(5, 4, 1));
		helper.succeedWhen(() -> {
			assertPropVariant(helper, lavaCompanionCube, 1);
			assertPropVariant(helper, fireCompanionCube, 1);
			assertPropVariant(helper, cauldronCompanionCube, 1);
		});
	}

	//Tests for dirty/charred props being washed when dropped into water
	//Note - add redirection cubes to this once they get added
	@GameTest(template = GROUP + "prop_washing")
	public void propWashing(GameTestHelper helper) {
		Prop p2StorageCube = spawnDirtyProp(helper, PropType.STORAGE_CUBE, new BlockPos(1, 3, 1));
		Prop p2CompanionCube = spawnDirtyProp(helper, PropType.COMPANION_CUBE, new BlockPos(1, 3, 2));
		Prop radio = spawnDirtyProp(helper, PropType.RADIO, new BlockPos(1, 3, 3));
		Prop p1CompanionCube = spawnDirtyProp(helper, PropType.PORTAL_1_COMPANION_CUBE, new BlockPos(1, 3, 4));
		helper.succeedWhen(() -> {
			assertPropVariant(helper, p2StorageCube, 0);
			assertPropVariant(helper, p2CompanionCube, 0);
			assertPropVariant(helper, radio, 0);
			assertPropVariant(helper, p1CompanionCube, 0);
		});
	}

	//Tests the interaction of dirtying a prop with moss/vines
	//Note - add redirection cubes to this once they get added
	@GameTest(template = GROUP + "prop_dirtying")
	public void propDirtying(GameTestHelper helper) {
		Prop p2StorageCube = spawnProp(helper, PropType.STORAGE_CUBE, new BlockPos(1, 3, 1));
		Prop p2CompanionCube = spawnProp(helper, PropType.COMPANION_CUBE, new BlockPos(1, 3, 2));
		Prop radio = spawnProp(helper, PropType.RADIO, new BlockPos(1, 3, 3));

		Player robot = helper.makeMockSurvivalPlayer();
		robot.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.MOSS_BLOCK, 3));
		p2StorageCube.interact(robot, InteractionHand.MAIN_HAND);
		p2CompanionCube.interact(robot, InteractionHand.MAIN_HAND);
		radio.interact(robot, InteractionHand.MAIN_HAND);

		helper.succeedIf(() -> {
			ItemStack material = robot.getMainHandItem();
			if (!material.isEmpty())
				throw new GameTestAssertException("Material used to dirty prop was not consumed : " + material);
			assertPropVariant(helper, p2StorageCube, 2);
			assertPropVariant(helper, p2CompanionCube, 2);
			assertPropVariant(helper, radio, 1);
		});
	}

	public static Prop spawnProp(GameTestHelper helper, PropType type, BlockPos pos) {
		return Util.make(helper.spawn(type.entityType(), pos), p -> p.setSilent(true));
	}

	public static Prop spawnDirtyProp(GameTestHelper helper, PropType type, BlockPos pos) {
		return Util.make(spawnProp(helper, type, pos), p -> p.setDirty(true));
	}

	public static void assertPropVariant(GameTestHelper helper, Prop prop, int expectedVariant) {
		helper.assertEntityProperty(prop, Prop::getVariant, "variant", expectedVariant);
	}
}
