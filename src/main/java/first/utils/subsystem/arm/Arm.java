package first.utils.subsystem.arm;

import java.util.List;

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
            Translation2d armTranslation2d = 
                new Translation2d(joint.config.length, joint.getAngle());

            Translation3d armTranslation3d = new Translation3d(armTranslation2d).rotateBy(joint.config.jointType.direction);
            endEffector = endEffector.plus(armTranslation3d);
        }

        return endEffector;
    }

}
