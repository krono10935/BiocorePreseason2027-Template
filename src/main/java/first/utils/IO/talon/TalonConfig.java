package first.utils.IO.talon;

import com.ctre.phoenix6.configs.TalonFXConfiguration;

import first.utils.IO.MotorConfig;

/**
 * Configuration for a CTRE TalonFX motor controller used by {@link TalonIO}.
 */
public class TalonConfig extends MotorConfig {
    /**
     * Phoenix 6 configuration applied to the TalonFX on construction.
     *
     * <p>{@code Feedback.SensorToMechanismRatio} is overwritten with {@link #gearRatio} when the
     * {@link TalonIO} is created; set the gear ratio through {@link #gearRatio} instead.
     */
    public TalonFXConfiguration talonFXConfig = new TalonFXConfiguration();

    /** Creates a config with the {@link MotorConfig} defaults and a default TalonFX configuration. */
    public TalonConfig() {}
}
