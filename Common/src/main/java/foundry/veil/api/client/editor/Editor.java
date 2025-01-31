package foundry.veil.api.client.editor;

import foundry.veil.api.client.render.VeilRenderSystem;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.system.NativeResource;

/**
 * A basic panel that can be toggled in the editor view.
 *
 * @author Ocelot
 */
public interface Editor extends NativeResource {

    /**
     * Renders elements into the menu bar. Only called if {@link #isMenuBarEnabled()} is <code>true</code>.
     */
    default void renderMenuBar() {
    }

    /**
     * Renders this editor to the screen.
     */
    void render();

    /**
     * Called just before the imgui state is drawn to the screen
     */
    default void renderLast() {
    }

    /**
     * @return The visible display name of this editor
     */
    String getDisplayName();

    /**
     * @return The name of the tab group to put this editor in or <code>null</code> for default
     */
    default @Nullable String getGroup() {
        return null;
    }

    /**
     * @return Whether this editor should be selectable.
     */
    default boolean isEnabled() {
        return true;
    }

    /**
     * @return Whether this editor should draw into the menu bar
     */
    default boolean isMenuBarEnabled() {
        return false;
    }

    /**
     * Called when this editor is first opened.
     */
    default void onShow() {
    }

    /**
     * Called when this editor is no longer open.
     */
    default void onHide() {
    }

    /**
     * @return Whether this editor is open
     */
    default boolean isOpen() {
        return VeilRenderSystem.renderer().getEditorManager().isVisible(this);
    }

    /**
     * Frees any resources allocated by this editor before being destroyed.
     */
    @Override
    default void free() {
    }
}
