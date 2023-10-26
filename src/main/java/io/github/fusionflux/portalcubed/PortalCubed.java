package io.github.fusionflux.portalcubed;

import io.github.fusionflux.portalcubed.content.PortalCubedBlocks;
import io.github.fusionflux.portalcubed.content.PortalCubedItems;
import io.github.fusionflux.portalcubed.content.PortalCubedTabs;
import io.github.fusionflux.portalcubed.registration.Registrar;
import net.minecraft.resources.ResourceLocation;

import org.quiltmc.loader.api.ModContainer;
import org.quiltmc.qsl.base.api.entrypoint.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PortalCubed implements ModInitializer {
	public static final String ID = "portalcubed";
	public static final Logger LOGGER = LoggerFactory.getLogger(ID);

	public static final Registrar REGISTRAR = new Registrar(ID);

	@Override
	public void onInitialize(ModContainer mod) {
		LOGGER.info("Portal Cubed (" + mod.metadata().version() + ") initializing...");
		PortalCubedBlocks.init();
		PortalCubedItems.init();
		PortalCubedTabs.init();
	}

	public static ResourceLocation id(String path) {
		return new ResourceLocation(ID, path);
	}
}
