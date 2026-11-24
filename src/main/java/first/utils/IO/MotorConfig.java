package first.utils.IO;

import org.wpilib.hardware.bus.CANPort;

import com.ctre.phoenix6.CANBus;

/**
 * Vendor-agnostic configuration shared by all motor controllers. Vendor-specific configs (such as
 * {@link first.utils.IO.talon.TalonConfig}) extend this class with their own settings.
 */
public class MotorConfig {
    /** Number of configs created so far, used to generate unique default names. */
    private static int motorCount = 0;

    /** CAN device ID of the motor controller. */
    public int id = 0;

    /** CAN bus the motor controller is connected to. */
    public CANBus canBus = new CANBus(CANPort.CAN_D0);

    /**
     * Sensor-to-mechanism gear ratio: motor (sensor) rotations per one mechanism rotation. For
     * example, {@code 25.0} for a 25:1 reduction. Reported positions and velocities, and position
     * and velocity setpoints, are in mechanism units after this ratio is applied.
     */
    public double gearRatio = 1.0;

    /**
     * Name used to identify the motor, such as in log keys. Defaults to {@code "motor<n>"}, where
     * {@code n} counts up with each config created. Because that number depends on creation order,
     * set an explicit name to keep log keys stable as code changes.
     */
    public String name = "motor" + motorCount++;

    /**
     * Creates a config with ID 0 on the default CAN bus, a 1:1 gear ratio, and the next unique
     * default {@link #name}.
     */
    public MotorConfig() {}
}
