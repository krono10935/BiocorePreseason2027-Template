package first.utils.subsystem.arm;

import java.util.List;

import org.wpilib.math.geometry.Rotation3d;
import org.wpilib.math.geometry.Translation3d;

/**
 * A serial chain of {@link ArmJoint}s mounted at a fixed base point, with forward kinematics for
 * locating the end effector.
 */
public class Arm {
    /** Joints ordered from the base outward. */
    public List<ArmJoint> armJoints;

    /** Position of the first joint's axis, in meters, in the robot frame. */
    public Translation3d armBase;


    /**
     * Creates an arm.
     *
     * @param armBase   position of the first joint's axis, in meters, in the robot frame
     * @param armJoints joints ordered from the base outward
     */
    public Arm(Translation3d armBase, List<ArmJoint> armJoints){
        this.armJoints = armJoints;
        this.armBase = armBase;
    }

    /**
     * Computes the end effector position using forward kinematics.
     *
     * <p>Each link starts along +X of the previous link's frame. Its joint rotates it about the
     * axis given by {@link ArmJointConfig#jointType}, and that rotation is composed with every
     * earlier joint's rotation, so each joint angle is relative to the previous link. At all zero
     * angles the arm lies straight along +X from {@link #armBase}.
     *
     * <p>Uses each joint's angle as of its last {@link ArmJoint#update()}.
     *
     * @return end effector position, in meters, in the robot frame
     */
    public Translation3d getEndEffector(){
        Translation3d endEffector = armBase;
        Rotation3d cumulativeRotation = Rotation3d.ZERO;

        for (ArmJoint joint : armJoints){
            double angleRad = joint.getAngle().getRadians();

            Rotation3d jointRotation = switch (joint.config.jointType) {
                case PITCH -> new Rotation3d(0.0, angleRad, 0.0);
                case YAW -> new Rotation3d(0.0, 0.0, angleRad);
                case ROLL -> new Rotation3d(angleRad, 0.0, 0.0);
            };

            // Apply this joint about the previous link's local axes (intrinsic composition).
            cumulativeRotation = jointRotation.rotateBy(cumulativeRotation);

            Translation3d linkVector = new Translation3d(joint.config.length, 0.0, 0.0)
                .rotateBy(cumulativeRotation);

            endEffector = endEffector.plus(linkVector);
        }

        return endEffector;
    }

}
