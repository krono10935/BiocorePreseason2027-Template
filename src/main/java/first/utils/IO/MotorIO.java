package first.utils.IO;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.DoubleConsumer;

import org.littletonrobotics.junction.AutoLog;

/**
 * Hardware-agnostic motor interface using a fluent builder pattern to bind
 * {@link ControlMode} values to motor controller callbacks.
 *
 * <p>Designed to pair with AdvantageKit {@code @AutoLog} input structs so higher-level
 * subsystems can command angular motion, duty cycle, torque, or idle modes without
 * coupling directly to vendor-specific motor controller APIs.</p>
 */
public class MotorIO {

    /**
     * Logged telemetry inputs for a motor mechanism.
     */
    @AutoLog
    public static class MotorInputs {
        /** Whether the motor controller is currently connected and responding. */
        public boolean connected = false;

        /** Angular position of the mechanism in rotations (after the configured gear ratio). */
        public double angularPositionRotations = 0.0;

        /** Angular velocity of the mechanism in rotations per second (after the configured gear ratio). */
        public double angularVelocityRotationsPerSecond = 0.0;

        /** Applied voltage in volts. */
        public double appliedVolts = 0.0;

        /** Stator current draw in amps. */
        public double statorCurrentAmps = 0.0;

        /** Supply bus current draw in amps. */
        public double supplyCurrentAmps = 0.0;

        /** Motor temperature in degrees Celsius. */
        public double tempCelsius = 0.0;

        /** Creates an inputs struct with every field zeroed and {@code connected} set to false. */
        public MotorInputs() {}
    }

    /**
     * Supported control and idle modes for a {@link MotorIO} instance.
     */
    public enum ControlMode {
        /** Closed-loop angular position control, in mechanism rotations. */
        ANGULAR_POSITION,
        /** Closed-loop angular velocity control, in mechanism rotations per second. */
        ANGULAR_VELOCITY,
        /** Open-loop duty cycle output in the range {@code [-1.0, 1.0]}. */
        DUTYCYCLE,
        /** Torque-producing current control, in amps. */
        TORQUE_CURRENT,
        /** Neutral coast mode (motor leads allowed to spin freely). */
        COAST,
        /** Neutral brake mode (motor leads shorted to resist motion). */
        BRAKE
    }

    private final Map<ControlMode, DoubleConsumer> valueControlMap = new EnumMap<>(ControlMode.class);
    private final Map<ControlMode, Runnable> modeControlMap = new EnumMap<>(ControlMode.class);

    private ControlMode currentControlMode = ControlMode.COAST;
    private double lastSetpoint = 0.0;

    /**
     * Configuration of the motor behind this {@code MotorIO}, set by hardware implementations such
     * as {@link first.utils.IO.talon.TalonIO}. It is {@code null} for a {@code MotorIO} built
     * directly with the {@code with*} methods, and {@link #copy()} does not carry it over.
     */
    public MotorConfig config = null;

    /**
     * Creates a new {@code MotorIO} instance with no bound control callbacks.
     * Configure modes via the {@code with*Control} and {@code with*Mode} methods.
     */
    public MotorIO() {}

    /**
     * Creates a shallow copy of this {@code MotorIO} that shares the same control callbacks
     * (so it commands the same hardware). The copy starts with this instance's current mode
     * and last setpoint, but tracks them independently afterwards.
     *
     * <p>The copy is always a plain {@code MotorIO}, so a subclass's {@link #updateInputs}
     * override (e.g. {@link first.utils.IO.talon.TalonIO}) is not carried over and the copy
     * reports no telemetry.
     *
     * @return a new {@code MotorIO} instance with identical control bindings
     */
    public MotorIO copy() {
        MotorIO motor = new MotorIO();
        motor.valueControlMap.putAll(this.valueControlMap);
        motor.modeControlMap.putAll(this.modeControlMap);
        motor.currentControlMode = this.currentControlMode;
        motor.lastSetpoint = this.lastSetpoint;
        return motor;
    }

    /**
     * Binds a callback for {@link ControlMode#ANGULAR_POSITION}.
     *
     * @param setAngularPosition consumer accepting the target position in mechanism rotations
     * @return this {@code MotorIO} instance for method chaining
     */
    public MotorIO withAngularPositionControl(DoubleConsumer setAngularPosition) {
        valueControlMap.put(ControlMode.ANGULAR_POSITION, setAngularPosition);
        return this;
    }

