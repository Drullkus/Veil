package foundry.veil.impl.client.render;

import com.google.common.base.Suppliers;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import foundry.veil.api.client.render.VeilRenderBridge;
import foundry.veil.api.client.render.framebuffer.AdvancedFbo;
import foundry.veil.api.client.render.framebuffer.AdvancedFboAttachment;
import net.minecraft.client.Minecraft;
import org.apache.commons.lang3.Validate;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.IntStream;

import static org.lwjgl.opengl.GL11C.GL_OUT_OF_MEMORY;
import static org.lwjgl.opengl.GL20C.glDrawBuffers;
import static org.lwjgl.opengl.GL30.GL_COLOR_BUFFER_BIT;
import static org.lwjgl.opengl.GL30.GL_DEPTH_BUFFER_BIT;
import static org.lwjgl.opengl.GL30.GL_DRAW_FRAMEBUFFER;
import static org.lwjgl.opengl.GL30.GL_FRAMEBUFFER;
import static org.lwjgl.opengl.GL30.GL_FRAMEBUFFER_COMPLETE;
import static org.lwjgl.opengl.GL30.GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT;
import static org.lwjgl.opengl.GL30.GL_FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT;
import static org.lwjgl.opengl.GL30.GL_READ_FRAMEBUFFER;
import static org.lwjgl.opengl.GL30.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL30.GL_TEXTURE_MAG_FILTER;
import static org.lwjgl.opengl.GL30.GL_TEXTURE_MIN_FILTER;
import static org.lwjgl.opengl.GL30.GL_TEXTURE_WRAP_S;
import static org.lwjgl.opengl.GL30.GL_TEXTURE_WRAP_T;
import static org.lwjgl.opengl.GL30.glBindFramebuffer;
import static org.lwjgl.opengl.GL30.glCheckFramebufferStatus;
import static org.lwjgl.opengl.GL30.glDeleteFramebuffers;
import static org.lwjgl.opengl.GL30.glGenFramebuffers;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL30C.GL_COLOR_ATTACHMENT0;
import static org.lwjgl.opengl.GL30C.GL_FRAMEBUFFER_INCOMPLETE_DRAW_BUFFER;
import static org.lwjgl.opengl.GL30C.GL_FRAMEBUFFER_INCOMPLETE_MULTISAMPLE;
import static org.lwjgl.opengl.GL30C.GL_FRAMEBUFFER_INCOMPLETE_READ_BUFFER;
import static org.lwjgl.opengl.GL30C.GL_FRAMEBUFFER_UNDEFINED;
import static org.lwjgl.opengl.GL30C.GL_FRAMEBUFFER_UNSUPPORTED;

/**
 * Default implementation of {@link AdvancedFbo}.
 *
 * @author Ocelot
 */
@ApiStatus.Internal
public class AdvancedFboImpl implements AdvancedFbo {

    private static final Map<Integer, String> ERRORS = Map.of(
            GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT, "GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT",
            GL_FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT, "GL_FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT",
            GL_FRAMEBUFFER_INCOMPLETE_DRAW_BUFFER, "GL_FRAMEBUFFER_INCOMPLETE_DRAW_BUFFER",
            GL_FRAMEBUFFER_INCOMPLETE_READ_BUFFER, "GL_FRAMEBUFFER_INCOMPLETE_READ_BUFFER",
            GL_FRAMEBUFFER_UNSUPPORTED, "GL_FRAMEBUFFER_UNSUPPORTED",
            GL_FRAMEBUFFER_INCOMPLETE_MULTISAMPLE, "GL_FRAMEBUFFER_INCOMPLETE_MULTISAMPLE",
            GL_FRAMEBUFFER_UNDEFINED, "GL_FRAMEBUFFER_UNDEFINED",
            GL_OUT_OF_MEMORY, "GL_OUT_OF_MEMORY"
    );

