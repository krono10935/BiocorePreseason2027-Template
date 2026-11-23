package first.utils.subsystem.arm;

import java.util.List;

import org.wpilib.math.geometry.Rotation3d;
import org.wpilib.math.geometry.Translation2d;
import org.wpilib.math.geometry.Translation3d;

public class Arm {
    public List<ArmJoint> armJoints;

    public Translation3d armBase;
    

    public Arm(Translation3d armBase, List<ArmJoint> armJoints){
        this.armJoints = armJoints;
        this.armBase = armBase;
    }

    public Translation3d getEndEffector(){
        Translation3d endEffector = armBase;

        for (ArmJoint joint : armJoints){
            double angleRad = joint.getAngle().getRadians();
            
            Rotation3d jointRotation = switch (joint.config.jointType) {
                case PITCH -> new Rotation3d(0.0, angleRad, 0.0);
                case YAW -> new Rotation3d(0.0, 0.0, angleRad);
                case ROLL -> new Rotation3d(angleRad, 0.0, 0.0);
            };

            Translation3d linkVector = new Translation3d(joint.config.length, 0.0, 0.0)
                .rotateBy(jointRotation);

            endEffector = endEffector.plus(linkVector);
        }

        return endEffector;
    }

}
