package foundry.veil.framebuffer;

import com.google.common.base.Suppliers;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import org.apache.commons.lang3.Validate;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.opengl.GL30;

import java.util.function.IntSupplier;
import java.util.function.Supplier;

import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL30C.GL_COLOR_ATTACHMENT0;

/**
 * Wraps any render target with an {@link AdvancedFbo}.
 *
 * @author Ocelot
 */
public class VanillaAdvancedFboWrapper implements AdvancedFbo {

    private final Supplier<RenderTarget> renderTargetSupplier;
    private final Supplier<AttachmentWrapper> colorBuffer;
    private final Supplier<AttachmentWrapper> depthBuffer;

    public VanillaAdvancedFboWrapper(Supplier<RenderTarget> renderTargetSupplier) {
        this.renderTargetSupplier = Suppliers.memoize(renderTargetSupplier::get);
        this.colorBuffer = Suppliers.memoize(() -> new AttachmentWrapper(this, () -> this.toRenderTarget().getColorTextureId(), GL_COLOR_ATTACHMENT0));
        this.depthBuffer = Suppliers.memoize(() -> new AttachmentWrapper(this, () -> this.toRenderTarget().getDepthTextureId(), GL_DEPTH_ATTACHMENT));
    }

    @Override
    public void create() {
        throw new UnsupportedOperationException("Vanilla framebuffers cannot be created");
    }

    @Override
    public void clear() {
        this.toRenderTarget().clear(Minecraft.ON_OSX);
    }

    @Override
    public void bind(boolean setViewport) {
        this.toRenderTarget().bindWrite(setViewport);
    }

    @Override
    public void bindRead() {
        RenderSystem.assertOnRenderThreadOrInit();
        glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, this.toRenderTarget().frameBufferId);
    }

    @Override
    public void bindDraw(boolean setViewport) {
        RenderSystem.assertOnRenderThreadOrInit();
        RenderTarget renderTarget = this.toRenderTarget();

        glBindFramebuffer(GL_DRAW_FRAMEBUFFER, renderTarget.frameBufferId);
        if (setViewport) {
            RenderSystem.viewport(0, 0, renderTarget.viewWidth, renderTarget.viewHeight);
        }
    }

    @Override
    public void resolveToFbo(int id, int width, int height, int mask, int filtering) {
        RenderSystem.assertOnRenderThreadOrInit();
        RenderTarget renderTarget = this.toRenderTarget();

        this.bindRead();
        glBindFramebuffer(GL_DRAW_FRAMEBUFFER, id);
        glBlitFramebuffer(0, 0, renderTarget.width, renderTarget.height, 0, 0, width, height, mask, filtering);
        AdvancedFbo.unbind();
    }

    @Override
    public void resolveToScreen(int mask, int filtering) {
        RenderSystem.assertOnRenderThreadOrInit();
        RenderTarget renderTarget = this.toRenderTarget();
        Window window = Minecraft.getInstance().getWindow();

        this.bindRead();
        AdvancedFbo.unbindDraw();
        glDrawBuffer(GL_BACK);
        glBlitFramebuffer(0, 0, renderTarget.width, renderTarget.height, 0, 0, window.getWidth(), window.getHeight(), mask, filtering);
        glDrawBuffer(GL_FRONT);
        AdvancedFbo.unbindRead();
    }

    @Override
    public int getId() {
        return this.toRenderTarget().frameBufferId;
    }

    @Override
    public int getWidth() {
        return this.toRenderTarget().width;
    }

    @Override
    public int getHeight() {
        return this.toRenderTarget().height;
    }

    @Override
    public int getColorAttachments() {
        return 1;
    }

    @Override
    public int getClearMask() {
        return GL_COLOR_BUFFER_BIT | (this.toRenderTarget().useDepth ? GL_DEPTH_BUFFER_BIT : 0);
    }

    @Override
    public int[] getDrawBuffers() {
        return new int[]{GL_COLOR_ATTACHMENT0};
    }

    @Override
    public boolean hasColorAttachment(int attachment) {
        return attachment == 0;
    }

    @Override
    public boolean hasDepthAttachment() {
        return this.toRenderTarget().useDepth;
    }

    @Override
    public AdvancedFboAttachment getColorAttachment(int attachment) {
        Validate.isTrue(this.hasColorAttachment(attachment), "Color attachment " + attachment + " does not exist.");
        return this.colorBuffer.get();
    }

    @Override
    public AdvancedFboAttachment getDepthAttachment() {
        Validate.isTrue(this.hasDepthAttachment(), "Depth attachment does not exist.");
        return this.depthBuffer.get();
    }

    @Override
    public RenderTarget toRenderTarget() {
        return this.renderTargetSupplier.get();
    }

    @Override
    public void free() {
        this.toRenderTarget().destroyBuffers();
    }

    private record AttachmentWrapper(AdvancedFbo parent, IntSupplier id, int format) implements AdvancedFboAttachment {

        @Override
        public void create() {
            throw new UnsupportedOperationException("Vanilla framebuffer attachments cannot be created");
        }

        @Override
        public void attach(int target, int attachment) {
            throw new UnsupportedOperationException("Vanilla framebuffer attachments cannot be attached");
        }

        @Override
        public void bindAttachment() {
            RenderSystem.bindTexture(this.id.getAsInt());
        }

        @Override
        public void unbindAttachment() {
            RenderSystem.bindTexture(0);
        }

        @Override
        public int getAttachmentType() {
            return GL_COLOR_ATTACHMENT0;
        }

        @Override
        public int getFormat() {
            return this.format;
        }

        @Override
        public int getWidth() {
            return this.parent.getWidth();
        }

        @Override
        public int getHeight() {
            return this.parent.getHeight();
        }

        @Override
        public int getLevels() {
            return 1;
        }

        @Override
        public boolean canSample() {
            return true;
        }

        @Override
        public @Nullable String getName() {
            return null;
        }

        @Override
        public AdvancedFboAttachment createCopy() {
            return new AttachmentWrapper(this.parent, this.id, this.format);
        }

        @Override
        public void free() {
            throw new UnsupportedOperationException("Vanilla framebuffer attachments cannot be deleted");
        }
    }
}