    /**
     * Binds a callback for {@link ControlMode#ANGULAR_VELOCITY}.
     *
     * @param setAngularVelocity consumer accepting the target velocity in mechanism rotations per second
     * @return this {@code MotorIO} instance for method chaining
     */
    public MotorIO withAngularVelocityControl(DoubleConsumer setAngularVelocity) {
        valueControlMap.put(ControlMode.ANGULAR_VELOCITY, setAngularVelocity);
        return this;
    }

    /**
     * Binds a callback for {@link ControlMode#DUTYCYCLE}.
     *
     * @param setDutyCycle consumer accepting duty cycle output in {@code [-1.0, 1.0]}
     * @return this {@code MotorIO} instance for method chaining
     */
    public MotorIO withDutyCycleControl(DoubleConsumer setDutyCycle) {
        valueControlMap.put(ControlMode.DUTYCYCLE, setDutyCycle);
        return this;
    }

    /**
     * Binds a callback for {@link ControlMode#TORQUE_CURRENT}.
     *
     * @param setTorque consumer accepting the target torque-producing current in amps
     * @return this {@code MotorIO} instance for method chaining
     */
    public MotorIO withTorqueControl(DoubleConsumer setTorque) {
        valueControlMap.put(ControlMode.TORQUE_CURRENT, setTorque);
        return this;
    }

    /**
     * Binds a callback for {@link ControlMode#COAST}.
     *
     * @param onCoast action executed when switching to coast mode
     * @return this {@code MotorIO} instance for method chaining
     */
    public MotorIO withCoastMode(Runnable onCoast) {
        modeControlMap.put(ControlMode.COAST, onCoast);
        return this;
    }

    /**
     * Binds a callback for {@link ControlMode#BRAKE}.
     *
     * @param onBrake action executed when switching to brake mode
     * @return this {@code MotorIO} instance for method chaining
     */
    public MotorIO withBrakeMode(Runnable onBrake) {
        modeControlMap.put(ControlMode.BRAKE, onBrake);
        return this;
    }

    /**
     * Commands the motor using a numeric setpoint under the given {@link ControlMode}.
     *
     * @param mode the setpoint control mode to activate
     * @param value target setpoint value in the mode's expected units
     * @throws IllegalArgumentException if {@code mode} does not accept a numeric setpoint
     * @throws IllegalStateException if no callback is registered for {@code mode}
     */
    public void setSetpoint(ControlMode mode, double value) {
        if (mode == ControlMode.COAST || mode == ControlMode.BRAKE) {
            throw new IllegalArgumentException("Mode " + mode + " does not accept a numeric setpoint");
        }
        DoubleConsumer handler = valueControlMap.get(mode);
        if (handler == null) {
            throw new IllegalStateException("No handler registered for control mode: " + mode);
        }
        this.currentControlMode = mode;
        this.lastSetpoint = value;
        handler.accept(value);
    }

    /**
     * Switches the motor to a standalone mode such as {@link ControlMode#COAST} or
     * {@link ControlMode#BRAKE}.
     *
     * Resets the last setpoint to {@code 0.0}.
     *
     * @param mode the mode to activate
     * @throws IllegalArgumentException if {@code mode} requires a numeric setpoint; use
     *     {@link #setSetpoint(ControlMode, double)} instead
     * @throws IllegalStateException if no handler is registered for {@code mode}
     */
    public void setMode(ControlMode mode) {
        if (mode != ControlMode.COAST && mode != ControlMode.BRAKE) {
            throw new IllegalArgumentException("Mode " + mode + " requires a numeric setpoint; use setSetpoint");
        }
        Runnable handler = modeControlMap.get(mode);
        if (handler == null) {
            throw new IllegalStateException("No handler registered for control mode: " + mode);
        }
        this.currentControlMode = mode;
        this.lastSetpoint = 0.0;
        handler.run();
    }

    /**
     * Updates the provided {@link MotorInputs} struct with latest hardware telemetry.
     * Override or implement in hardware/simulation subclasses or wrappers as needed.
     *
     * @param inputs the telemetry struct to populate
     */
    public void updateInputs(MotorInputs inputs) {}

    /**
     * Returns the most recently commanded {@link ControlMode}.
     *
     * @return active control mode
     */
    public ControlMode getCurrentControlMode() {
        return currentControlMode;
    }

    /**
     * Returns the most recently commanded numeric setpoint, or {@code 0.0} when in a
     * non-setpoint mode.
     *
     * @return last commanded setpoint
     */
    public double getLastSetpoint() {
        return lastSetpoint;
    }
}