    public static final AdvancedFbo MAIN_WRAPPER = VeilRenderBridge.wrap(Minecraft.getInstance()::getMainRenderTarget);

    private int id;
    private int width;
    private int height;
    private final AdvancedFboAttachment[] colorAttachments;
    private final AdvancedFboAttachment depthAttachment;
    private final int clearMask;
    private final int[] drawBuffers;
    private final Supplier<Wrapper> wrapper;

    public AdvancedFboImpl(int width, int height, AdvancedFboAttachment[] colorAttachments, @Nullable AdvancedFboAttachment depthAttachment) {
        this.id = -1;
        this.width = width;
        this.height = height;
        this.colorAttachments = colorAttachments;
        this.depthAttachment = depthAttachment;

        int mask = 0;
        if (this.hasColorAttachment(0)) {
            mask |= GL_COLOR_BUFFER_BIT;
        }
        if (this.hasDepthAttachment()) {
            mask |= GL_DEPTH_BUFFER_BIT;
        }
        this.clearMask = mask;
        this.drawBuffers = IntStream.range(0, this.colorAttachments.length)
                .map(i -> GL_COLOR_ATTACHMENT0 + i)
                .toArray();
        this.wrapper = Suppliers.memoize(() -> new Wrapper(this));
    }

    @Override
    public void create() {
        for (AdvancedFboAttachment attachment : this.colorAttachments) {
            attachment.create();
        }
        if (this.depthAttachment != null) {
            this.depthAttachment.create();
        }

        this.id = glGenFramebuffers();
        this.bind(false);

        for (int i = 0; i < this.colorAttachments.length; i++) {
            this.colorAttachments[i].attach(GL_FRAMEBUFFER, i);
        }
        if (this.depthAttachment != null) {
            this.depthAttachment.attach(GL_FRAMEBUFFER, 0);
        }

        int status = glCheckFramebufferStatus(GL_FRAMEBUFFER);
        if (status != GL_FRAMEBUFFER_COMPLETE) {
            String error = ERRORS.containsKey(status) ? ERRORS.get(status) : "0x" + Integer.toHexString(status).toUpperCase(Locale.ROOT);
            throw new IllegalStateException("Advanced FBO status did not return GL_FRAMEBUFFER_COMPLETE. " + error);
        }

        glDrawBuffers(this.drawBuffers);

        AdvancedFbo.unbind();
    }

    @Override
    public void clear() {
        if (this.clearMask != 0) {
            GlStateManager._clear(this.clearMask, Minecraft.ON_OSX);
        }
    }

    @Override
    public void bind(boolean setViewport) {
        glBindFramebuffer(GL_FRAMEBUFFER, this.id);
        if (setViewport) {
            RenderSystem.viewport(0, 0, this.width, this.height);
        }
    }

    @Override
    public void bindRead() {
        glBindFramebuffer(GL_READ_FRAMEBUFFER, this.id);
    }

    @Override
    public void bindDraw(boolean setViewport) {
        glBindFramebuffer(GL_DRAW_FRAMEBUFFER, this.id);
        if (setViewport) {
            RenderSystem.viewport(0, 0, this.width, this.height);
        }
    }

    @Override
    public void free() {
        if (this.id == -1) {
            return;
        }
        glDeleteFramebuffers(this.id);
        this.id = -1;
        for (AdvancedFboAttachment attachment : this.colorAttachments) {
            attachment.free();
        }
        if (this.depthAttachment != null) {
            this.depthAttachment.free();
        }
    }

    @Override
    public int getId() {
        return this.id;
    }

    @Override
    public int getWidth() {
        return this.width;
    }

    @Override
    public int getHeight() {
        return this.height;
    }

    @Override
    public int getColorAttachments() {
        return this.colorAttachments.length;
    }

    @Override
    public int getClearMask() {
        return this.clearMask;
    }

    @Override
    public int[] getDrawBuffers() {
        return this.drawBuffers;
    }

