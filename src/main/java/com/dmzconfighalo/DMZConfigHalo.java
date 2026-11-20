package com.dmzconfighalo;

import com.dmzconfighalo.client.ClientSetup;
import com.dmzconfighalo.command.HaloConfigCommand;
import com.dmzconfighalo.network.HaloNetwork;
import com.mojang.logging.LogUtils;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafml.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import org.slf4j.Logger;

/**
 * DMZ: Config Halo - lets every player configure the height and color of their own DragonMine Z halo.
 */
@Mod(DMZConfigHalo.MOD_ID)
public class DMZConfigHalo {

    public static final String MOD_ID = "dmzconfighalo";
    public static final Logger LOGGER = LogUtils.getLogger();

    public DMZConfigHalo() {
        var modBus = FMLJavaModLoadingContext.get().getModEventBus();

        // Client config file: config/dmzconfighalo-client.toml
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, HaloConfig.SPEC, "dmzconfighalo-client.toml");

        HaloNetwork.register();

        MinecraftForge.EVENT_BUS.register(HaloConfigCommand.class);
        MinecraftForge.EVENT_BUS.register(HaloServerEvents.class);

        if (FMLEnvironment.dist == Dist.CLIENT) {
            ClientSetup.init(modBus);
        }
    }
}
