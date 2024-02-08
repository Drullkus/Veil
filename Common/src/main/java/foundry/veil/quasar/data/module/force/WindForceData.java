package foundry.veil.quasar.data.module.force;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import foundry.veil.quasar.client.particle.ParticleModuleSet;
import foundry.veil.quasar.data.module.ModuleType;
import foundry.veil.quasar.data.module.ParticleModuleData;
import foundry.veil.quasar.emitters.modules.particle.force.ConstantForceModule;
import foundry.veil.quasar.util.CodecUtil;
import org.joml.Vector3d;
import org.joml.Vector3dc;

/**
 * A force that applies a wind force to a particle.
 *
 * <p>
 * Wind forces are useful for simulating wind.
 * The strength of the force is determined by the strength parameter.
 * The falloff parameter is unused.
 * The direction and speed of the wind is determined by the windDirection and windSpeed parameters.
 * The windDirection parameter is a vector that determines the direction of the wind.
 * The windSpeed parameter determines the speed of the wind.
 * The windSpeed parameter is measured in blocks/tick^2.
 */
public record WindForceData(Vector3dc windDirection,
                            float windSpeed,
                            float strength) implements ParticleModuleData {

    public static final Codec<WindForceData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            CodecUtil.VECTOR3D_CODEC.fieldOf("wind_direction").forGetter(WindForceData::windDirection),
            Codec.FLOAT.fieldOf("wind_speed").forGetter(WindForceData::windSpeed),
            Codec.FLOAT.fieldOf("strength").forGetter(WindForceData::strength)
    ).apply(instance, WindForceData::new));

    @Override
    public void addModules(ParticleModuleSet.Builder builder) {
        builder.addModule(new ConstantForceModule(this.windDirection.normalize(this.windSpeed, new Vector3d())));
    }

    @Override
    public ModuleType<?> getType() {
        return ModuleType.WIND;
    }
}