    @Override
    public boolean hasColorAttachment(int attachment) {
        return attachment >= 0 && attachment < this.colorAttachments.length;
    }

    @Override
    public boolean hasDepthAttachment() {
        return this.depthAttachment != null;
    }

    @Override
    public AdvancedFboAttachment getColorAttachment(int attachment) {
        Validate.isTrue(this.hasColorAttachment(attachment), "Color attachment " + attachment + " does not exist.");
        return this.colorAttachments[attachment];
    }

    @Override
    public AdvancedFboAttachment getDepthAttachment() {
        return Objects.requireNonNull(this.depthAttachment, "Depth attachment does not exist.");
    }

    @Override
    public Wrapper toRenderTarget() {
        return this.wrapper.get();
    }

    @ApiStatus.Internal
    public static Builder copy(RenderTarget parent) {
        if (parent instanceof Wrapper wrapper) {
            AdvancedFbo fbo = wrapper.fbo();
            return new AdvancedFbo.Builder(fbo.getWidth(), fbo.getHeight()).addAttachments(fbo);
        }
        return new Builder(parent.width, parent.height).addAttachments(parent);
    }

    /**
     * A vanilla {@link RenderTarget} wrapper of the {@link AdvancedFboImpl}.
     *
     * @author Ocelot
     * @see AdvancedFbo
     * @since 3.0.0
     */
    public static class Wrapper extends TextureTarget {

        private final AdvancedFboImpl fbo;

        private Wrapper(AdvancedFboImpl fbo) {
            super(fbo.width, fbo.height, fbo.hasDepthAttachment(), Minecraft.ON_OSX);
            this.fbo = fbo;
            this.createBuffers(this.fbo.getWidth(), this.fbo.getHeight(), Minecraft.ON_OSX);
        }

        @Override
        public void resize(int width, int height, boolean onMac) {
            if (!RenderSystem.isOnRenderThread()) {
                RenderSystem.recordRenderCall(() -> this.createBuffers(width, height, onMac));
            } else {
                this.createBuffers(width, height, onMac);
            }
        }

        @Override
        public void destroyBuffers() {
            this.fbo.close();
        }

        @Override
        public void createBuffers(int width, int height, boolean onMac) {
            this.viewWidth = width;
            this.viewHeight = height;
            if (this.fbo == null) {
                return;
            }

            this.fbo.width = width;
            this.fbo.height = height;
            AdvancedFboAttachment attachment = this.fbo.hasColorAttachment(0) ? this.fbo.getColorAttachment(0) : null;
            this.width = attachment == null ? this.viewWidth : attachment.getWidth();
            this.height = attachment == null ? this.viewHeight : attachment.getHeight();
        }

        @Override
        public void setFilterMode(int framebufferFilter) {
            RenderSystem.assertOnRenderThreadOrInit();
            this.filterMode = framebufferFilter;
            for (int i = 0; i < this.fbo.getColorAttachments(); i++) {
                this.fbo.getColorAttachment(i).bindAttachment();
                GlStateManager._texParameter(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, framebufferFilter);
                GlStateManager._texParameter(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, framebufferFilter);
                GlStateManager._texParameter(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP);
                GlStateManager._texParameter(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP);
                this.fbo.getColorAttachment(i).unbindAttachment();
            }
        }

        @Override
        public void bindRead() {
            if (this.fbo.hasColorAttachment(0)) {
                this.fbo.getColorAttachment(0).bindAttachment();
            }
        }

        @Override
        public void unbindRead() {
            if (this.fbo.hasColorAttachment(0)) {
                this.fbo.getColorAttachment(0).unbindAttachment();
            }
        }

        @Override
        public void bindWrite(boolean setViewport) {
            this.fbo.bind(setViewport);
        }

        /**
         * @return The backing advanced fbo
         */
        public AdvancedFboImpl fbo() {
            return this.fbo;
        }
    }
}
