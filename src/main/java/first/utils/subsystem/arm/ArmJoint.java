package first.utils.subsystem.arm;

import java.util.Optional;

import org.wpilib.math.geometry.Rotation2d;

import first.utils.AbsEncoderIO;
import first.utils.IO.MotorIO;
import first.utils.IO.MotorInputsAutoLogged;

public class ArmJoint {
    private final Optional<AbsEncoderIO> absEncoder; 

    private final MotorIO motor;
    private final MotorInputsAutoLogged inputs;

    public final ArmJointConfig config;

    public ArmJoint(ArmJointConfig config, MotorIO motor, Optional<AbsEncoderIO> absEncoder){
        this.absEncoder = absEncoder;
        this.motor = motor;
        this.inputs = new MotorInputsAutoLogged();
        this.config = config;
    }

    public void update(){
        motor.updateInputs(inputs);
    }

    public Rotation2d getAngle(){
        return absEncoder.isPresent() ? 
            absEncoder.get().get() : 
            Rotation2d.fromRotations(inputs.angularPositionRotations);
    }


}
