package foundry.veil.api.client.registry;

import foundry.veil.Veil;
import foundry.veil.api.resource.editor.FramebufferFileEditor;
import foundry.veil.api.resource.editor.ResourceFileEditor;
import foundry.veil.api.resource.editor.TextFileEditor;
import foundry.veil.platform.registry.RegistrationProvider;
import foundry.veil.platform.registry.RegistryObject;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import org.jetbrains.annotations.ApiStatus;

public final class VeilResourceEditorRegistry {

    public static final ResourceKey<Registry<ResourceFileEditor<?>>> REGISTRY_KEY = ResourceKey.createRegistryKey(Veil.veilPath("resource_editor"));
    private static final RegistrationProvider<ResourceFileEditor<?>> VANILLA_PROVIDER = RegistrationProvider.get(REGISTRY_KEY, Veil.MODID);
    public static final Registry<ResourceFileEditor<?>> REGISTRY = VANILLA_PROVIDER.asVanillaRegistry();

    public static final RegistryObject<TextFileEditor> TEXT = VANILLA_PROVIDER.register("text", TextFileEditor::new);
    public static final RegistryObject<FramebufferFileEditor> FRAMEBUFFER = VANILLA_PROVIDER.register("framebuffer", FramebufferFileEditor::new);

    private VeilResourceEditorRegistry() {
    }

    @ApiStatus.Internal
    public static void bootstrap() {
    }
}
