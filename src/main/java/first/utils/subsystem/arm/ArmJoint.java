package first.utils.subsystem.arm;

import java.util.Optional;

import org.littletonrobotics.junction.Logger;
import org.wpilib.math.geometry.Rotation2d;

import first.utils.AbsEncoderIO;
import first.utils.AbsEncoderInputsAutoLogged;
import first.utils.IO.MotorIO;
import first.utils.IO.MotorInputsAutoLogged;

/**
 * A single joint in an {@link Arm}, driven by one or more motors, with an optional absolute
 * encoder.
 *
 * <p>Inputs are logged through AdvantageKit under the joint's log key:
 *
 * <ul>
 *   <li>{@code <logKey>/Motor/<name>} &ndash; telemetry for each motor
 *       ({@link MotorIO.MotorInputs}), keyed by its {@link first.utils.IO.MotorConfig#name}, or by
 *       its index in the motor array if it has no {@link MotorIO#config}
 *   <li>{@code <logKey>/AbsEncoder} &ndash; absolute encoder reading, when present
 *   <li>{@code <logKey>/AngleRotations} &ndash; joint angle output, in rotations
 * </ul>
 */
public class ArmJoint {
    private final String logKey;

    private final Optional<AbsEncoderIO> absEncoder;
    private final AbsEncoderInputsAutoLogged absEncoderInputs;

    private final MotorIO[] motors;
    private final MotorInputsAutoLogged[] inputs;
    private final String[] motorLogKeys;

    /** Physical description of this joint and its link. */
    public final ArmJointConfig config;

    /**
     * Creates a joint.
     *
     * @param logKey     AdvantageKit log key for this joint's inputs and outputs (e.g. {@code "Arm/Shoulder"});
     *                   must be unique per joint
     * @param config     physical description of the joint and its link
     * @param absEncoder absolute encoder measuring the joint angle, or empty to use the first
     *                   motor's position instead
     * @param motors     motors driving the joint. Each is logged under its
     *                   {@link MotorIO#config} name (read once, here), or under its index in this
     *                   array if it has no config; names must be unique within the joint. Without
     *                   an absolute encoder, the first motor's position (in joint rotations) is
     *                   used as the joint angle
     * @throws IllegalArgumentException if {@code motors} is empty and {@code absEncoder} is empty,
     *     since the joint would have no way to measure its angle
     */
    public ArmJoint(String logKey, ArmJointConfig config, Optional<AbsEncoderIO> absEncoder, MotorIO... motors){
        if (motors.length == 0 && absEncoder.isEmpty()){
            throw new IllegalArgumentException(
                "ArmJoint '" + logKey + "' needs at least one motor or an absolute encoder to measure its angle");
        }

        this.logKey = logKey;
        this.absEncoder = absEncoder;
        this.absEncoderInputs = new AbsEncoderInputsAutoLogged();
        this.motors = motors;
        this.inputs = new MotorInputsAutoLogged[motors.length];
        this.motorLogKeys = new String[motors.length];

        for (int i = 0; i < motors.length; i++){
            this.inputs[i] = new MotorInputsAutoLogged();

            String motorName = motors[i].config != null ? motors[i].config.name : String.valueOf(i);
            this.motorLogKeys[i] = logKey + "/Motor/" + motorName;
        }

        this.config = config;
    }

    /**
     * Reads the latest telemetry from every motor and the absolute encoder, processes it through
     * AdvantageKit (recorded normally, or replaced with logged values during replay), then records
     * the joint angle. Call once per loop before {@link #getAngle()}.
     */
    public void update(){
        for (int i = 0; i < motors.length; i++){
            motors[i].updateInputs(inputs[i]);
            Logger.processInputs(motorLogKeys[i], inputs[i]);
        }

        absEncoder.ifPresent(encoder -> {
            encoder.updateInputs(absEncoderInputs);
            Logger.processInputs(logKey + "/AbsEncoder", absEncoderInputs);
        });

        Logger.recordOutput(logKey + "/AngleRotations", getAngle().getRotations());
    }

    /**
     * Returns the joint angle relative to the previous link (or to the arm base for the first
     * joint), as of the most recent {@link #update()}. Uses the absolute encoder when present,
     * otherwise the first motor's position.
     *
     * @return joint angle relative to the previous link
     */
    public Rotation2d getAngle(){
        return absEncoder.isPresent() ?
            Rotation2d.fromRotations(absEncoder.get().fromRaw(absEncoderInputs.rawRotations)) :
            Rotation2d.fromRotations(inputs[0].angularPositionRotations);
    }


}
