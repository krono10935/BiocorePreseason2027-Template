package first.utils.IO.talon;

import com.ctre.phoenix6.controls.CoastOut;
import com.ctre.phoenix6.controls.DutyCycleOut;
import com.ctre.phoenix6.controls.PositionDutyCycle;
import com.ctre.phoenix6.controls.StaticBrake;
import com.ctre.phoenix6.controls.TorqueCurrentFOC;
import com.ctre.phoenix6.controls.VelocityDutyCycle;
import com.ctre.phoenix6.hardware.TalonFX;

import first.utils.IO.MotorIO;

/**
 * {@link MotorIO} implementation backed by a CTRE TalonFX running Phoenix 6.
 *
 * <p>Control modes map to Phoenix 6 requests as follows:
 *
 * <ul>
 *   <li>{@link ControlMode#ANGULAR_POSITION} &rarr; {@link PositionDutyCycle} (mechanism rotations)
 *   <li>{@link ControlMode#ANGULAR_VELOCITY} &rarr; {@link VelocityDutyCycle} (mechanism rotations
 *       per second)
 *   <li>{@link ControlMode#DUTYCYCLE} &rarr; {@link DutyCycleOut}
 *   <li>{@link ControlMode#TORQUE_CURRENT} &rarr; {@link TorqueCurrentFOC} (amps; requires a
 *       Phoenix Pro license, otherwise the motor will not move)
 *   <li>{@link ControlMode#BRAKE} &rarr; {@link StaticBrake}
 *   <li>{@link ControlMode#COAST} &rarr; {@link CoastOut}
 * </ul>
 *
 * <p>Closed-loop gains come from the slot 0 gains in {@link TalonConfig#talonFXConfig}.
 */
public class TalonIO extends MotorIO {
    private final static int IMPORTANT_SIGNAL_FREQUENCY = 50;

    /** The underlying TalonFX device. */
    protected TalonFX talon;

    /**
     * Creates the TalonFX, applies {@code config} to it (including {@link TalonConfig#gearRatio}
     * as the sensor-to-mechanism ratio), configures status signal rates, and binds every
     * {@link ControlMode} to the matching Phoenix 6 control request.
     *
     * <p>{@code config} is stored in {@link MotorIO#config}. Note that {@code config.talonFXConfig}
     * is modified in place to carry the gear ratio.
     *
     * @param config CAN ID, bus, name, gear ratio, and Phoenix 6 configuration for the motor
     */
    public TalonIO(TalonConfig config){
        this.config = config;
        talon = new TalonFX(config.id, config.canBus);

        config.talonFXConfig.Feedback.SensorToMechanismRatio = config.gearRatio;
        talon.getConfigurator().apply(config.talonFXConfig);

        talon.optimizeBusUtilization();

        talon.getPosition().setUpdateFrequency(IMPORTANT_SIGNAL_FREQUENCY);
        talon.getVelocity().setUpdateFrequency(IMPORTANT_SIGNAL_FREQUENCY);
        talon.getMotorVoltage().setUpdateFrequency(IMPORTANT_SIGNAL_FREQUENCY);



        withAngularPositionControl(
            (double positionRotations) -> talon.setControl(new PositionDutyCycle(positionRotations))
        );

        withAngularVelocityControl(
            (double velocityRotationsPerSecond) -> talon.setControl(new VelocityDutyCycle(velocityRotationsPerSecond))
        );

        withBrakeMode(
            () -> talon.setControl(new StaticBrake())
        );

        withCoastMode(
            () -> talon.setControl(new CoastOut())
        );

        withDutyCycleControl(
            (double dutyCycleOut) -> talon.setControl(new DutyCycleOut(dutyCycleOut))
        );

        withTorqueControl(
            (double current) -> talon.setControl(new TorqueCurrentFOC(current))
        );
    }

    /**
     * Reads the latest TalonFX status signals into {@code inputs}.
     *
     * <p>Position, velocity, and applied voltage update at 50 Hz. Current and temperature are left
     * at the rate chosen by {@code optimizeBusUtilization()} (4 Hz by default).
     *
     * @param inputs the telemetry struct to populate
     */
    @Override
    public void updateInputs(MotorInputs inputs){
        inputs.angularPositionRotations = talon.getPosition().getValueAsDouble();
        inputs.angularVelocityRotationsPerSecond = talon.getVelocity().getValueAsDouble();
        inputs.appliedVolts = talon.getMotorVoltage().getValueAsDouble();
        inputs.connected = talon.isConnected();
        inputs.statorCurrentAmps = talon.getStatorCurrent().getValueAsDouble();
        inputs.supplyCurrentAmps = talon.getSupplyCurrent().getValueAsDouble();
        inputs.tempCelsius = talon.getDeviceTemp().getValueAsDouble();
    }

}
