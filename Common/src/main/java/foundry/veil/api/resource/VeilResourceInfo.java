package foundry.veil.api.resource;

import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.nio.file.FileSystems;
import java.nio.file.Path;

/**
 * @param path            The resource location path this resource is located at
 * @param filePath        The file path of this resource
 * @param modResourcePath The path to this resource in the build folder if in a dev environment
 * @param hidden          Whether this resource should appear in the resource panel
 */
public record VeilResourceInfo(ResourceLocation path,
                               Path filePath,
                               @Nullable Path modResourcePath,
                               boolean hidden) {


    /**
     * @return The file name of this resource
     */
    public String fileName() {
        String path = this.path().getPath();
        String[] split = path.split("/");
        return split[split.length - 1];
    }

    /**
     * @return If this file cannot be accessed by the native file system
     */
    public boolean isStatic() {
        Path filePath = this.filePath();
        return filePath == null || filePath.getFileSystem() != FileSystems.getDefault();
    }
}