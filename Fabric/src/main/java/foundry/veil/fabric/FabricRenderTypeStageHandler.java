package foundry.veil.fabric;

import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import foundry.veil.api.event.VeilRenderLevelStageEvent;
import foundry.veil.ext.LevelRendererBlockLayerExtension;
import foundry.veil.fabric.event.FabricVeilRenderLevelStageEvent;
import foundry.veil.mixin.accessor.RenderBuffersAccessor;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

import java.util.*;

@ApiStatus.Internal
public class FabricRenderTypeStageHandler {

    private static final Map<VeilRenderLevelStageEvent.Stage, Set<RenderType>> STAGE_RENDER_TYPES = new HashMap<>();
    private static Set<RenderType> CUSTOM_BLOCK_LAYERS;
    private static List<RenderType> BLOCK_LAYERS;

    public static void register(@Nullable VeilRenderLevelStageEvent.Stage stage, RenderType renderType) {
        SortedMap<RenderType, BufferBuilder> fixedBuffers = ((RenderBuffersAccessor) Minecraft.getInstance().renderBuffers()).getFixedBuffers();
        fixedBuffers.put(renderType, new BufferBuilder(renderType.bufferSize()));

        if (stage != null) {
            STAGE_RENDER_TYPES.computeIfAbsent(stage, unused -> new HashSet<>()).add(renderType);
        }
    }

    public static void renderStage(LevelRendererBlockLayerExtension extension, ProfilerFiller profiler, VeilRenderLevelStageEvent.Stage stage, LevelRenderer levelRenderer, MultiBufferSource.BufferSource bufferSource, PoseStack poseStack, Matrix4f projectionMatrix, int renderTick, float partialTicks, Camera camera, Frustum frustum) {
        profiler.push(stage.getName());
        FabricVeilRenderLevelStageEvent.EVENT.invoker().onRenderLevelStage(stage, levelRenderer, bufferSource, poseStack, projectionMatrix, renderTick, partialTicks, camera, frustum);
        profiler.pop();

        Set<RenderType> stages = STAGE_RENDER_TYPES.get(stage);
        if (stages != null) {
            stages.forEach(renderType -> {
                if (CUSTOM_BLOCK_LAYERS.contains(renderType)) {
                    Vec3 pos = camera.getPosition();
                    extension.veil$drawBlockLayer(renderType, poseStack, pos.x, pos.y, pos.z, projectionMatrix);
                }
                bufferSource.endBatch(renderType);
            });
        }
    }

    public static List<RenderType> getBlockLayers() {
        return BLOCK_LAYERS;
    }

    public static void setBlockLayers(ImmutableList.Builder<RenderType> blockLayers) {
        CUSTOM_BLOCK_LAYERS = new HashSet<>(blockLayers.build());
        blockLayers.addAll(RenderType.chunkBufferLayers());
        BLOCK_LAYERS = blockLayers.build();
    }
}
