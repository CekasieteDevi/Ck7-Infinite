package com.ck7infinite.potatoprofile;

import com.ck7infinite.Ck7Infinite;
import com.ck7infinite.resscale.ResScaleConfig;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * /ck7potato apply|dismiss -aplicado explicitamente por el jugador (nunca automatico, ver
 * PotatoProfileHandler). "apply" es el UNICO preset que este comando aplica: habilitar resscale
 * con una escala moderada (0.75) y Aggressive Mipmapping -el modulo de mayor palanca en iGPU segun
 * V2_ROADMAP.md seccion G-. Deliberadamente NO toca Adaptive Simulation Distance (default false,
 * historial de causar problemas si no se prueba primero en el pack real) ni ningun otro modulo:
 * un preset general que escriba across muchos specs es justo la infraestructura que el roadmap
 * senala como cara/fragil de construir bien; este es un preset especifico y acotado, no generico.
 * <p>
 * El registro va con @Mod.EventBusSubscriber a nivel de clase (bus FORGE, no MOD): un intento
 * anterior registraba el listener de RegisterClientCommandsEvent a mano dentro de
 * FMLClientSetupEvent (bus MOD) y el comando nunca quedaba disponible -confirmado en vivo
 * ("Unknown or incomplete command" al ejecutar /ck7potato apply-, RegisterClientCommandsEvent se
 * dispara antes de que ese registro manual llegue a correr. La anotacion de clase hace que Forge
 * enganche el listener en el momento correcto (escaneo de mods), sin depender del orden relativo
 * entre eventos.
 */
@Mod.EventBusSubscriber(modid = Ck7Infinite.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
final class PotatoProfileCommand {

    private PotatoProfileCommand() {
    }

    @SubscribeEvent
    static void onRegisterClientCommands(RegisterClientCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("ck7potato")
                .then(Commands.literal("apply").executes(PotatoProfileCommand::apply))
                .then(Commands.literal("dismiss").executes(PotatoProfileCommand::dismiss)));
    }

    private static int apply(com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx) {
        ResScaleConfig.ENABLED.set(true);
        ResScaleConfig.SCALE.set(0.75);
        ResScaleConfig.AGGRESSIVE_MIPMAPPING.set(true);
        ResScaleConfig.AGGRESSIVE_MIPMAPPING_BIAS.set(0.5);
        ResScaleConfig.SPEC.save();
        ResScaleConfig.refresh();
        PotatoProfileConfig.DISMISSED.set(true);
        PotatoProfileConfig.SPEC.save();
        ctx.getSource().sendSuccess(() -> Component.translatable("ck7infinite.potatoprofile.command.applied"), false);
        return 1;
    }

    private static int dismiss(com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx) {
        PotatoProfileConfig.DISMISSED.set(true);
        PotatoProfileConfig.SPEC.save();
        ctx.getSource().sendSuccess(() -> Component.translatable("ck7infinite.potatoprofile.command.dismissed"), false);
        return 1;
    }
}
